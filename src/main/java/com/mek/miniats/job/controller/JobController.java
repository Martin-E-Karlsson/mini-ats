package com.mek.miniats.job.controller;

import com.mek.miniats.auth.SupabasePrincipal;
import com.mek.miniats.common.CurrentUser;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal SupabasePrincipal me, Authentication auth, Model model) {
        model.addAttribute("jobs", service.list(me.id(), CurrentUser.isAdmin(auth)));
        return "jobs/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("job", new Job());
        return "jobs/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal SupabasePrincipal me, Authentication auth,
                         @Valid @ModelAttribute("job") Job job, BindingResult binding) {
        if (binding.hasErrors()) {
            return "jobs/form";
        }
        service.create(job, me.id(), CurrentUser.isAdmin(auth), null);
        return "redirect:/jobs";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                           Authentication auth, Model model) {
        model.addAttribute("job", service.get(id, me.id(), CurrentUser.isAdmin(auth)));
        return "jobs/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                         Authentication auth, @Valid @ModelAttribute("job") Job job, BindingResult binding) {
        if (binding.hasErrors()) {
            return "jobs/form";
        }
        service.update(id, job, me.id(), CurrentUser.isAdmin(auth));
        return "redirect:/jobs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal SupabasePrincipal me,
                         Authentication auth) {
        service.delete(id, me.id(), CurrentUser.isAdmin(auth));
        return "redirect:/jobs";
    }
}
