package com.mek.miniats.admin;

import com.mek.miniats.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Backing object for the admin "create account" form. */
@Getter
@Setter
public class CreateAccountForm {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    private String fullName;

    @NotNull
    private UserRole role = UserRole.customer;
}
