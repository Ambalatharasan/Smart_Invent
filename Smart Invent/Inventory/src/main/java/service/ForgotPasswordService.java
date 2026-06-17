package com.stockwise.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ForgotPasswordService {
    private static final String SUCCESS_MESSAGE = "Your password update request has been submitted. Admin will contact you if the account is valid.";

    private final EmailDeliveryService emailDeliveryService;
    private final long cooldownSeconds;
    private final Map<String, Instant> lastRequestByEmail = new ConcurrentHashMap<>();

    public ForgotPasswordService(
            EmailDeliveryService emailDeliveryService,
            @Value("${stockwise.forgot-password.cooldown-seconds:60}") long cooldownSeconds
    ) {
        this.emailDeliveryService = emailDeliveryService;
        this.cooldownSeconds = Math.max(1, cooldownSeconds);
    }

    public String requestPasswordUpdate(String email) {
        String normalizedEmail = normalizeEmail(email);
        enforceCooldown(normalizedEmail);
        emailDeliveryService.sendPasswordUpdateRequestToAdmin(normalizedEmail, Instant.now());
        lastRequestByEmail.put(normalizedEmail, Instant.now());
        return SUCCESS_MESSAGE;
    }

    private void enforceCooldown(String email) {
        Instant lastRequest = lastRequestByEmail.get(email);
        if (lastRequest == null) {
            return;
        }

        long secondsElapsed = Instant.now().getEpochSecond() - lastRequest.getEpochSecond();
        long secondsRemaining = cooldownSeconds - secondsElapsed;
        if (secondsRemaining > 0) {
            throw new VerificationCooldownException(
                    "Please wait %d seconds before requesting password support again.".formatted(secondsRemaining),
                    secondsRemaining
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
