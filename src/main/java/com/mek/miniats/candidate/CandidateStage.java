package com.mek.miniats.candidate;

/**
 * Kanban columns. Lowercase constants so @Enumerated(EnumType.STRING) matches
 * the candidates.stage CHECK constraint in the database.
 */
public enum CandidateStage {
    applied,
    screening,
    interview,
    offer,
    hired,
    rejected
}
