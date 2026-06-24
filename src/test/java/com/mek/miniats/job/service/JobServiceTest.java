package com.mek.miniats.job.service;

import com.mek.miniats.common.ResourceNotFoundException;
import com.mek.miniats.job.entity.Job;
import com.mek.miniats.job.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    JobRepository jobs;

    @InjectMocks
    JobService service;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private Job jobOwnedBy(UUID owner) {
        Job j = new Job();
        j.setId(UUID.randomUUID());
        j.setOwnerId(owner);
        j.setTitle("Backend Engineer");
        return j;
    }

    @Test
    void customer_list_queriesOnlyOwnJobs() {
        service.list(alice, false);
        verify(jobs).findByOwnerIdOrderByCreatedAtDesc(alice);
        verify(jobs, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void admin_list_queriesAllJobs() {
        service.list(alice, true);
        verify(jobs).findAllByOrderByCreatedAtDesc();
        verify(jobs, never()).findByOwnerIdOrderByCreatedAtDesc(any());
    }

    @Test
    void customer_get_ownJob_succeeds() {
        Job job = jobOwnedBy(alice);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        assertThat(service.get(job.getId(), alice, false)).isSameAs(job);
    }

    @Test
    void customer_get_othersJob_throwsNotFound() {
        Job job = jobOwnedBy(bob);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> service.get(job.getId(), alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void admin_get_othersJob_succeeds() {
        Job job = jobOwnedBy(bob);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        assertThat(service.get(job.getId(), alice, true)).isSameAs(job);
    }

    @Test
    void get_missingJob_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(jobs.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id, alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customer_create_forcesOwnerToSelf_ignoringOverride() {
        Job input = new Job();
        input.setTitle("Designer");
        when(jobs.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        // bob is passed as override but must be ignored for a non-admin
        service.create(input, alice, false, bob);

        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(alice);
    }

    @Test
    void admin_create_withOverride_setsOwnerToCustomer() {
        Job input = new Job();
        input.setTitle("Designer");
        when(jobs.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        service.create(input, alice, true, bob);

        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(bob);
    }

    @Test
    void admin_create_withoutOverride_setsOwnerToSelf() {
        Job input = new Job();
        input.setTitle("Designer");
        when(jobs.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        service.create(input, alice, true, null);

        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(alice);
    }

    @Test
    void customer_update_othersJob_throwsAndDoesNotSave() {
        Job job = jobOwnedBy(bob);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        Job form = new Job();
        form.setTitle("Hacked");

        assertThatThrownBy(() -> service.update(job.getId(), form, alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void customer_update_ownJob_appliesChanges() {
        Job job = jobOwnedBy(alice);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobs.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        Job form = new Job();
        form.setTitle("Senior Backend Engineer");
        form.setLocation("Stockholm");
        form.setOpen(false);

        Job result = service.update(job.getId(), form, alice, false);

        assertThat(result.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(result.getLocation()).isEqualTo("Stockholm");
        assertThat(result.isOpen()).isFalse();
    }

    @Test
    void customer_delete_ownJob_deletes() {
        Job job = jobOwnedBy(alice);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        service.delete(job.getId(), alice, false);
        verify(jobs).delete(job);
    }

    @Test
    void customer_delete_othersJob_throwsAndDoesNotDelete() {
        Job job = jobOwnedBy(bob);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> service.delete(job.getId(), alice, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobs, never()).delete(any());
    }
}
