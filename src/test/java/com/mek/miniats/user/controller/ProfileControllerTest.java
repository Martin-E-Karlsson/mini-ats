package com.mek.miniats.user.controller;

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

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminUserService service;

    @MockitoBean
    AuthenticationProvider supabaseAuthenticationProvider;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor signedIn() {
        var principal = new SupabasePrincipal(userId, "u@b.com", "User", "token");
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return authentication(token);
    }

    private Profile profile() {
        Profile p = new Profile();
        p.setId(userId);
        p.setEmail("u@b.com");
        p.setFullName("User");
        p.setRole(UserRole.customer);
        return p;
    }

    @Test
    void anonymous_isRedirectedToLogin() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void getProfile_rendersFormWithRole() throws Exception {
        when(service.get(userId)).thenReturn(profile());
        mvc.perform(get("/profile").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("form", "role"));
    }

    @Test
    void postProfile_valid_updatesOwnProfileAndRedirects() throws Exception {
        mvc.perform(post("/profile").with(signedIn()).with(csrf())
                        .param("fullName", "New Name")
                        .param("email", "new@b.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(service).updateOwnProfile(eq(userId), eq("new@b.com"), eq("New Name"), isNull());
    }

    @Test
    void postProfile_passwordMismatch_returnsFormWithoutSaving() throws Exception {
        when(service.get(userId)).thenReturn(profile());

        mvc.perform(post("/profile").with(signedIn()).with(csrf())
                        .param("fullName", "New Name")
                        .param("email", "new@b.com")
                        .param("newPassword", "password8")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));

        verify(service, never()).updateOwnProfile(any(), any(), any(), any());
    }
}
