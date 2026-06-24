package com.mek.miniats.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/user/login")
    public String login() {
        return "user/login";
    }

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("name", auth.getName());
        model.addAttribute("role", admin ? "Admin" : "Customer");
        return "home";
    }
}
