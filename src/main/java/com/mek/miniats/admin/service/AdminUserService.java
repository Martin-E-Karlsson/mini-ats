package com.mek.miniats.admin.service;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.auth.SupabaseAuthClient;
import com.mek.miniats.common.ResourceNotFoundException;
import com.mek.miniats.user.UserRole;
import com.mek.miniats.user.entity.Profile;
import com.mek.miniats.user.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Account management on top of Supabase Auth. Used by both the admin screens
 * (create/edit/delete any account, including role) and the self-service profile
 * screen (a user editing their own details, never their own role).
 *
 * Identity (email/password) lives in Supabase Auth; full_name and role live in
 * the profiles table, which the app reads for display and authorization.
 */
@Service
@Transactional
public class AdminUserService {

    private final SupabaseAuthClient authClient;
    private final ProfileRepository profiles;

    public AdminUserService(SupabaseAuthClient authClient, ProfileRepository profiles) {
        this.authClient = authClient;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<Profile> list() {
        return profiles.findAllByOrderByEmailAsc();
    }

    @Transactional(readOnly = true)
    public Profile get(UUID id) {
        return profiles.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    public void createAccount(String email, String password, String fullName, UserRole role) {
        try {
            authClient.adminCreateUser(email, password, fullName, role.name());
        } catch (RestClientResponseException e) {
            throw new AccountCreationException(friendlyMessage(e), e);
        }
    }

    /** Admin edit: may change the role. */
    public void updateAccount(UUID id, String email, String fullName, UserRole role, String newPassword) {
        applyAuthChanges(id, email, fullName, newPassword);
        Profile profile = get(id);
        profile.setEmail(email);
        profile.setFullName(fullName);
        profile.setRole(role);
        profiles.save(profile);
    }

    /** Self-service edit: keeps the existing role (a user can't change their own). */
    public void updateOwnProfile(UUID id, String email, String fullName, String newPassword) {
        Profile profile = get(id);
        applyAuthChanges(id, email, fullName, newPassword);
        profile.setEmail(email);
        profile.setFullName(fullName);
        profiles.save(profile);
    }

    public void deleteAccount(UUID id) {
        try {
            authClient.adminDeleteUser(id.toString());
        } catch (RestClientResponseException e) {
            throw new AccountCreationException(friendlyMessage(e), e);
        }
        // profiles row is removed by the ON DELETE CASCADE foreign key.
    }

    private void applyAuthChanges(UUID id, String email, String fullName, String newPassword) {
        try {
            authClient.adminUpdateUser(id.toString(), email, newPassword, fullName);
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
            return "Password does not meet the requirements (at least 8 characters).";
        }
        return "Could not save the account. Please check the details and try again.";
    }
}
