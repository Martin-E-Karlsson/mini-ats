package com.mek.miniats.admin.controller;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.admin.service.AdminUserService;
import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.config.SecurityConfig;
import com.mek.miniats.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminUserService adminUserService;

    @MockitoBean
    AuthenticationProvider supabaseAuthenticationProvider;

    private RequestPostProcessor signedIn(String role) {
        var principal = new SupabasePrincipal(UUID.randomUUID(), "u@b.com", "User", "token");
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        return authentication(token);
    }

    @Test
    void customer_cannotAccessCreateForm() throws Exception {
        mvc.perform(get("/admin/users/new").with(signedIn("ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_getsCreateForm() throws Exception {
        mvc.perform(get("/admin/users/new").with(signedIn("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"))
                .andExpect(model().attributeExists("form", "roles"));
    }

    @Test
    void admin_createsAccount_redirects() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("email", "new@b.com")
                        .param("password", "secret123")
                        .param("fullName", "New User")
                        .param("role", "customer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/new?created"));

        verify(adminUserService).createAccount("new@b.com", "secret123", "New User", UserRole.customer);
    }

    @Test
    void duplicateEmail_returnsFormWithError() throws Exception {
        doThrow(new AccountCreationException("A user with this email already exists.", null))
                .when(adminUserService).createAccount(eq("dupe@b.com"), eq("secret123"), eq("Dupe"), eq(UserRole.customer));

        mvc.perform(post("/admin/users").with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("email", "dupe@b.com")
                        .param("password", "secret123")
                        .param("fullName", "Dupe")
                        .param("role", "customer"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void invalidForm_blankEmail_returnsForm() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("email", "")
                        .param("password", "secret123")
                        .param("fullName", "No Email")
                        .param("role", "customer"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"));
    }

    @Test
    void customer_cannotPostCreate() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("email", "x@b.com")
                        .param("password", "secret123")
                        .param("fullName", "X")
                        .param("role", "customer"))
                .andExpect(status().isForbidden());
    }
}
