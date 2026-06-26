package com.mek.miniats.screening;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.common.CurrentUser;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.service.JobService;
import com.mek.miniats.storage.CvStorageService;
import com.mek.miniats.storage.CvTextExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/candidates/{id}/screen")
public class CvScreeningController {

    private final CandidateService candidateService;
    private final JobService jobService;
    private final CvScreeningService screeningService;
    private final CvStorageService cvStorage;
    private final CvTextExtractor cvTextExtractor;

    public CvScreeningController(CandidateService candidateService, JobService jobService,
                                CvScreeningService screeningService, CvStorageService cvStorage,
                                CvTextExtractor cvTextExtractor) {
        this.candidateService = candidateService;
        this.jobService = jobService;
        this.screeningService = screeningService;
        this.cvStorage = cvStorage;
        this.cvTextExtractor = cvTextExtractor;
    }

    @GetMapping
    public String form(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                       Authentication auth, Model model) {
        boolean admin = CurrentUser.isAdmin(auth);
        Candidate candidate = candidateService.get(id, me.id(), admin);
        model.addAttribute("candidate", candidate);
        model.addAttribute("jobTitle", jobTitleFor(candidate, me.id(), admin));

        // If a CV is on file, pre-fill the textarea with its extracted text.
        if (candidate.getCvPath() != null) {
            try {
                byte[] bytes = cvStorage.download(candidate.getCvPath());
                model.addAttribute("cvText", cvTextExtractor.extract(bytes, candidate.getCvFilename()));
            } catch (Exception e) {
                // Extraction/download failed — leave the box empty so the user can paste.
            }
        }
        return "screening/screen";
    }

    @PostMapping
    public String run(@PathVariable UUID id, @RequestParam String cvText,
                      @AuthenticationPrincipal SupabasePrincipal me, Authentication auth, Model model) {
        boolean admin = CurrentUser.isAdmin(auth);
        Candidate candidate = candidateService.get(id, me.id(), admin);
        Job job = candidate.getJobId() == null ? null
                : jobService.get(candidate.getJobId(), me.id(), admin);

        String jobTitle = job != null ? job.getTitle() : "(no specific role)";
        String jobDescription = job != null ? job.getDescription() : "";

        try {
            String assessment = screeningService.screen(
                    candidate.getFullName(), jobTitle, jobDescription, cvText);
            model.addAttribute("assessment", assessment);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("candidate", candidate);
        model.addAttribute("jobTitle", jobTitle);
        model.addAttribute("cvText", cvText);
        return "screening/screen";
    }

    private String jobTitleFor(Candidate candidate, UUID userId, boolean admin) {
        if (candidate.getJobId() == null) {
            return "(no specific role)";
        }
        return jobService.get(candidate.getJobId(), userId, admin).getTitle();
    }
}
