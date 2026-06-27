package com.mek.miniats.admin;

/** Raised when Supabase Auth rejects an admin account-creation request. */
public class AccountCreationException extends RuntimeException {

    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
