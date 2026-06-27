package com.mek.miniats.candidate.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.config.SecurityConfig;
import com.mek.miniats.storage.CvStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvController.class)
@Import(SecurityConfig.class)
class CvControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CandidateService candidateService;

    @MockitoBean
    CvStorageService cvStorage;

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
        return c;
    }

    @Test
    void upload_storesFileAttachesAndRedirects() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.get(eq(id), eq(userId), eq(false))).thenReturn(candidate(id));
        when(cvStorage.store(eq(id), eq("resume.pdf"), any(), eq("application/pdf"))).thenReturn("key123");

        var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf-bytes".getBytes());

        mvc.perform(multipart("/candidates/{id}/cv", id).file(file).with(signedIn()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/candidates/" + id + "/edit"));

        verify(cvStorage).store(eq(id), eq("resume.pdf"), any(), eq("application/pdf"));
        verify(candidateService).attachCv(eq(id), eq("key123"), eq("resume.pdf"), eq(userId), eq(false));
    }

    @Test
    void download_returnsFileBytesAsAttachment() throws Exception {
        UUID id = UUID.randomUUID();
        Candidate c = candidate(id);
        c.setCvPath("key123");
        c.setCvFilename("resume.pdf");
        when(candidateService.get(eq(id), eq(userId), eq(false))).thenReturn(c);
        when(cvStorage.download("key123")).thenReturn("pdf-bytes".getBytes());

        mvc.perform(get("/candidates/{id}/cv", id).with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("resume.pdf")));
    }

    @Test
    void download_whenNoCv_returnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidateService.get(eq(id), eq(userId), eq(false))).thenReturn(candidate(id)); // cvPath null

        mvc.perform(get("/candidates/{id}/cv", id).with(signedIn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_withoutCsrf_isForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "x".getBytes());
        mvc.perform(multipart("/candidates/{id}/cv", id).file(file).with(signedIn()))
                .andExpect(status().isForbidden());
    }
}
