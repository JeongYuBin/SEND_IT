package com.sendit.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {
    @Test
    void trimsAndUpdatesNickname() {
        UserRepository users = mock(UserRepository.class);
        User user = new User("user@example.com", "encoded", "기존 이름");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        UserProfileService service = new UserProfileService(users);

        UserProfileDtos.Response response = service.update(
                "user@example.com",
                new UserProfileDtos.UpdateRequest("  새 이름  "));

        assertThat(response.nickname()).isEqualTo("새 이름");
        assertThat(user.getNickname()).isEqualTo("새 이름");
    }

    @Test
    void updatesTravelPreferences() {
        UserRepository users = mock(UserRepository.class);
        User user = new User("user@example.com", "encoded", "여행자");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        UserProfileService service = new UserProfileService(users);

        UserProfileDtos.Response response = service.updatePreferences(
                "user@example.com",
                new UserProfileDtos.PreferencesUpdateRequest("CAR", true));

        assertThat(response.preferredTransport()).isEqualTo("CAR");
        assertThat(response.travelWithPet()).isTrue();
        assertThat(user.getPreferredTransport()).isEqualTo("CAR");
        assertThat(user.isTravelWithPet()).isTrue();
    }
}
