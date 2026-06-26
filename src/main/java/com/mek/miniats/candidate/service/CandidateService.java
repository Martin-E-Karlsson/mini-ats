package com.mek.miniats.candidate.service;

import com.mek.miniats.candidate.CandidateStage;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.repository.CandidateRepository;
import com.mek.miniats.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owner/admin isolation, identical in spirit to JobService: customers only ever
 * touch their own candidates; admins touch any. Listing also applies the board's
 * optional filters (by job and by name).
 */
@Service
@Transactional
public class CandidateService {

    private final CandidateRepository candidates;

    public CandidateService(CandidateRepository candidates) {
        this.candidates = candidates;
    }

    @Transactional(readOnly = true)
    public List<Candidate> list(UUID userId, boolean isAdmin, UUID jobFilter, String nameFilter) {
        List<Candidate> base = isAdmin
                ? candidates.findAllByOrderByPositionAsc()
                : candidates.findByOwnerIdOrderByPositionAsc(userId);

        String needle = nameFilter == null ? "" : nameFilter.trim().toLowerCase();

        return base.stream()
                .filter(c -> jobFilter == null || jobFilter.equals(c.getJobId()))
                .filter(c -> needle.isEmpty() || c.getFullName().toLowerCase().contains(needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public Candidate get(UUID id, UUID userId, boolean isAdmin) {
        Candidate c = candidates.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (!isAdmin && !c.getOwnerId().equals(userId)) {
            throw new ResourceNotFoundException();
        }
        return c;
    }

    public Candidate create(Candidate candidate, UUID userId, boolean isAdmin, UUID ownerOverride) {
        candidate.setOwnerId(isAdmin && ownerOverride != null ? ownerOverride : userId);
        return candidates.save(candidate);
    }

    public Candidate update(UUID id, Candidate form, UUID userId, boolean isAdmin) {
        Candidate c = get(id, userId, isAdmin);
        c.setFullName(form.getFullName());
        c.setEmail(form.getEmail());
        c.setLinkedinUrl(form.getLinkedinUrl());
        c.setNotes(form.getNotes());
        c.setJobId(form.getJobId());
        return candidates.save(c);
    }

    /** Attach an uploaded CV (storage key + original filename) to a candidate. */
    public Candidate attachCv(UUID id, String cvKey, String filename, UUID userId, boolean isAdmin) {
        Candidate c = get(id, userId, isAdmin);
        c.setCvPath(cvKey);
        c.setCvFilename(filename);
        return candidates.save(c);
    }

    /** Kanban drag-and-drop: move a candidate to a new column and ordering slot. */
    public Candidate moveToStage(UUID id, CandidateStage stage, double position,
                                 UUID userId, boolean isAdmin) {
        Candidate c = get(id, userId, isAdmin);
        c.setStage(stage);
        c.setPosition(position);
        return candidates.save(c);
    }

    public void delete(UUID id, UUID userId, boolean isAdmin) {
        candidates.delete(get(id, userId, isAdmin));
    }
}
