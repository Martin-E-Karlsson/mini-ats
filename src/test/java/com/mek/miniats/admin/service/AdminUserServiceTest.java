package com.mek.miniats.admin.service;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.auth.SupabaseAuthClient;
import com.mek.miniats.user.UserRole;
import com.mek.miniats.user.entity.Profile;
import com.mek.miniats.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    SupabaseAuthClient authClient;

    @Mock
    ProfileRepository profiles;

    @InjectMocks
    AdminUserService service;

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void createAccount_delegatesToGoTrueWithRoleName() {
        service.createAccount("new@b.com", "secret123", "New User", UserRole.customer);
        verify(authClient).adminCreateUser("new@b.com", "secret123", "New User", "customer");
    }

    @Test
    void createAccount_adminRole_passesAdminToGoTrue() {
        service.createAccount("boss@b.com", "secret123", "The Boss", UserRole.admin);
        verify(authClient).adminCreateUser("boss@b.com", "secret123", "The Boss", "admin");
    }

    @Test
    void createAccount_duplicateEmail_throwsFriendlyException() {
        byte[] body = "{\"msg\":\"already been registered\"}".getBytes(StandardCharsets.UTF_8);
        doThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", body, StandardCharsets.UTF_8))
                .when(authClient).adminCreateUser("dupe@b.com", "secret123", "Dupe", "customer");

        assertThatThrownBy(() -> service.createAccount("dupe@b.com", "secret123", "Dupe", UserRole.customer))
                .isInstanceOf(AccountCreationException.class)
                .hasMessageContaining("already");
    }

    // ── update (admin: role can change) ───────────────────────────────────

    @Test
    void updateAccount_updatesAuthAndProfileIncludingRole() {
        UUID id = UUID.randomUUID();
        Profile existing = new Profile();
        existing.setId(id);
        existing.setRole(UserRole.customer);
        when(profiles.findById(id)).thenReturn(Optional.of(existing));

        service.updateAccount(id, "new@b.com", "New Name", UserRole.admin, "password8");

        verify(authClient).adminUpdateUser(id.toString(), "new@b.com", "password8", "New Name");
        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@b.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("New Name");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.admin);
    }

    // ── self update (role preserved) ──────────────────────────────────────

    @Test
    void updateOwnProfile_keepsExistingRole() {
        UUID id = UUID.randomUUID();
        Profile existing = new Profile();
        existing.setId(id);
        existing.setRole(UserRole.admin);
        when(profiles.findById(id)).thenReturn(Optional.of(existing));

        service.updateOwnProfile(id, "me@b.com", "Me", null);

        verify(authClient).adminUpdateUser(id.toString(), "me@b.com", null, "Me");
        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.admin); // unchanged
        assertThat(saved.getValue().getEmail()).isEqualTo("me@b.com");
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void deleteAccount_deletesGoTrueUser() {
        UUID id = UUID.randomUUID();
        service.deleteAccount(id);
        verify(authClient).adminDeleteUser(id.toString());
    }
}
