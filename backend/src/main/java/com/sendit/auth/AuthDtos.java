package com.sendit.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignUpRequest(
            @NotBlank @Pattern(regexp = "(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,15}",
                    message = "아이디는 영문 소문자와 숫자를 포함한 8~15자여야 합니다.") String username,
            @Email @NotBlank String email,
            @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,15}",
                    message = "비밀번호는 영문과 숫자를 포함한 8~15자여야 합니다.") String password,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해 주세요.") String emailOtp,
            @NotBlank @Size(max = 50) String nickname
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(
            String tokenType,
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            UserSummary user
    ) {
    }

    public record EmailRequest(@Email @NotBlank String email) {}

    public record EmailOtpRequest(
            @Email @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code
    ) {}

    public record UsernameAvailability(boolean available) {}

    public record FindUsernameRequest(@Email @NotBlank String email) {}

    public record PasswordResetCodeRequest(
            @NotBlank String username,
            @Email @NotBlank String email
    ) {}

    public record PasswordResetRequest(
            @NotBlank String username,
            @Email @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code,
            @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,15}",
                    message = "비밀번호는 영문과 숫자를 포함한 8~15자여야 합니다.") String newPassword
    ) {}

    public record UserSummary(Long id, String username, String email, String nickname) {
    }
}
