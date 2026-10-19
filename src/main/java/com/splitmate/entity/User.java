package com.splitmate.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "owner")
    private List<ExpenseGroup> ownedGroups = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<GroupMember> groupMembers = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "payer")
    private List<Expense> paidExpenses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ExpenseShare> expenseShares = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "fromUser")
    private List<Settlement> sentSettlements = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "toUser")
    private List<Settlement> receivedSettlements = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<PhoneVerification> phoneVerifications = new ArrayList<>();
}
