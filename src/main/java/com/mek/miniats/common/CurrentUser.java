package com.mek.miniats.common;

import org.springframework.security.core.Authentication;

/** Small helpers for reading the acting user's role from the security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
