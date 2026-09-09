package com.sendit.auth;

import com.sendit.auth.AuthDtos.LoginRequest;
import com.sendit.auth.AuthDtos.LogoutRequest;
import com.sendit.auth.AuthDtos.RefreshRequest;
import com.sendit.auth.AuthDtos.SignUpRequest;
import com.sendit.auth.AuthDtos.TokenResponse;
import com.sendit.auth.AuthDtos.EmailRequest;
import com.sendit.auth.AuthDtos.EmailOtpRequest;
import com.sendit.auth.AuthDtos.UsernameAvailability;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/usernames/{username}/availability")
    UsernameAvailability usernameAvailability(@PathVariable String username) {
        return new UsernameAvailability(authService.isUsernameAvailable(username));
    }

    @PostMapping("/email-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendEmailOtp(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.send(request.email());
    }

    @PostMapping("/email-otp/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmailOtp(@Valid @RequestBody EmailOtpRequest request) {
        emailVerificationService.verify(request.email(), request.code(), false);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }
}
