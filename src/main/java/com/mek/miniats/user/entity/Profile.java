package com.mek.miniats.user.entity;

import com.mek.miniats.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors a Supabase Auth user (auth.users). The row is created automatically
 * by the on_auth_user_created trigger, so the app mostly READS this table.
 * The id is NOT generated here — it equals auth.users.id, which Supabase owns.
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
public class Profile {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String email;

    @Column(name = "full_name", nullable = false, columnDefinition = "text")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private UserRole role;

    // Set by the database default (now()); we never write it.
    @Column(name = "created_at", columnDefinition = "timestamptz",
            insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
