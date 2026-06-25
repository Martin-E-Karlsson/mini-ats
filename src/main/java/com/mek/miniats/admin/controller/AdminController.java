package com.mek.miniats.admin.controller;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.admin.CreateAccountForm;
import com.mek.miniats.admin.service.AdminUserService;
import com.mek.miniats.user.UserRole;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CreateAccountForm());
        model.addAttribute("roles", UserRole.values());
        return "admin/create-user";
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("form") CreateAccountForm form,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "admin/create-user";
        }
        try {
            adminUserService.createAccount(form.getEmail(), form.getPassword(),
                    form.getFullName(), form.getRole());
        } catch (AccountCreationException e) {
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("error", e.getMessage());
            return "admin/create-user";
        }
        return "redirect:/admin/users/new?created";
    }
}
