package com.sendit.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.share.MediaStorageCleaner;
import com.sendit.share.SharedContentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserProfileServiceTest {
    @Test
    void trimsAndUpdatesNickname() {
        UserRepository users = mock(UserRepository.class);
        SharedContentRepository contents = mock(SharedContentRepository.class);
        MediaStorageCleaner cleaner = mock(MediaStorageCleaner.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User("user@example.com", "encoded", "기존 이름");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        UserProfileService service = new UserProfileService(users, contents, cleaner, encoder);

        UserProfileDtos.Response response = service.update(
                "user@example.com",
                new UserProfileDtos.UpdateRequest("  새 이름  "));

        assertThat(response.nickname()).isEqualTo("새 이름");
        assertThat(user.getNickname()).isEqualTo("새 이름");
    }

    @Test
    void deletesAccountAfterPasswordVerification() {
        UserRepository users = mock(UserRepository.class);
        SharedContentRepository contents = mock(SharedContentRepository.class);
        MediaStorageCleaner cleaner = mock(MediaStorageCleaner.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User("user@example.com", "encoded", "여행자");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "encoded")).thenReturn(true);
        when(contents.findAllByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of());
        UserProfileService service = new UserProfileService(users, contents, cleaner, encoder);

        service.delete("user@example.com", new UserProfileDtos.DeleteRequest("password"));

        verify(users).delete(user);
        verify(users).flush();
        verify(cleaner).deleteAll(List.of());
    }

    @Test
    void rejectsAccountDeletionWithWrongPassword() {
        UserRepository users = mock(UserRepository.class);
        SharedContentRepository contents = mock(SharedContentRepository.class);
        MediaStorageCleaner cleaner = mock(MediaStorageCleaner.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User("user@example.com", "encoded", "여행자");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "encoded")).thenReturn(false);
        UserProfileService service = new UserProfileService(users, contents, cleaner, encoder);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.delete(
                        "user@example.com", new UserProfileDtos.DeleteRequest("wrong")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(users, never()).delete(user);
    }
}
