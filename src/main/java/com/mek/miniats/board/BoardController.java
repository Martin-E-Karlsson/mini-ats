package com.mek.miniats.board;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.CandidateStage;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.common.CurrentUser;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.service.JobService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class BoardController {

    private final CandidateService candidateService;
    private final JobService jobService;

    public BoardController(CandidateService candidateService, JobService jobService) {
        this.candidateService = candidateService;
        this.jobService = jobService;
    }

    @GetMapping("/board")
    public String board(@RequestParam(required = false) UUID jobId,
                        @RequestParam(required = false) String name,
                        @AuthenticationPrincipal SupabasePrincipal me,
                        Authentication auth,
                        Model model) {
        boolean admin = CurrentUser.isAdmin(auth);

        List<Candidate> candidates = candidateService.list(me.id(), admin, jobId, name);
        List<Job> jobs = jobService.list(me.id(), admin);

        // One column per stage, in declared order, even if empty.
        Map<CandidateStage, List<Candidate>> columns = new LinkedHashMap<>();
        for (CandidateStage stage : CandidateStage.values()) {
            columns.put(stage, new ArrayList<>());
        }
        for (Candidate c : candidates) {
            columns.get(c.getStage()).add(c);
        }

        Map<UUID, String> jobTitles = jobs.stream()
                .collect(Collectors.toMap(Job::getId, Job::getTitle));

        model.addAttribute("columns", columns);
        model.addAttribute("jobTitles", jobTitles);
        model.addAttribute("jobs", jobs);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("nameFilter", name);
        return "board";
    }
}
