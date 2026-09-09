package com.sendit.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);
    void deleteByEmail(String email);
}
