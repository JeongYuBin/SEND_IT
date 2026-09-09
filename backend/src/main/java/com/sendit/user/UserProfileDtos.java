package com.sendit.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public final class UserProfileDtos {
    private UserProfileDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 50) String nickname
    ) {
    }

    public record Response(Long id, String username, String email, String nickname) {
    }

    public record DeleteRequest(
            @NotBlank @Size(max = 100) String password
    ) {
    }

    public record PasswordUpdateRequest(
            @NotBlank @Size(max = 100) String currentPassword,
            @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,15}",
                    message = "새 비밀번호는 영문과 숫자를 포함한 8~15자여야 합니다.") String newPassword
    ) {
    }
}
