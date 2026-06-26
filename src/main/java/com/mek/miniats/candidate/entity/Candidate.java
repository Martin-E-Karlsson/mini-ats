package com.mek.miniats.candidate.entity;

import com.mek.miniats.candidate.CandidateStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A person in a customer's pipeline. job_id is optional; stage drives the kanban column. */
@Entity
@Table(name = "candidates")
@Getter
@Setter
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "job_id", columnDefinition = "uuid")
    private UUID jobId;

    @NotBlank
    @Column(name = "full_name", nullable = false, columnDefinition = "text")
    private String fullName;

    @Column(columnDefinition = "text")
    private String email;

    @Column(name = "linkedin_url", columnDefinition = "text")
    private String linkedinUrl;

    @Column(columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private CandidateStage stage = CandidateStage.applied;

    /** Ordering within a kanban column (for drag-and-drop). */
    @Column(nullable = false)
    private double position = 0;

    /** Storage object key for the uploaded CV (null if none). */
    @Column(name = "cv_path", columnDefinition = "text")
    private String cvPath;

    /** Original filename of the uploaded CV, for display/download. */
    @Column(name = "cv_filename", columnDefinition = "text")
    private String cvFilename;

    @Column(name = "created_at", columnDefinition = "timestamptz",
            insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
