package com.mek.miniats.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component

public class SupabaseAuthClient {

    private final RestClient anon;     // uses anon key

    private final RestClient admin;    // uses service_role key

    public SupabaseAuthClient(

            @Value("${supabase.url}") String url,

            @Value("${supabase.anon-key}") String anonKey,

            @Value("${supabase.service-role-key}") String serviceKey) {

        this.anon = RestClient.builder()

                .baseUrl(url + "/auth/v1")

                .defaultHeader("apikey", anonKey)

                .build();

        this.admin = RestClient.builder()

                .baseUrl(url + "/auth/v1")

                .defaultHeader("apikey", serviceKey)

                .defaultHeader("Authorization", "Bearer " + serviceKey)

                .build();

    }

    /** Verify a user's password. Returns the token response, or throws on bad creds. */

    public TokenResponse signInWithPassword(String email, String password) {

        return anon.post()

                .uri("/token?grant_type=password")

                .body(Map.of("email", email, "password", password))

                .retrieve()

                .body(TokenResponse.class);

    }

    /** Admin-only: create an account with a role. Called from your admin screens. */

    public void adminCreateUser(String email, String password, String fullName, String role) {

        admin.post()

                .uri("/admin/users")

                .body(Map.of(

                        "email", email,

                        "password", password,

                        "email_confirm", true,

                        "user_metadata", Map.of("full_name", fullName, "role", role)))

                .retrieve()

                .toBodilessEntity();

        // The on_auth_user_created trigger auto-inserts the matching profiles row.

    }

    /**
     * Admin-only: update a user's email, full name, and/or password. A blank
     * password leaves it unchanged. Email is auto-confirmed to avoid lockout.
     */
    public void adminUpdateUser(String userId, String email, String newPassword, String fullName) {
        Map<String, Object> body = new HashMap<>();
        if (email != null && !email.isBlank()) {
            body.put("email", email);
            body.put("email_confirm", true);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            body.put("password", newPassword);
        }
        if (fullName != null) {
            body.put("user_metadata", Map.of("full_name", fullName));
        }
        admin.put()
                .uri("/admin/users/" + userId)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** Admin-only: delete a user. The profiles row cascades via its FK. */
    public void adminDeleteUser(String userId) {
        admin.delete()
                .uri("/admin/users/" + userId)
                .retrieve()
                .toBodilessEntity();
    }

}

// TokenResponse: a record with access_token, refresh_token, and a nested user (id, email).
