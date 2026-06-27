package com.mek.miniats.admin.controller;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.admin.CreateAccountForm;
import com.mek.miniats.admin.EditUserForm;
import com.mek.miniats.admin.service.AdminUserService;
import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.user.UserRole;
import com.mek.miniats.user.entity.Profile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminController {

    private static final int MIN_PASSWORD = 8;

    private final AdminUserService service;

    public AdminController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", service.list());
        return "admin/users";
    }

    // ── Create ────────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CreateAccountForm());
        model.addAttribute("roles", UserRole.values());
        return "admin/create-user";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateAccountForm form,
                         BindingResult binding, Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "Mismatch", "Passwords do not match");
        }
        if (binding.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "admin/create-user";
        }
        try {
            service.createAccount(form.getEmail(), form.getPassword(), form.getFullName(), form.getRole());
        } catch (AccountCreationException e) {
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("error", e.getMessage());
            return "admin/create-user";
        }
        return "redirect:/admin/users";
    }

    // ── Edit any user ─────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me, Model model) {
        Profile profile = service.get(id);
        EditUserForm form = new EditUserForm();
        form.setFullName(profile.getFullName());
        form.setEmail(profile.getEmail());
        form.setRole(profile.getRole());
        model.addAttribute("form", form);
        model.addAttribute("userId", id);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("editingSelf", id.equals(me.id()));
        return "admin/edit-user";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                         @Valid @ModelAttribute("form") EditUserForm form, BindingResult binding, Model model) {
        validateOptionalPassword(form.getNewPassword(), form.getConfirmPassword(), binding);
        boolean editingSelf = id.equals(me.id());
        if (binding.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("editingSelf", editingSelf);
            return "admin/edit-user";
        }
        // Never let an admin change their own role here (avoids self-lockout).
        UserRole role = editingSelf ? service.get(id).getRole() : form.getRole();
        try {
            service.updateAccount(id, form.getEmail(), form.getFullName(), role, form.getNewPassword());
        } catch (AccountCreationException e) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("editingSelf", editingSelf);
            model.addAttribute("error", e.getMessage());
            return "admin/edit-user";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me) {
        if (!id.equals(me.id())) { // can't delete yourself
            service.deleteAccount(id);
        }
        return "redirect:/admin/users";
    }

    private void validateOptionalPassword(String pw, String confirm, BindingResult binding) {
        if (pw == null || pw.isBlank()) {
            return; // leaving it blank keeps the existing password
        }
        if (pw.length() < MIN_PASSWORD) {
            binding.rejectValue("newPassword", "Short", "Password must be at least 8 characters");
        }
        if (!pw.equals(confirm)) {
            binding.rejectValue("confirmPassword", "Mismatch", "Passwords do not match");
        }
    }
}
