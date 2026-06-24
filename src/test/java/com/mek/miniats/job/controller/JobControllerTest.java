package com.mek.miniats.job.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.config.SecurityConfig;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.service.JobService;
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

@WebMvcTest(JobController.class)
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JobService jobService;

    // SecurityConfig requires an AuthenticationProvider bean; the slice has no DB,
    // so we mock it (its real collaborators are never constructed here).
    @MockitoBean
    AuthenticationProvider supabaseAuthenticationProvider;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor signedIn(String role) {
        var principal = new SupabasePrincipal(userId, "u@b.com", "User", "token");
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        return authentication(token);
    }

    @Test
    void anonymous_isRedirectedToLogin() throws Exception {
        mvc.perform(get("/jobs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void customer_seesJobList() throws Exception {
        when(jobService.list(eq(userId), eq(false))).thenReturn(List.of());

        mvc.perform(get("/jobs").with(signedIn("ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"))
                .andExpect(model().attributeExists("jobs"));

        verify(jobService).list(userId, false);
    }

    @Test
    void create_validJob_savesAndRedirects() throws Exception {
        when(jobService.create(any(Job.class), eq(userId), eq(false), isNull())).thenReturn(new Job());

        mvc.perform(post("/jobs").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("title", "Backend Engineer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        verify(jobService).create(any(Job.class), eq(userId), eq(false), isNull());
    }

    @Test
    void create_blankTitle_returnsFormWithoutSaving() throws Exception {
        mvc.perform(post("/jobs").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("title", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/form"));
    }

    @Test
    void post_withoutCsrf_isForbidden() throws Exception {
        mvc.perform(post("/jobs").with(signedIn("ROLE_CUSTOMER"))
                        .param("title", "Backend Engineer"))
                .andExpect(status().isForbidden());
    }
}
