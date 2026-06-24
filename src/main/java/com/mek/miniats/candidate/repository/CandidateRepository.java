package com.mek.miniats.candidate.repository;

import com.mek.miniats.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByOwnerIdOrderByPositionAsc(UUID ownerId);

    List<Candidate> findAllByOrderByPositionAsc();
}
