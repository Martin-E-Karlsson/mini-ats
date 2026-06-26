package com.mek.miniats.user.controller;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.admin.service.AdminUserService;
import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.user.ProfileForm;
import com.mek.miniats.user.entity.Profile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final int MIN_PASSWORD = 8;

    private final AdminUserService service;

    public ProfileController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public String form(@AuthenticationPrincipal SupabasePrincipal me, Model model) {
        Profile profile = service.get(me.id());
        ProfileForm form = new ProfileForm();
        form.setFullName(profile.getFullName());
        form.setEmail(profile.getEmail());
        model.addAttribute("form", form);
        model.addAttribute("role", profile.getRole()); // shown read-only
        return "profile/edit";
    }

    @PostMapping
    public String update(@AuthenticationPrincipal SupabasePrincipal me,
                         @Valid @ModelAttribute("form") ProfileForm form, BindingResult binding, Model model) {
        validateOptionalPassword(form.getNewPassword(), form.getConfirmPassword(), binding);
        if (binding.hasErrors()) {
            model.addAttribute("role", service.get(me.id()).getRole());
            return "profile/edit";
        }
        try {
            service.updateOwnProfile(me.id(), form.getEmail(), form.getFullName(), form.getNewPassword());
        } catch (AccountCreationException e) {
            model.addAttribute("role", service.get(me.id()).getRole());
            model.addAttribute("error", e.getMessage());
            return "profile/edit";
        }
        return "redirect:/profile?updated";
    }

    private void validateOptionalPassword(String pw, String confirm, BindingResult binding) {
        if (pw == null || pw.isBlank()) {
            return;
        }
        if (pw.length() < MIN_PASSWORD) {
            binding.rejectValue("newPassword", "Short", "Password must be at least 8 characters");
        }
        if (!pw.equals(confirm)) {
            binding.rejectValue("confirmPassword", "Mismatch", "Passwords do not match");
        }
    }
}
