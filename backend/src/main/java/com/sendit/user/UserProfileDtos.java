package com.sendit.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserProfileDtos {
    private UserProfileDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 50) String nickname
    ) {
    }

    public record PreferencesUpdateRequest(
            @NotNull
            @Pattern(regexp = "WALKING|PUBLIC_TRANSIT|CAR")
            String preferredTransport,
            boolean travelWithPet
    ) {
    }

    public record Response(
            Long id,
            String email,
            String nickname,
            String preferredTransport,
            boolean travelWithPet
    ) {
    }
}
