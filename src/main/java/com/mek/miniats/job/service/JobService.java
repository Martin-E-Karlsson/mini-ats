package com.mek.miniats.job.service;

import com.mek.miniats.common.ResourceNotFoundException;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * All methods take the acting user's id and whether they're an admin. This is
 * the "RLS in Java": customers only ever touch their own rows; admins touch any.
 */
@Service
@Transactional
public class JobService {

    private final JobRepository jobs;

    public JobService(JobRepository jobs) {
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public List<Job> list(UUID userId, boolean isAdmin) {
        return isAdmin
                ? jobs.findAllByOrderByCreatedAtDesc()
                : jobs.findByOwnerIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Job get(UUID id, UUID userId, boolean isAdmin) {
        Job job = jobs.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (!isAdmin && !job.getOwnerId().equals(userId)) {
            throw new ResourceNotFoundException(); // hide existence from other tenants
        }
        return job;
    }

    /**
     * @param ownerOverride when an admin creates a job on behalf of a customer,
     *                      pass that customer's id; otherwise null.
     */
    public Job create(Job job, UUID userId, boolean isAdmin, UUID ownerOverride) {
        job.setOwnerId(isAdmin && ownerOverride != null ? ownerOverride : userId);
        return jobs.save(job);
    }

    public Job update(UUID id, Job form, UUID userId, boolean isAdmin) {
        Job job = get(id, userId, isAdmin);
        job.setTitle(form.getTitle());
        job.setDescription(form.getDescription());
        job.setLocation(form.getLocation());
        job.setOpen(form.isOpen());
        return jobs.save(job);
    }

    public void delete(UUID id, UUID userId, boolean isAdmin) {
        jobs.delete(get(id, userId, isAdmin));
    }
}
