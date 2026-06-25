package com.mek.miniats.board;

import com.mek.miniats.auth.SupabasePrincipal;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
class BoardControllerTest {

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
    void anonymous_isRedirectedToLogin() throws Exception {
        mvc.perform(get("/board"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void customer_seesBoardWithColumns() throws Exception {
        when(candidateService.list(eq(userId), eq(false), isNull(), isNull())).thenReturn(List.of());
        when(jobService.list(eq(userId), eq(false))).thenReturn(List.of());

        mvc.perform(get("/board").with(signedIn("ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("board"))
                .andExpect(model().attributeExists("columns"))
                .andExpect(model().attributeExists("jobs"));

        verify(candidateService).list(userId, false, null, null);
    }

    @Test
    void filtersArePassedThroughToTheService() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(candidateService.list(eq(userId), eq(false), eq(jobId), eq("ada"))).thenReturn(List.of());
        when(jobService.list(eq(userId), eq(false))).thenReturn(List.of());

        mvc.perform(get("/board").param("jobId", jobId.toString()).param("name", "ada")
                        .with(signedIn("ROLE_CUSTOMER")))
                .andExpect(status().isOk());

        verify(candidateService).list(userId, false, jobId, "ada");
    }
}
