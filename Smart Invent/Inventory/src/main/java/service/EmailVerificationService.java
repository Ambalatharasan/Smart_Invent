package com.stockwise.api.service;

import com.stockwise.api.entity.EmailVerificationCode;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.repository.EmailVerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDeliveryService emailDeliveryService;
    private final long codeExpiresInMinutes;
    private final long resendCooldownSeconds;

    public EmailVerificationService(
            EmailVerificationCodeRepository emailVerificationCodeRepository,
            PasswordEncoder passwordEncoder,
            EmailDeliveryService emailDeliveryService,
            @Value("${stockwise.verification.code-expires-minutes:10}") long codeExpiresInMinutes,
            @Value("${stockwise.verification.resend-cooldown-seconds:30}") long resendCooldownSeconds
    ) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailDeliveryService = emailDeliveryService;
        this.codeExpiresInMinutes = codeExpiresInMinutes;
        this.resendCooldownSeconds = Math.max(0, resendCooldownSeconds);
    }

    public record VerificationDelivery(Instant expiresAt, Instant nextResendAt, boolean emailSent) {
    }

    @Transactional
    public VerificationDelivery sendRegistrationCode(UserAccount user) {
        return issueRegistrationCode(user, false);
    }

    @Transactional
    public VerificationDelivery resendRegistrationCode(UserAccount user) {
        return issueRegistrationCode(user, true);
    }

    private VerificationDelivery issueRegistrationCode(UserAccount user, boolean enforceCooldown) {
        Instant now = Instant.now();
        if (enforceCooldown) {
            emailVerificationCodeRepository
                    .findFirstByUserAccountAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                            user,
                            EmailVerificationCode.PURPOSE_REGISTRATION
                    )
                    .ifPresent(latestCode -> enforceResendCooldown(latestCode, now));
        }

        emailVerificationCodeRepository
                .findAllByUserAccountAndPurposeAndConsumedAtIsNull(user, EmailVerificationCode.PURPOSE_REGISTRATION)
                .forEach(EmailVerificationCode::markConsumed);

        String code = generateCode();
        Instant expiresAt = now.plus(Duration.ofMinutes(codeExpiresInMinutes));
        Instant nextResendAt = now.plus(Duration.ofSeconds(resendCooldownSeconds));
        EmailVerificationCode verificationCode = new EmailVerificationCode(
                user,
                user.getEmail(),
                passwordEncoder.encode(code),
                EmailVerificationCode.PURPOSE_REGISTRATION,
                expiresAt
        );
        emailVerificationCodeRepository.save(verificationCode);
        boolean emailSent = emailDeliveryService.sendVerificationCode(user.getEmail(), user.getName(), code, codeExpiresInMinutes);
        return new VerificationDelivery(expiresAt, nextResendAt, emailSent);
    }

    @Transactional
    public UserAccount verifyRegistrationCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        normalizedEmail,
                        EmailVerificationCode.PURPOSE_REGISTRATION
                )
                .orElseThrow(() -> new IllegalArgumentException("Verification code was not found or has already been used"));

        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification code has expired. Request a new code.");
        }
        if (verificationCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many verification attempts. Request a new code.");
        }

        if (!passwordEncoder.matches(code.trim(), verificationCode.getCodeHash())) {
            verificationCode.incrementAttempts();
            throw new IllegalArgumentException("Verification code is incorrect");
        }

        verificationCode.markConsumed();
        UserAccount user = verificationCode.getUserAccount();
        user.markEmailVerified();
        return user;
    }

    private String generateCode() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    private void enforceResendCooldown(EmailVerificationCode latestCode, Instant now) {
        if (resendCooldownSeconds <= 0 || latestCode.getCreatedAt() == null) {
            return;
        }
        Instant nextAllowedAt = latestCode.getCreatedAt().plus(Duration.ofSeconds(resendCooldownSeconds));
        if (nextAllowedAt.isAfter(now)) {
            long secondsRemaining = Math.max(1, Duration.between(now, nextAllowedAt).toSeconds());
            throw new VerificationCooldownException(secondsRemaining);
        }
    }
}
