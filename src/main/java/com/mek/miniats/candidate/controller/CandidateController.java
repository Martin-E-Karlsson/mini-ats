package com.mek.miniats.candidate.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.candidate.CandidateStage;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.service.CandidateService;
import com.mek.miniats.common.CurrentUser;
import com.mek.miniats.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.beans.PropertyEditorSupport;
import java.util.UUID;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    private final CandidateService service;
    private final JobService jobService;

    public CandidateController(CandidateService service, JobService jobService) {
        this.service = service;
        this.jobService = jobService;
    }

    /**
     * The "— No job —" option submits an empty string; without this, binding ""
     * to the UUID jobId field would fail. Treat blank as null instead.
     */
    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(UUID.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : UUID.fromString(text.trim()));
            }
        });
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) UUID jobId,
                          @AuthenticationPrincipal SupabasePrincipal me, Authentication auth, Model model) {
        Candidate candidate = new Candidate();
        candidate.setJobId(jobId); // optional pre-selection from the board
        model.addAttribute("candidate", candidate);
        model.addAttribute("jobs", jobService.list(me.id(), CurrentUser.isAdmin(auth)));
        return "candidates/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("candidate") Candidate candidate, BindingResult binding,
                         @AuthenticationPrincipal SupabasePrincipal me, Authentication auth, Model model) {
        boolean admin = CurrentUser.isAdmin(auth);
        if (binding.hasErrors()) {
            model.addAttribute("jobs", jobService.list(me.id(), admin));
            return "candidates/form";
        }
        service.create(candidate, me.id(), admin, null);
        return "redirect:/board";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                           Authentication auth, Model model) {
        boolean admin = CurrentUser.isAdmin(auth);
        model.addAttribute("candidate", service.get(id, me.id(), admin));
        model.addAttribute("jobs", jobService.list(me.id(), admin));
        return "candidates/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @Valid @ModelAttribute("candidate") Candidate candidate,
                         BindingResult binding, @AuthenticationPrincipal SupabasePrincipal me,
                         Authentication auth, Model model) {
        boolean admin = CurrentUser.isAdmin(auth);
        if (binding.hasErrors()) {
            model.addAttribute("jobs", jobService.list(me.id(), admin));
            return "candidates/form";
        }
        service.update(id, candidate, me.id(), admin);
        return "redirect:/board";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                         Authentication auth) {
        service.delete(id, me.id(), CurrentUser.isAdmin(auth));
        return "redirect:/board";
    }

    /** Drag-and-drop endpoint: the board posts the new stage + ordering here via fetch. */
    @PostMapping("/{id}/move")
    @ResponseBody
    public ResponseEntity<Void> move(@PathVariable UUID id,
                                     @RequestParam CandidateStage stage,
                                     @RequestParam(defaultValue = "0") double position,
                                     @AuthenticationPrincipal SupabasePrincipal me, Authentication auth) {
        service.moveToStage(id, stage, position, me.id(), CurrentUser.isAdmin(auth));
        return ResponseEntity.noContent().build();
    }
}
