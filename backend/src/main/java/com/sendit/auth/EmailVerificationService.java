package com.sendit.auth;

import com.sendit.user.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailVerificationService {
    static final String SIGNUP = "SIGNUP";
    static final String PASSWORD_RESET = "PASSWORD_RESET";
    private static final Duration VALIDITY = Duration.ofMinutes(3);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private final EmailVerificationRepository verifications;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final String from;

    public EmailVerificationService(EmailVerificationRepository verifications,
            UserRepository users, PasswordEncoder encoder, JavaMailSender mailSender,
            @Value("${app.mail.from}") String from) {
        this.verifications = verifications;
        this.users = users;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String rawEmail) {
        String email = normalize(rawEmail);
        if (users.existsByEmail(email)) throw new AuthException("이미 가입된 이메일입니다.");
        Instant now = Instant.now();
        verifications.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, SIGNUP)
                .filter(value -> value.getCreatedAt() != null
                        && value.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN)))
                .ifPresent(value -> { throw new AuthException("인증번호는 60초 후 다시 요청할 수 있습니다."); });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        verifications.save(new EmailVerification(email, SIGNUP, encoder.encode(code), now.plus(VALIDITY)));
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(email);
        message.setSubject("[SEND IT] 이메일 인증번호");
        message.setText("SEND IT 회원가입 인증번호는 " + code
                + " 입니다. 인증번호는 3분 동안 유효합니다.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new AuthException("인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    @Transactional(noRollbackFor = AuthException.class)
    public void verify(String rawEmail, String code, boolean consume) {
        String email = normalize(rawEmail);
        EmailVerification verification = verifications.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, SIGNUP)
                .orElseThrow(() -> new AuthException("먼저 이메일 인증번호를 요청해 주세요."));
        Instant now = Instant.now();
        if (verification.isConsumed() || !verification.getExpiresAt().isAfter(now)) {
            throw new AuthException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new AuthException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
        }
        if (!encoder.matches(code, verification.getCodeHash())) {
            verification.fail();
            throw new AuthException("인증번호가 올바르지 않습니다.");
        }
        if (!verification.isVerified()) verification.verify(now);
        if (consume) verification.consume(now);
    }

    void sendPasswordReset(String email) {
        sendCode(email, PASSWORD_RESET, "[SEND IT] 비밀번호 재설정 인증번호",
                "안녕하세요, SEND IT입니다.\n\n비밀번호 재설정을 위한 인증번호입니다.\n\n인증번호: %s\n\n"
                        + "인증번호는 발급 후 3분 동안 사용할 수 있습니다.\n"
                        + "본인이 요청하지 않았다면 비밀번호를 변경할 필요가 없습니다.\n\nSEND IT");
    }

    void verifyPasswordReset(String email, String code) {
        verifyCode(email, PASSWORD_RESET, code, true);
    }

    private void sendCode(String rawEmail, String purpose, String subject, String bodyTemplate) {
        String email = normalize(rawEmail);
        Instant now = Instant.now();
        verifications.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .filter(value -> value.getCreatedAt() != null
                        && value.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN)))
                .ifPresent(value -> { throw new AuthException("인증번호는 60초 후 다시 요청할 수 있습니다."); });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        verifications.save(new EmailVerification(email, purpose, encoder.encode(code), now.plus(VALIDITY)));
        sendMail(email, subject, bodyTemplate.formatted(code));
    }

    @Transactional(noRollbackFor = AuthException.class)
    void verifyCode(String rawEmail, String purpose, String code, boolean consume) {
        String email = normalize(rawEmail);
        EmailVerification verification = verifications
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new AuthException("먼저 이메일 인증번호를 요청해 주세요."));
        Instant now = Instant.now();
        if (verification.isConsumed() || !verification.getExpiresAt().isAfter(now)) {
            throw new AuthException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new AuthException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
        }
        if (!encoder.matches(code, verification.getCodeHash())) {
            verification.fail();
            throw new AuthException("인증번호가 올바르지 않습니다.");
        }
        if (!verification.isVerified()) verification.verify(now);
        if (consume) verification.consume(now);
    }

    void sendMail(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new AuthException("이메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
