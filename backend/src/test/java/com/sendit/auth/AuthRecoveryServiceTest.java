package com.sendit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.user.User;
import com.sendit.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthRecoveryServiceTest {

    @Test
    void sendsApprovedUsernameGuideToRegisteredEmail() {
        Fixture fixture = new Fixture();
        when(fixture.users.findByEmail("user@example.com")).thenReturn(Optional.of(fixture.user));

        fixture.service.sendUsername(" USER@example.com ");

        verify(fixture.verification).sendMail(
                eq("user@example.com"), eq("[SEND IT] 아이디 안내"), contains("아이디: traveler1"));
    }

    @Test
    void hidesWhetherEmailIsRegistered() {
        Fixture fixture = new Fixture();
        when(fixture.users.findByEmail("none@example.com")).thenReturn(Optional.empty());

        fixture.service.sendUsername("none@example.com");

        verify(fixture.verification, never()).sendMail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void verifiesCodeChangesPasswordAndRevokesSessions() {
        Fixture fixture = new Fixture();
        when(fixture.users.findByUsername("traveler1")).thenReturn(Optional.of(fixture.user));
        when(fixture.encoder.encode("newpass1")).thenReturn("new-encoded");

        fixture.service.resetPassword("traveler1", "user@example.com", "123456", "newpass1");

        verify(fixture.verification).verifyPasswordReset("user@example.com", "123456");
        verify(fixture.tokens).deleteByUserId(fixture.user.getId());
        assertThat(fixture.user.getPassword()).isEqualTo("new-encoded");
    }

    private static final class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final EmailVerificationService verification = mock(EmailVerificationService.class);
        final PasswordEncoder encoder = mock(PasswordEncoder.class);
        final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        final User user = new User("traveler1", "user@example.com", "old-encoded", "여행자");
        final AuthRecoveryService service = new AuthRecoveryService(
                users, verification, encoder, tokens);
    }
}
