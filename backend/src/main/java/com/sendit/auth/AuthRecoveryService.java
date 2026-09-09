package com.sendit.auth;

import com.sendit.user.User;
import com.sendit.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthRecoveryService {
    private static final String GENERIC_MESSAGE =
            "입력한 정보와 일치하는 계정이 있다면 이메일을 전송했습니다.";
    private final UserRepository users;
    private final EmailVerificationService emailVerification;
    private final PasswordEncoder encoder;
    private final RefreshTokenRepository refreshTokens;

    public AuthRecoveryService(UserRepository users, EmailVerificationService emailVerification,
            PasswordEncoder encoder, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.emailVerification = emailVerification;
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
    }

    public void sendUsername(String rawEmail) {
        String email = normalize(rawEmail);
        users.findByEmail(email).ifPresent(user -> emailVerification.sendMail(
                email,
                "[SEND IT] 아이디 안내",
                "안녕하세요, SEND IT입니다.\n\n요청하신 계정의 아이디를 안내해 드립니다.\n\n"
                        + "아이디: " + user.getUsername() + "\n\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.\n\nSEND IT"));
    }

    public void sendPasswordResetCode(String rawUsername, String rawEmail) {
        findMatchingUser(rawUsername, rawEmail)
                .ifPresent(user -> emailVerification.sendPasswordReset(user.getEmail()));
    }

    @Transactional(noRollbackFor = AuthException.class)
    public void resetPassword(String rawUsername, String rawEmail, String code, String newPassword) {
        User user = findMatchingUser(rawUsername, rawEmail)
                .orElseThrow(() -> new AuthException(GENERIC_MESSAGE));
        emailVerification.verifyPasswordReset(user.getEmail(), code);
        user.updatePassword(encoder.encode(newPassword));
        refreshTokens.deleteByUserId(user.getId());
    }

    private java.util.Optional<User> findMatchingUser(String rawUsername, String rawEmail) {
        String username = rawUsername.trim();
        String email = normalize(rawEmail);
        return users.findByUsername(username).filter(user -> user.getEmail().equals(email));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
