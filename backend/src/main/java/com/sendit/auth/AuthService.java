package com.sendit.auth;

import com.sendit.auth.AuthDtos.LoginRequest;
import com.sendit.auth.AuthDtos.SignUpRequest;
import com.sendit.auth.AuthDtos.TokenResponse;
import com.sendit.auth.AuthDtos.UserSummary;
import com.sendit.user.User;
import com.sendit.user.UserRepository;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenResponse signUp(SignUpRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("이미 가입된 이메일입니다.");
        }

        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.nickname().trim()
        ));
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("이메일 또는 비밀번호가 올바르지 않습니다."));
        return issueTokens(user);
    }

    public TokenResponse refresh(String rawToken) {
        try {
            if (!"refresh".equals(jwtTokenProvider.parse(rawToken).get("type", String.class))) {
                throw new AuthException("유효하지 않은 리프레시 토큰입니다.");
            }
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        }

        RefreshToken savedToken = refreshTokenRepository
                .findByTokenHash(TokenHash.sha256(rawToken))
                .orElseThrow(() -> new AuthException("등록되지 않은 리프레시 토큰입니다."));

        Instant now = Instant.now();
        if (!savedToken.isUsable(now)) {
            throw new AuthException("만료되었거나 폐기된 리프레시 토큰입니다.");
        }

        savedToken.revoke(now);
        return issueTokens(savedToken.getUser());
    }

    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHash.sha256(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        Instant expiration = jwtTokenProvider.getExpiration(accessToken);

        refreshTokenRepository.save(new RefreshToken(
                user,
                TokenHash.sha256(refreshToken),
                jwtTokenProvider.getExpiration(refreshToken)
        ));

        return new TokenResponse(
                "Bearer",
                accessToken,
                refreshToken,
                Math.max(0, Duration.between(Instant.now(), expiration).toSeconds()),
                new UserSummary(user.getId(), user.getEmail(), user.getNickname())
        );
    }
}

