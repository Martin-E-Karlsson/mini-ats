package com.mek.miniats.candidate.service;

import com.mek.miniats.candidate.CandidateStage;
import com.mek.miniats.candidate.entity.Candidate;
import com.mek.miniats.candidate.repository.CandidateRepository;
import com.mek.miniats.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    CandidateRepository candidates;

    @InjectMocks
    CandidateService service;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID jobA = UUID.randomUUID();
    private final UUID jobB = UUID.randomUUID();

    private Candidate candidate(UUID owner, UUID jobId, String name) {
        Candidate c = new Candidate();
        c.setId(UUID.randomUUID());
        c.setOwnerId(owner);
        c.setJobId(jobId);
        c.setFullName(name);
        c.setStage(CandidateStage.applied);
        return c;
    }

    // ── listing & isolation ───────────────────────────────────────────────

    @Test
    void customer_list_queriesOnlyOwnCandidates() {
        when(candidates.findByOwnerIdOrderByPositionAsc(alice)).thenReturn(List.of());
        service.list(alice, false, null, null);
        verify(candidates).findByOwnerIdOrderByPositionAsc(alice);
        verify(candidates, never()).findAllByOrderByPositionAsc();
    }

    @Test
    void admin_list_queriesAllCandidates() {
        when(candidates.findAllByOrderByPositionAsc()).thenReturn(List.of());
        service.list(alice, true, null, null);
        verify(candidates).findAllByOrderByPositionAsc();
        verify(candidates, never()).findByOwnerIdOrderByPositionAsc(any());
    }

    // ── filtering (the board's filter bar) ────────────────────────────────

    @Test
    void list_filtersByJob() {
        when(candidates.findByOwnerIdOrderByPositionAsc(alice)).thenReturn(List.of(
                candidate(alice, jobA, "Ada Lovelace"),
                candidate(alice, jobB, "Alan Turing")));

        List<Candidate> result = service.list(alice, false, jobA, null);

        assertThat(result).extracting(Candidate::getFullName).containsExactly("Ada Lovelace");
    }

    @Test
    void list_filtersByName_caseInsensitiveSubstring() {
        when(candidates.findByOwnerIdOrderByPositionAsc(alice)).thenReturn(List.of(
                candidate(alice, jobA, "Ada Lovelace"),
                candidate(alice, jobA, "Alan Turing")));

        List<Candidate> result = service.list(alice, false, null, "tur");

        assertThat(result).extracting(Candidate::getFullName).containsExactly("Alan Turing");
    }

    @Test
    void list_blankNameFilter_returnsAll() {
        when(candidates.findByOwnerIdOrderByPositionAsc(alice)).thenReturn(List.of(
                candidate(alice, jobA, "Ada Lovelace"),
                candidate(alice, jobA, "Alan Turing")));

        List<Candidate> result = service.list(alice, false, null, "   ");

        assertThat(result).hasSize(2);
    }

    // ── get ───────────────────────────────────────────────────────────────

    @Test
    void customer_get_othersCandidate_throwsNotFound() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.get(c.getId(), alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void admin_get_othersCandidate_succeeds() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        assertThat(service.get(c.getId(), alice, true)).isSameAs(c);
    }

    // ── create with admin override ────────────────────────────────────────

    @Test
    void customer_create_forcesOwnerToSelf() {
        Candidate input = candidate(null, jobA, "New Person");
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        service.create(input, alice, false, bob);

        ArgumentCaptor<Candidate> saved = ArgumentCaptor.forClass(Candidate.class);
        verify(candidates).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(alice);
    }

    @Test
    void admin_create_withOverride_setsOwnerToCustomer() {
        Candidate input = candidate(null, jobA, "New Person");
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        service.create(input, alice, true, bob);

        ArgumentCaptor<Candidate> saved = ArgumentCaptor.forClass(Candidate.class);
        verify(candidates).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(bob);
    }

    // ── kanban drag: move to stage ────────────────────────────────────────

    @Test
    void moveToStage_ownCandidate_updatesStageAndPosition() {
        Candidate c = candidate(alice, jobA, "Ada Lovelace");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        Candidate result = service.moveToStage(c.getId(), CandidateStage.interview, 2.0, alice, false);

        assertThat(result.getStage()).isEqualTo(CandidateStage.interview);
        assertThat(result.getPosition()).isEqualTo(2.0);
    }

    @Test
    void moveToStage_othersCandidate_throwsAndDoesNotSave() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));

        assertThatThrownBy(() ->
                service.moveToStage(c.getId(), CandidateStage.hired, 0.0, alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(candidates, never()).save(any());
    }

    // ── delete isolation ──────────────────────────────────────────────────

    @Test
    void customer_delete_othersCandidate_throwsAndDoesNotDelete() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.delete(c.getId(), alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(candidates, never()).delete(any());
    }

    // ── attach CV ─────────────────────────────────────────────────────────

    @Test
    void attachCv_ownCandidate_setsPathAndFilename() {
        Candidate c = candidate(alice, jobA, "Ada");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        Candidate result = service.attachCv(c.getId(), "key123", "ada.pdf", alice, false);

        assertThat(result.getCvPath()).isEqualTo("key123");
        assertThat(result.getCvFilename()).isEqualTo("ada.pdf");
    }

    @Test
    void attachCv_othersCandidate_throwsAndDoesNotSave() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.attachCv(c.getId(), "key", "cv.pdf", alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(candidates, never()).save(any());
    }

    // ── advance / retreat stage (board buttons) ───────────────────────────

    @Test
    void advanceStage_movesToNextStage() {
        Candidate c = candidate(alice, jobA, "Ada");
        c.setStage(CandidateStage.applied);
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        Candidate result = service.advanceStage(c.getId(), alice, false);

        assertThat(result.getStage()).isEqualTo(CandidateStage.screening);
    }

    @Test
    void advanceStage_atLastStage_doesNotChangeOrSave() {
        Candidate c = candidate(alice, jobA, "Ada");
        c.setStage(CandidateStage.rejected); // last in the enum
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));

        Candidate result = service.advanceStage(c.getId(), alice, false);

        assertThat(result.getStage()).isEqualTo(CandidateStage.rejected);
        verify(candidates, never()).save(any());
    }

    @Test
    void retreatStage_movesToPreviousStage() {
        Candidate c = candidate(alice, jobA, "Ada");
        c.setStage(CandidateStage.interview);
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        when(candidates.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        Candidate result = service.retreatStage(c.getId(), alice, false);

        assertThat(result.getStage()).isEqualTo(CandidateStage.screening);
    }

    @Test
    void retreatStage_atFirstStage_doesNotChangeOrSave() {
        Candidate c = candidate(alice, jobA, "Ada");
        c.setStage(CandidateStage.applied); // first in the enum
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));

        Candidate result = service.retreatStage(c.getId(), alice, false);

        assertThat(result.getStage()).isEqualTo(CandidateStage.applied);
        verify(candidates, never()).save(any());
    }

    @Test
    void advanceStage_othersCandidate_throws() {
        Candidate c = candidate(bob, jobA, "Bob's Candidate");
        when(candidates.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.advanceStage(c.getId(), alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
