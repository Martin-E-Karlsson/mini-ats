package com.mek.miniats.candidate.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.common.CurrentUser;
import com.mek.miniats.storage.CvStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/candidates/{id}/cv")
public class CvController {

    private final CandidateService candidateService;
    private final CvStorageService cvStorage;

    public CvController(CandidateService candidateService, CvStorageService cvStorage) {
        this.candidateService = candidateService;
        this.cvStorage = cvStorage;
    }

    @PostMapping
    public String upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
                         @AuthenticationPrincipal SupabasePrincipal me, Authentication auth) throws IOException {
        boolean admin = CurrentUser.isAdmin(auth);
        candidateService.get(id, me.id(), admin); // authorization + existence check
        if (!file.isEmpty()) {
            String key = cvStorage.store(id, file.getOriginalFilename(), file.getBytes(), file.getContentType());
            candidateService.attachCv(id, key, file.getOriginalFilename(), me.id(), admin);
        }
        return "redirect:/candidates/" + id + "/edit";
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
                                           @AuthenticationPrincipal SupabasePrincipal me, Authentication auth) {
        boolean admin = CurrentUser.isAdmin(auth);
        Candidate candidate = candidateService.get(id, me.id(), admin);
        if (candidate.getCvPath() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = cvStorage.download(candidate.getCvPath());
        String filename = candidate.getCvFilename() == null ? "cv" : candidate.getCvFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
