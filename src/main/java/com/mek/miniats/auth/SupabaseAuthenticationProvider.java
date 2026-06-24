package com.mek.miniats.auth;

import com.mek.miniats.user.entity.Profile;
import com.mek.miniats.user.repository.ProfileRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Replaces the old DaoAuthenticationProvider. Instead of checking a local
 * password hash, it asks Supabase Auth to verify the credentials, then loads
 * the matching profile to determine the user's role.
 *
 * Because this is the only AuthenticationProvider bean, Spring's form-login
 * filter routes username/password attempts straight to it.
 */
@Component
public class SupabaseAuthenticationProvider implements AuthenticationProvider {

    private final SupabaseAuthClient authClient;
    private final ProfileRepository profiles;

    public SupabaseAuthenticationProvider(SupabaseAuthClient authClient, ProfileRepository profiles) {
        this.authClient = authClient;
        this.profiles = profiles;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        // 1. Let Supabase verify the password.
        TokenResponse token;
        try {
            token = authClient.signInWithPassword(email, password);
        } catch (RestClientResponseException e) {
            // GoTrue returns 400 for bad credentials.
            throw new BadCredentialsException("Invalid email or password");
        }

        // 2. Load the profile (created by the DB trigger) to get the role.
        Profile profile = profiles.findById(UUID.fromString(token.user().id()))
                .orElseThrow(() -> new BadCredentialsException("No profile found for this user"));

        // 3. Build the authenticated token with a Spring role authority.
        var authority = new SimpleGrantedAuthority("ROLE_" + profile.getRole().name().toUpperCase());
        var principal = new SupabasePrincipal(
                profile.getId(), profile.getEmail(), profile.getFullName(), token.accessToken());

        return new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
