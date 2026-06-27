package com.mek.miniats.admin;

import com.mek.miniats.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin form for editing an existing account. Password is optional here —
 * a blank new password leaves the current one unchanged.
 */
@Getter
@Setter
public class EditUserForm {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UserRole role;

    // Optional; if set, must be >= 8 chars and match confirmPassword (checked in the controller).
    private String newPassword;

    private String confirmPassword;
}
