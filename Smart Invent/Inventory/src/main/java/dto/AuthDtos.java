package com.stockwise.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 72, message = "Password must not exceed 72 characters.") String password
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {
    }

    public record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String code
    ) {
    }

    public record ResendVerificationRequest(
            @NotBlank @Email String email
    ) {
    }

    public record RegistrationResponse(
            String email,
            String message,
            String expiresAt,
            String nextResendAt
    ) {
    }

    public record MessageResponse(
            String message
    ) {
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresInMinutes,
            UserProfile user
    ) {
    }

    public record UserProfile(
            Long id,
            String name,
            String email,
            String role,
            boolean emailVerified
    ) {
    }
}
