package com.mek.miniats.candidate.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.CandidateStage;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CandidateController.class)
@Import(SecurityConfig.class)
class CandidateControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CandidateService candidateService;

    @MockitoBean
    JobService jobService;

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
    void create_validCandidate_savesAndRedirectsToBoard() throws Exception {
        when(candidateService.create(any(Candidate.class), eq(userId), eq(false), isNull()))
                .thenReturn(new Candidate());

        mvc.perform(post("/candidates").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("fullName", "Ada Lovelace"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board"));

        verify(candidateService).create(any(Candidate.class), eq(userId), eq(false), isNull());
    }

    @Test
    void create_blankName_returnsFormWithoutSaving() throws Exception {
        when(jobService.list(eq(userId), eq(false))).thenReturn(List.of());

        mvc.perform(post("/candidates").with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("fullName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("candidates/form"));
    }

    @Test
    void move_updatesStageAndReturnsNoContent() throws Exception {
        UUID candidateId = UUID.randomUUID();

        mvc.perform(post("/candidates/{id}/move", candidateId)
                        .with(signedIn("ROLE_CUSTOMER")).with(csrf())
                        .param("stage", "interview")
                        .param("position", "2"))
                .andExpect(status().isNoContent());

        verify(candidateService).moveToStage(candidateId, CandidateStage.interview, 2.0, userId, false);
    }

    @Test
    void move_withoutCsrf_isForbidden() throws Exception {
        mvc.perform(post("/candidates/{id}/move", UUID.randomUUID())
                        .with(signedIn("ROLE_CUSTOMER"))
                        .param("stage", "interview"))
                .andExpect(status().isForbidden());
    }
}
