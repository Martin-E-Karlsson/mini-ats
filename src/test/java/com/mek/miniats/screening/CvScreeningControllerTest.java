package com.mek.miniats.screening;

import com.mek.miniats.auth.SupabasePrincipal;
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

@WebMvcTest(CvScreeningController.class)
@Import(SecurityConfig.class)
class CvScreeningControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CandidateService candidateService;

    @MockitoBean
    JobService jobService;

    @MockitoBean
    CvScreeningService screeningService;

    @MockitoBean
    com.mek.miniats.storage.CvStorageService cvStorageService;

    @MockitoBean
    com.mek.miniats.storage.CvTextExtractor cvTextExtractor;

    @MockitoBean
    AuthenticationProvider supabaseAuthenticationProvider;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor signedIn() {
        var principal = new SupabasePrincipal(userId, "u@b.com", "User", "token");
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return authentication(token);
    }

    private Candidate candidate(UUID id) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setOwnerId(userId);
        c.setFullName("Ada Lovelace");
        c.setJobId(null); // no job => no jobService lookup needed
        return c;
    }

    @Test
    void anonymous_isRedirectedToLogin() throws Exception {
        mvc.perform(get("/candidates/{id}/screen", UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void getForm_rendersScreeningPage() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.get(eq(id), eq(userId), eq(false))).thenReturn(candidate(id));

        mvc.perform(get("/candidates/{id}/screen", id).with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(view().name("screening/screen"))
                .andExpect(model().attributeExists("candidate", "jobTitle"));
    }

    @Test
    void post_runsScreeningAndShowsAssessment() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.get(eq(id), eq(userId), eq(false))).thenReturn(candidate(id));
        when(screeningService.screen(eq("Ada Lovelace"), any(), any(), eq("CV text here")))
                .thenReturn("Fit score: 8/10");

        mvc.perform(post("/candidates/{id}/screen", id).with(signedIn()).with(csrf())
                        .param("cvText", "CV text here"))
                .andExpect(status().isOk())
                .andExpect(view().name("screening/screen"))
                .andExpect(model().attribute("assessment", "Fit score: 8/10"));

        verify(screeningService).screen(eq("Ada Lovelace"), any(), any(), eq("CV text here"));
    }

    @Test
    void post_withoutCsrf_isForbidden() throws Exception {
        mvc.perform(post("/candidates/{id}/screen", UUID.randomUUID()).with(signedIn())
                        .param("cvText", "CV text here"))
                .andExpect(status().isForbidden());
    }
}
