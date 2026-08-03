package com.sendit.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserProfileDtos {
    private UserProfileDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 50) String nickname
    ) {
    }

    public record Response(Long id, String email, String nickname) {
    }
}
