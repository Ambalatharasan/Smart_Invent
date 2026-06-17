package com.stockwise.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class ManagerDtos {
    private ManagerDtos() {
    }

    public record ManagerRequest(
            @NotBlank String fullName,
            @NotBlank String title,
            @NotBlank String department,
            @NotBlank @Email String email,
            @NotBlank String phone,
            @NotBlank String shiftName,
            @NotBlank String status,
            String responsibilities
    ) {
    }

    public record ManagerResponse(
            Long id,
            String fullName,
            String title,
            String department,
            String email,
            String phone,
            String shiftName,
            String status,
            String responsibilities,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
