package com.sendit.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.auth.RefreshTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserPasswordServiceTest {
    @Test
    void changesPasswordAndRevokesRefreshTokens() {
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User("user@example.com", "old-encoded", "여행자");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "old-encoded")).thenReturn(true);
        when(encoder.matches("new-password", "old-encoded")).thenReturn(false);
        when(encoder.encode("new-password")).thenReturn("new-encoded");
        UserPasswordService service = new UserPasswordService(users, tokens, encoder);

        service.update("user@example.com", new UserProfileDtos.PasswordUpdateRequest(
                "old-password", "new-password"));

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(tokens).deleteByUserId(user.getId());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User("user@example.com", "old-encoded", "여행자");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "old-encoded")).thenReturn(false);
        UserPasswordService service = new UserPasswordService(users, tokens, encoder);

        assertThatThrownBy(() -> service.update("user@example.com",
                new UserProfileDtos.PasswordUpdateRequest("wrong", "new-password")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tokens, never()).deleteByUserId(user.getId());
    }
}
