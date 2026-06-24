package com.mek.miniats.auth;

import java.util.UUID;

/**
 * The authenticated user as the rest of the app sees it. Carries the Supabase
 * user id (== profiles.id, used for owner checks later) and the access token
 * (handy if you ever forward calls to Supabase on the user's behalf).
 *
 * toString() returns the email so that Authentication#getName() is the email.
 */
public record SupabasePrincipal(UUID id, String email, String fullName, String accessToken) {

    @Override
    public String toString() {
        return email;
    }
}
