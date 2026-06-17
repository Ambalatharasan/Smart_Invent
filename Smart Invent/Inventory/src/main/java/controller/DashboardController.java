package com.stockwise.api.controller;

import com.stockwise.api.dto.DashboardDtos.CategoryStockResponse;
import com.stockwise.api.dto.DashboardDtos.DashboardSummaryResponse;
import com.stockwise.api.dto.DashboardDtos.InventoryStatusResponse;
import com.stockwise.api.dto.DashboardDtos.InventoryValueTrendResponse;
import com.stockwise.api.dto.DashboardDtos.LowStockWatchlistResponse;
import com.stockwise.api.dto.DashboardDtos.MonthlyRestockResponse;
import com.stockwise.api.dto.DashboardDtos.RecentActivityResponse;
import com.stockwise.api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/inventory-status")
    public InventoryStatusResponse inventoryStatus() {
        return dashboardService.inventoryStatus();
    }

    @GetMapping("/category-stock")
    public List<CategoryStockResponse> categoryStock() {
        return dashboardService.categoryStock();
    }

    @GetMapping("/monthly-restock")
    public List<MonthlyRestockResponse> monthlyRestock() {
        return dashboardService.monthlyRestock();
    }

    @GetMapping("/inventory-value-trend")
    public List<InventoryValueTrendResponse> inventoryValueTrend() {
        return dashboardService.inventoryValueTrend();
    }

    @GetMapping("/low-stock-watchlist")
    public List<LowStockWatchlistResponse> lowStockWatchlist() {
        return dashboardService.lowStockWatchlist();
    }

    @GetMapping("/recent-activity")
    public List<RecentActivityResponse> recentActivity() {
        return dashboardService.recentActivity();
    }
}
