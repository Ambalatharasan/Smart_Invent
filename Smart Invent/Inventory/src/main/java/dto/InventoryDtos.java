package com.stockwise.api.dto;

import com.stockwise.api.entity.StockStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    public record InventoryRequest(
            String sku,
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String supplier,
            @Min(0) int quantity,
            @Min(0) int reorderPoint,
            @NotNull @DecimalMin("0.01") BigDecimal unitCost,
            @NotNull @DecimalMin("0.01") BigDecimal retailPrice,
            @Min(0) int leadTimeDays,
            @NotBlank String location,
            String notes,
            LocalDate lastRestockDate,
            @Min(0) Integer lastRestockQuantity
    ) {
    }

    public record StockAdjustmentRequest(
            int quantityChange,
            @NotBlank String reason
    ) {
    }

    public record InventoryResponse(
            Long id,
            String sku,
            String name,
            String category,
            String supplier,
            int quantity,
            int reorderPoint,
            BigDecimal unitCost,
            BigDecimal retailPrice,
            int leadTimeDays,
            String location,
            String notes,
            StockStatus status,
            int suggestedOrderQuantity,
            BigDecimal inventoryValue,
            BigDecimal retailValue,
            LocalDate lastRestockDate,
            Integer lastRestockQuantity,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record InventoryCsvImportResponse(
            int createdCount,
            int updatedCount,
            int skippedCount,
            List<String> errors
    ) {
    }
}
