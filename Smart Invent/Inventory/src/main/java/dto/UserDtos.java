package com.stockwise.api.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserAccountResponse(
            Long id,
            String name,
            String email,
            String role,
            boolean active,
            Instant createdAt,
            UserProfileResponse profile
    ) {
    }

    public record UserProfileResponse(
            Long id,
            String displayName,
            String phone,
            String department,
            String jobTitle,
            String location,
            String bio,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UpdateUserProfileRequest(
            @Size(max = 120) String displayName,
            @Size(max = 40) String phone,
            @Size(max = 80) String department,
            @Size(max = 120) String jobTitle,
            @Size(max = 120) String location,
            @Size(max = 500) String bio
    ) {
    }

    public record LoginEventResponse(
            Long id,
            String email,
            boolean successful,
            String failureReason,
            Instant createdAt
    ) {
    }
}
