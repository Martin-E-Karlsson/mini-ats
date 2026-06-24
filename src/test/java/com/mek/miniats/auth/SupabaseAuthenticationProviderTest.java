package com.mek.miniats.auth;

import com.mek.miniats.user.UserRole;
import com.mek.miniats.user.entity.Profile;
import com.mek.miniats.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseAuthenticationProviderTest {

    @Mock
    SupabaseAuthClient authClient;

    @Mock
    ProfileRepository profiles;

    private SupabaseAuthenticationProvider provider() {
        return new SupabaseAuthenticationProvider(authClient, profiles);
    }

    private static Authentication attempt(String email, String password) {
        return new UsernamePasswordAuthenticationToken(email, password);
    }

    private static TokenResponse tokenFor(UUID userId, String email) {
        return new TokenResponse("access-token", "refresh-token",
                new TokenResponse.User(userId.toString(), email));
    }

    private static Profile profile(UUID id, String email, UserRole role) {
        Profile p = new Profile();
        p.setId(id);
        p.setEmail(email);
        p.setFullName("Test User");
        p.setRole(role);
        return p;
    }

    @Test
    void validCredentials_customer_grantsCustomerRole() {
        UUID id = UUID.randomUUID();
        when(authClient.signInWithPassword("a@b.com", "pw")).thenReturn(tokenFor(id, "a@b.com"));
        when(profiles.findById(id)).thenReturn(Optional.of(profile(id, "a@b.com", UserRole.customer)));

        Authentication result = provider().authenticate(attempt("a@b.com", "pw"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
        assertThat(result.getPrincipal()).isInstanceOf(SupabasePrincipal.class);
        SupabasePrincipal principal = (SupabasePrincipal) result.getPrincipal();
        assertThat(principal.id()).isEqualTo(id);
        assertThat(principal.email()).isEqualTo("a@b.com");
        assertThat(principal.accessToken()).isEqualTo("access-token");
    }

    @Test
    void validCredentials_admin_grantsAdminRole() {
        UUID id = UUID.randomUUID();
        when(authClient.signInWithPassword("admin@b.com", "pw")).thenReturn(tokenFor(id, "admin@b.com"));
        when(profiles.findById(id)).thenReturn(Optional.of(profile(id, "admin@b.com", UserRole.admin)));

        Authentication result = provider().authenticate(attempt("admin@b.com", "pw"));

        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void wrongPassword_throwsBadCredentials() {
        when(authClient.signInWithPassword("a@b.com", "wrong"))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> provider().authenticate(attempt("a@b.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticatedButNoProfileRow_throwsBadCredentials() {
        UUID id = UUID.randomUUID();
        when(authClient.signInWithPassword("ghost@b.com", "pw")).thenReturn(tokenFor(id, "ghost@b.com"));
        when(profiles.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider().authenticate(attempt("ghost@b.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void supports_usernamePasswordAuthenticationToken() {
        assertThat(provider().supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }
}
