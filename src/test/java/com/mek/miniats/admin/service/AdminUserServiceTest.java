package com.mek.miniats.admin.service;

import com.mek.miniats.admin.AccountCreationException;
import com.mek.miniats.auth.SupabaseAuthClient;
import com.mek.miniats.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    SupabaseAuthClient authClient;

    @InjectMocks
    AdminUserService service;

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
    void createAccount_duplicateEmail_throwsFriendlyAccountCreationException() {
        byte[] body = "{\"msg\":\"A user with this email address has already been registered\"}"
                .getBytes(StandardCharsets.UTF_8);
        doThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", body, StandardCharsets.UTF_8))
                .when(authClient).adminCreateUser("dupe@b.com", "secret123", "Dupe", "customer");

        assertThatThrownBy(() ->
                service.createAccount("dupe@b.com", "secret123", "Dupe", UserRole.customer))
                .isInstanceOf(AccountCreationException.class)
                .hasMessageContaining("already");
    }

    @Test
    void createAccount_otherGoTrueError_throwsAccountCreationException() {
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(authClient).adminCreateUser("bad@b.com", "x", "Bad", "customer");

        assertThatThrownBy(() ->
                service.createAccount("bad@b.com", "x", "Bad", UserRole.customer))
                .isInstanceOf(AccountCreationException.class);
    }
}
