package com.mek.miniats.job.repository;

import com.mek.miniats.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Job> findAllByOrderByCreatedAtDesc();
}
