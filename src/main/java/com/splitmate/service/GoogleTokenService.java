package com.splitmate.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service 
public class GoogleTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(clientId))
                .build();
    }
    public GoogleIdToken.Payload verify(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new BadCredentialsException("Google credential is required");
        }
        try {
            // 驗證簽章、簽發者、有效期限，以及是否發給本應用程式。
            GoogleIdToken token = verifier.verify(credential);

            if (token == null) {
                throw new BadCredentialsException("Invalid Google credential");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())
                    || payload.getEmail() == null
                    || payload.getEmail().isBlank()) {
                throw new BadCredentialsException("Google email is not verified");
            }

            return payload;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid Google credential", e);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Google verification service is temporarily unavailable", e);
        }
    }

}
