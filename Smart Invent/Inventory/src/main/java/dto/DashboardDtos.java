package com.stockwise.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record DashboardSummaryResponse(
            int unitsOnHand,
            long lowStockItems,
            long outOfStockItems,
            BigDecimal inventoryValue,
            BigDecimal restockBudget,
            long activeManagers
    ) {
    }

    public record InventoryStatusResponse(
            long healthy,
            long lowStock,
            long outOfStock
    ) {
    }

    public record CategoryStockResponse(
            String category,
            long quantity
    ) {
    }

    public record MonthlyRestockResponse(
            String month,
            long quantity
    ) {
    }

    public record InventoryValueTrendResponse(
            String month,
            BigDecimal value
    ) {
    }

    public record LowStockWatchlistResponse(
            String sku,
            String itemName,
            int quantity,
            int reorderLevel,
            String status
    ) {
    }

    public record RecentActivityResponse(
            Long id,
            String type,
            String activity,
            String loggedBy,
            Instant loggedDate
    ) {
    }
}
