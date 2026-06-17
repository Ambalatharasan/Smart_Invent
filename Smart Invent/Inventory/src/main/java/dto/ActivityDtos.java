
package com.stockwise.api.dto;

import com.stockwise.api.entity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public final class ActivityDtos {
	private ActivityDtos() {
	}

	public record CreateActivityRequest(@NotNull ActivityType type, @NotBlank String message, String itemSku,
			Integer quantity, LocalDate lastRestockDate) {
	}

	public record ActivityResponse(Long id, ActivityType type, String message, String itemSku, Integer quantity,
			LocalDate lastRestockDate, String actorEmail, Instant createdAt) {
	}
}
