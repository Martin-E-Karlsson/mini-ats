package com.mek.miniats.admin.service;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.auth.SupabaseAuthClient;
import com.mek.miniats.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Creates accounts through the Supabase Auth Admin API. GoTrue creates the
 * auth.users row; the on_auth_user_created DB trigger then inserts the matching
 * profiles row with the role we pass in user_metadata.
 */
@Service
public class AdminUserService {

    private final SupabaseAuthClient authClient;

    public AdminUserService(SupabaseAuthClient authClient) {
        this.authClient = authClient;
    }

    public void createAccount(String email, String password, String fullName, UserRole role) {
        try {
            authClient.adminCreateUser(email, password, fullName, role.name());
        } catch (RestClientResponseException e) {
            throw new AccountCreationException(friendlyMessage(e), e);
        }
    }

    private String friendlyMessage(RestClientResponseException e) {
        String body = e.getResponseBodyAsString().toLowerCase();
        int status = e.getStatusCode().value();

        if (status == 409 || status == 422
                || body.contains("already") || body.contains("registered") || body.contains("exists")) {
            return "A user with this email already exists.";
        }
        if (body.contains("password")) {
            return "Password does not meet the requirements (at least 6 characters).";
        }
        return "Could not create the account. Please check the details and try again.";
    }
}
