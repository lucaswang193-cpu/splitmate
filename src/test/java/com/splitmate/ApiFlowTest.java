package com.splitmate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.splitmate.entity.User;
import com.splitmate.repository.PhoneVerificationRepository;
import com.splitmate.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
public class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRunFullApiFlow() throws Exception {
        mockMvc.perform(get("/api/currencies?page=0&size=20"))
                .andExpect(status().isOk());

        Long categoryId = getFirstCategoryId();

        String aliceEmail = "alice-" + System.currentTimeMillis() + "@example.com";
        String bobEmail = "bob-" + System.currentTimeMillis() + "@example.com";
        String aliceId = createUser("Alice", aliceEmail);
        String bobId = createUser("Bob", bobEmail);
        String accessToken = loginAndGetToken(aliceEmail);
        String bobAccessToken = loginAndGetToken(bobEmail);

        mockMvc.perform(put("/api/users/%s".formatted(aliceId))
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice Updated",
                                  "phoneNumber": "+886900000001",
                                  "avatarUrl": "https://example.com/alice.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice Updated"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+886900000001"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/alice.png"));

        String groupBody = mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Japan Trip",
                                  "description": "Tokyo food and travel",
                                  "baseCurrency": "TWD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode groupData = objectMapper.readTree(groupBody).get("data");
        String groupId = groupData.get("id").asText();
        String inviteCode = groupData.get("inviteCode").asText();

        mockMvc.perform(post("/api/group-members/join")
                        .header("Authorization", bearer(bobAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "%s"
                                }
                                """.formatted(inviteCode)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/group-members/groups/%s?page=0&size=20".formatted(groupId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/groups/%s".formatted(groupId))
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Japan Trip Updated",
                                  "description": "Updated Tokyo food and travel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Japan Trip Updated"));

        var creationResult = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupId": "%s",
                                  "payerId": "%s",
                                  "id": "761c1066-0b8a-4b1c-965a-ff2f43391c27",
                                  "title": "Dinner",
                                  "categoryId": %d,
                                  "currency": "JPY",
                                  "amount": 12000.00,
                                  "exchangeRate": 0.22,
                                  "baseAmount": 2640.00,
                                  "expenseAt": "2026-08-27T19:30:15",
                                  "note": "Sushi dinner",
                                  "splits": [
                                    {
                                      "userId": "%s",
                                      "shareAmount": 1320.00,
                                      "paidAmount": 2640.00
                                    },
                                    {
                                      "userId": "%s",
                                      "shareAmount": 1320.00,
                                      "paidAmount": 0
                                    }
                                  ]
                                }
                                """.formatted(groupId, aliceId, categoryId, aliceId, bobId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("761c1066-0b8a-4b1c-965a-ff2f43391c27"))
                .andReturn();
        String expenseBody = creationResult.getResponse().getContentAsString();
        String originalRequest = creationResult.getRequest().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("2026-08-27T19:30:15",
                objectMapper.readTree(expenseBody).get("data").get("expenseAt").asText());
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalRequest.replace("2026-08-27T19:30:15", "2026-08-27")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalRequest.replace("2026-08-27T19:30:15", "2026-08-27T19:31:15")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalRequest.replace("\"id\": \"761c1066-0b8a-4b1c-965a-ff2f43391c27\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(originalRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("761c1066-0b8a-4b1c-965a-ff2f43391c27"))
                .andExpect(jsonPath("$.data.shares.length()").value(2));
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalRequest.replace("Dinner", "Changed dinner")))
                .andExpect(status().isConflict());

        String expenseId = objectMapper.readTree(expenseBody)
                .get("data")
                .get("id")
                .asText();

        mockMvc.perform(get("/api/expenses/%s".formatted(expenseId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/expenses/%s".formatted(expenseId))
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payerId": "%s",
                                  "title": "Dinner Updated",
                                  "categoryId": %d,
                                  "currency": "JPY",
                                  "amount": 13000.00,
                                  "exchangeRate": 0.22,
                                  "baseAmount": 2860.00,
                                  "expenseAt": "2026-08-28T20:45:30",
                                  "note": "Updated sushi dinner",
                                  "splits": [
                                    {
                                      "userId": "%s",
                                      "shareAmount": 1430.00,
                                      "paidAmount": 2860.00
                                    },
                                    {
                                      "userId": "%s",
                                      "shareAmount": 1430.00,
                                      "paidAmount": 0
                                    }
                                  ]
                                }
                                """.formatted(aliceId, categoryId, aliceId, bobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Dinner Updated"))
                .andExpect(jsonPath("$.data.expenseAt").value("2026-08-28T20:45:30"));

        mockMvc.perform(get("/api/expenses?groupId=%s&page=0&size=20&sort=expenseAt,desc".formatted(groupId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expenses/groups/%s/balances?page=0&size=20".formatted(groupId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/settlements")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupId": "%s",
                                  "fromUserId": "%s",
                                  "toUserId": "%s",
                                  "amount": 1320.00,
                                  "currency": "TWD"
                                }
                                """.formatted(groupId, bobId, aliceId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/groups/%s?page=0&size=20".formatted(groupId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/expenses/%s".formatted(expenseId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expenses/%s".formatted(expenseId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));

        mockMvc.perform(delete("/api/group-members/groups/%s/users/%s".formatted(groupId, aliceId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));

        mockMvc.perform(put("/api/groups/%s/owner".formatted(groupId))
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newOwnerId": "%s"
                                }
                                """.formatted(bobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(bobId));

        mockMvc.perform(delete("/api/group-members/groups/%s/users/%s".formatted(groupId, aliceId))
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/group-members/groups/%s/users/%s".formatted(groupId, aliceId))
                        .header("Authorization", bearer(bobAccessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Settle your group balance before leaving"));

        // Deleting the expense leaves the earlier repayment outstanding; refund it first.
        mockMvc.perform(post("/api/settlements")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupId": "%s",
                                  "fromUserId": "%s",
                                  "toUserId": "%s",
                                  "amount": 1320.00,
                                  "currency": "TWD"
                                }
                                """.formatted(groupId, aliceId, bobId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/group-members/groups/%s/users/%s".formatted(groupId, aliceId))
                        .header("Authorization", bearer(bobAccessToken)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = "duplicate-" + System.currentTimeMillis() + "@example.com";

        createUser("Alice", email);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "name": "Alice Clone",
                  "email": "%s",
                  "phoneNumber": null,
                  "password": "password123",
                  "avatarUrl": null
                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldRejectDuplicatePhoneNumber() throws Exception {
        String phoneNumber = "+8869" + System.currentTimeMillis();

        createUser("Phone Owner", "phone-owner-" + System.currentTimeMillis() + "@example.com", phoneNumber);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Phone Duplicate",
                                  "email": "phone-duplicate-%d@example.com",
                                  "phoneNumber": "%s",
                                  "password": "password123",
                                  "avatarUrl": null
                                }
                                """.formatted(System.currentTimeMillis(), phoneNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Phone number already exists"));
    }

    @Test
    void shouldStoreEncodedPasswordWhenCreatingUser() throws Exception {
        String email = "encoded-" + System.currentTimeMillis() + "@example.com";

        createUser("Encoded User", email);

        User user = userRepository.findByEmail(email).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(user.getPassword()).isNotEqualTo("password123");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }

    @Test
    void shouldLoginWithCorrectPassword() throws Exception {
        String email = "login-" + System.currentTimeMillis() + "@example.com";
        createUser("Login User", email, "+886912345678");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.phoneNumber").value("+886912345678"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void shouldGetCurrentUserWithAccessToken() throws Exception {
        String email = "me-" + System.currentTimeMillis() + "@example.com";
        createUser("Me User", email);

        String accessToken = loginAndGetToken(email);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void shouldUpdateCurrentUserWithAccessToken() throws Exception {
        String email = "update-me-" + System.currentTimeMillis() + "@example.com";
        createUser("Update Me User", email);

        String accessToken = loginAndGetToken(email);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Me",
                                  "phoneNumber": "+886900000002",
                                  "avatarUrl": "https://example.com/me.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Me"))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void shouldGetCurrentUserMembershipsWithAccessToken() throws Exception {
        String email = "my-memberships-" + System.currentTimeMillis() + "@example.com";
        createUser("Membership User", email);

        String accessToken = loginAndGetToken(email);

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "My Group",
                                  "description": "My memberships test",
                                  "baseCurrency": "TWD"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/group-members/me?page=0&size=20")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].role").value("OWNER"));
    }

    @Test
    void shouldRejectProtectedApiWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        String email = "wrong-password-" + System.currentTimeMillis() + "@example.com";
        createUser("Wrong Password User", email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrongPassword123"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Email or password incorrect"));
    }

    @Test
    void shouldRejectLoginWithUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown-%d@example.com",
                                  "password": "password123"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Email or password incorrect"));
    }

    @Test
    void shouldCreateAndConfirmPhoneVerification() throws Exception {
        String email = "phone-verify-" + System.currentTimeMillis() + "@example.com";
        String phoneNumber = "+8869" + System.nanoTime();
        String userId = createUser("Phone Verify User", email, phoneNumber);
        String accessToken = loginAndGetToken(email);

        String verificationBody = mockMvc.perform(post("/api/auth/phone-verifications")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "phoneNumber": "%s"
                                }
                                """.formatted(userId, phoneNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long verificationId = objectMapper.readTree(verificationBody)
                .get("data").get("id").asLong();
        String code = phoneVerificationRepository.findById(verificationId)
                .orElseThrow().getCode();

        mockMvc.perform(post("/api/auth/phone-verifications/confirm")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "phoneNumber": "%s",
                                  "code": "%s"
                                }
                                """.formatted(userId, phoneNumber, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneVerified").value(true));
    }

    @Test
    void shouldRejectIncorrectPhoneVerificationCode() throws Exception {
        String phoneNumber = "+8869" + System.nanoTime();
        String email = "wrong-code-" + System.currentTimeMillis() + "@example.com";
        String userId = createUser("Wrong Code User", email, phoneNumber);
        String accessToken = loginAndGetToken(email);

        String verificationBody = mockMvc.perform(post("/api/auth/phone-verifications")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "phoneNumber": "%s"
                                }
                                """.formatted(userId, phoneNumber)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long verificationId = objectMapper.readTree(verificationBody)
                .get("data").get("id").asLong();
        String correctCode = phoneVerificationRepository.findById(verificationId)
                .orElseThrow().getCode();
        String wrongCode = "000000".equals(correctCode) ? "111111" : "000000";

        mockMvc.perform(post("/api/auth/phone-verifications/confirm")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "phoneNumber": "%s",
                                  "code": "%s"
                                }
                                """.formatted(userId, phoneNumber, wrongCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PHONE_VERIFICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Phone verification code incorrect"))
                .andExpect(jsonPath("$.data.attemptCount").value(1))
                .andExpect(jsonPath("$.data.remainingAttempts").value(4))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        org.junit.jupiter.api.Assertions.assertEquals(1,
                phoneVerificationRepository.findById(verificationId).orElseThrow().getAttemptCount());
    }

    @Test
    void shouldRejectJoinWithUnknownInviteCode() throws Exception {
        String email = "charlie-" + System.currentTimeMillis() + "@example.com";
        String userId = createUser("Charlie", email);
        String accessToken = loginAndGetToken(email);

        mockMvc.perform(post("/api/group-members/join")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "NO_SUCH_CODE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldRejectExpenseWhenSplitTotalDoesNotEqualBaseAmount() throws Exception {
        Long categoryId = getFirstCategoryId();
        String ownerEmail = "dana-" + System.currentTimeMillis() + "@example.com";
        String ownerId = createUser("Dana", ownerEmail);
        String accessToken = loginAndGetToken(ownerEmail);

        String groupBody = mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mismatch Trip",
                                  "description": "Invalid split case",
                                  "baseCurrency": "TWD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String groupId = objectMapper.readTree(groupBody)
                .get("data")
                .get("id")
                .asText();

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupId": "%s",
                                  "payerId": "%s",
                                  "id": "bc06ce92-42eb-4c16-8a17-2593b91fb3a3",
                                  "title": "Wrong Total",
                                  "categoryId": %d,
                                  "currency": "TWD",
                                  "amount": 1000.00,
                                  "exchangeRate": 1.00,
                                  "baseAmount": 1000.00,
                                  "expenseAt": "2026-08-27T19:30:15",
                                  "note": "Split total is too low",
                                  "splits": [
                                    {
                                      "userId": "%s",
                                      "shareAmount": 900.00,
                                      "paidAmount": 1000.00
                                    }
                                  ]
                                }
                                """.formatted(groupId, ownerId, categoryId, ownerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"));
    }

    private Long getFirstCategoryId() throws Exception {
        String categoriesBody = mockMvc.perform(get("/api/expense-categories?page=0&size=20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(categoriesBody)
                .get("data")
                .get("content")
                .get(0)
                .get("id")
                .asLong();
    }

    private String createUser(String name) throws Exception {
        String email = name.toLowerCase() + "-" + System.currentTimeMillis() + "@example.com";

        return createUser(name, email);
    }

    private String createUser(String name, String email) throws Exception {
        return createUser(name, email, "+8869" + System.nanoTime());
    }

    private String createUser(String name, String email, String phoneNumber) throws Exception {

        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "phoneNumber": "%s",
                                  "password": "password123",
                                  "avatarUrl": null
                                }
                                """.formatted(name, email, phoneNumber)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body)
                .get("data")
                .get("id")
                .asText();
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body)
                .get("data")
                .get("accessToken")
                .asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
