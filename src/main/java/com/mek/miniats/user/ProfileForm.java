package com.mek.miniats.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Self-service profile form. Deliberately has no role field — a user cannot
 * change their own role. Password is optional (blank = unchanged).
 */
@Getter
@Setter
public class ProfileForm {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    private String newPassword;

    private String confirmPassword;
}
