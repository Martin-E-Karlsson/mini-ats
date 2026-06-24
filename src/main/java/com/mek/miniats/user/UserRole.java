package com.mek.miniats.user;

/**
 * Application role. Constants are lowercase on purpose so that
 * {@code @Enumerated(EnumType.STRING)} stores values that match the
 * profiles.role CHECK constraint ('admin','customer') in the database.
 */
public enum UserRole {
    admin,
    customer
}
