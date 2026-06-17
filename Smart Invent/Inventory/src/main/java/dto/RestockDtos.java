package com.stockwise.api.dto;

import com.stockwise.api.entity.RestockOrderStatus;
import com.stockwise.api.entity.StockStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RestockDtos {
    private RestockDtos() {
    }

    public record CreateRestockOrderRequest(
            @NotNull Long itemId,
            @Min(1) Integer requestedQuantity
    ) {
    }

    public record UpdateRestockStatusRequest(
            @NotNull RestockOrderStatus status
    ) {
    }

    public record RestockCandidateResponse(
            Long itemId,
            String sku,
            String name,
            String category,
            String supplier,
            int quantity,
            int reorderPoint,
            StockStatus status,
            int suggestedOrderQuantity,
            BigDecimal estimatedCost,
            int leadTimeDays,
            LocalDate lastRestockDate,
            Integer lastRestockQuantity
    ) {
    }

    public record RestockOrderResponse(
            Long id,
            Long itemId,
            String sku,
            String itemName,
            int requestedQuantity,
            BigDecimal estimatedCost,
            RestockOrderStatus status,
            String requestedBy,
            Instant createdAt,
            Instant updatedAt,
            Instant receivedAt
    ) {
    }

    public record RestockSummaryResponse(
            long candidateCount,
            long outOfStockCount,
            BigDecimal estimatedBudget,
            List<RestockCandidateResponse> candidates
    ) {
    }

    public record RestockCsvImportResponse(
            int createdCount,
            int updatedCount,
            int skippedCount,
            List<String> errors
    ) {
    }
}
