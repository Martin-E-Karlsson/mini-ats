package com.mek.miniats.admin.controller;

import com.mek.miniats.admin.service.AdminUserService;
import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.config.SecurityConfig;
import com.mek.miniats.user.UserRole;
import com.mek.miniats.user.entity.Profile;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    AdminUserService service;

    @MockitoBean
    AuthenticationProvider supabaseAuthenticationProvider;

    private final UUID adminId = UUID.randomUUID();

    private RequestPostProcessor signedIn(String role) {
        var principal = new SupabasePrincipal(adminId, "admin@b.com", "Admin", "token");
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        return authentication(token);
    }

    // ── list ──────────────────────────────────────────────────────────────

    @Test
    void customer_cannotSeeUsersList() throws Exception {
        mvc.perform(get("/admin/users").with(signedIn("ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_seesUsersList() throws Exception {
        when(service.list()).thenReturn(List.of());
        mvc.perform(get("/admin/users").with(signedIn("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void admin_getsCreateForm() throws Exception {
        mvc.perform(get("/admin/users/new").with(signedIn("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"))
                .andExpect(model().attributeExists("form", "roles"));
    }

    @Test
    void create_matchingPasswords_redirectsToList() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("email", "new@b.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("fullName", "New User")
                        .param("role", "customer"))
                .andExpect(redirectedUrl("/admin/users"));

        verify(service).createAccount("new@b.com", "secret123", "New User", UserRole.customer);
    }

    @Test
    void create_mismatchedPasswords_returnsFormWithoutSaving() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("email", "new@b.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "different")
                        .param("fullName", "New User")
                        .param("role", "customer"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"));

        verify(service, never()).createAccount(any(), any(), any(), any());
    }

    // ── edit / update / delete ────────────────────────────────────────────

    @Test
    void admin_getsEditForm() throws Exception {
        UUID otherId = UUID.randomUUID();
        Profile p = new Profile();
        p.setId(otherId);
        p.setEmail("x@b.com");
        p.setFullName("X");
        p.setRole(UserRole.customer);
        when(service.get(otherId)).thenReturn(p);

        mvc.perform(get("/admin/users/{id}/edit", otherId).with(signedIn("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/edit-user"))
                .andExpect(model().attributeExists("form", "userId", "roles", "editingSelf"));
    }

    @Test
    void update_otherUser_redirectsAndSaves() throws Exception {
        UUID otherId = UUID.randomUUID();
        mvc.perform(post("/admin/users/{id}", otherId).with(signedIn("ROLE_ADMIN")).with(csrf())
                        .param("fullName", "New Name")
                        .param("email", "new@b.com")
                        .param("role", "admin"))
                .andExpect(redirectedUrl("/admin/users"));

        verify(service).updateAccount(eq(otherId), eq("new@b.com"), eq("New Name"), eq(UserRole.admin), isNull());
    }

    @Test
    void delete_otherUser_callsService() throws Exception {
        UUID otherId = UUID.randomUUID();
        mvc.perform(post("/admin/users/{id}/delete", otherId).with(signedIn("ROLE_ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));
        verify(service).deleteAccount(otherId);
    }

    @Test
    void delete_self_isIgnored() throws Exception {
        mvc.perform(post("/admin/users/{id}/delete", adminId).with(signedIn("ROLE_ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));
        verify(service, never()).deleteAccount(any());
    }

    @Test
    void customer_cannotPostCreate() throws Exception {
        mvc.perform(post("/admin/users").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("email", "x@b.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("fullName", "X")
                        .param("role", "customer"))
                .andExpect(status().isForbidden());
    }
}
