package com.stockwise.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailDeliveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeliveryService.class);
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String adminAddress;

    public EmailDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${stockwise.mail.enabled:false}") boolean mailEnabled,
            @Value("${stockwise.mail.from:no-reply@smartinvent.local}") String fromAddress,
            @Value("${stockwise.mail.admin-address:}") String adminAddress
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.adminAddress = adminAddress == null ? "" : adminAddress.trim();
    }

    @PostConstruct
    void logMailConfiguration() {
        LOGGER.info("Mail configuration loaded: enabled={}, fromConfigured={}, adminConfigured={}, senderAvailable={}",
                mailEnabled, hasText(fromAddress), hasText(adminAddress), mailSenderProvider.getIfAvailable() != null);
    }

    public boolean sendVerificationCode(String email, String displayName, String code, long expiresInMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            LOGGER.warn("Email delivery is not configured ({}). Verification code for {} is {} and expires in {} minutes.",
                    mailConfigurationReason(mailSender), email, code, expiresInMinutes);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Your Smart Invent verification code");
        message.setText("""
                Hello %s,

                Your Smart Invent verification code is:

                %s

                This code expires in %d minutes. If you did not request this account, you can ignore this email.
                """.formatted(displayName, code, expiresInMinutes));
        try {
            mailSender.send(message);
            LOGGER.info("Verification email sent to {}", email);
            return true;
        } catch (MailException ex) {
            LOGGER.error("Verification email could not be sent to {}. Check MAIL_ENABLED and SPRING_MAIL_* settings.", email, ex);
            throw new IllegalStateException("Verification email could not be sent. Check SMTP settings and try again.");
        }
    }

    public void sendPasswordUpdateRequestToAdmin(String requestedUserEmail, Instant requestedAt) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null || adminAddress.isBlank()) {
            LOGGER.warn("Password update request email cannot be sent because admin mail is not configured ({}).",
                    mailConfigurationReason(mailSender));
            throw new IllegalStateException("Admin email is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(adminAddress);
        message.setSubject("Password Update Request - Smart Invent");
        message.setText("""
                A password update request was submitted.

                User Email: %s
                Requested At: %s

                Please verify the user identity before updating the password.

                Application: Smart Invent
                """.formatted(requestedUserEmail, REQUEST_TIME_FORMATTER.format(requestedAt)));
        try {
            mailSender.send(message);
            LOGGER.info("Password update request email sent to admin for {}", requestedUserEmail);
        } catch (MailException ex) {
            LOGGER.error("Password update request email could not be sent to admin. Check MAIL_ENABLED and SPRING_MAIL_* settings.", ex);
            throw new IllegalStateException("Failed to send request. Please try again later.");
        }
    }

    private String mailConfigurationReason(JavaMailSender mailSender) {
        if (!mailEnabled) {
            return "MAIL_ENABLED is false or was not loaded";
        }
        if (mailSender == null) {
            return "JavaMailSender bean is unavailable";
        }
        if (adminAddress.isBlank()) {
            return "ADMIN_EMAIL is blank";
        }
        return "unknown";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
