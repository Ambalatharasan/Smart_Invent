package com.stockwise.api.service;

import com.stockwise.api.dto.DashboardDtos.CategoryStockResponse;
import com.stockwise.api.dto.DashboardDtos.DashboardSummaryResponse;
import com.stockwise.api.dto.DashboardDtos.InventoryStatusResponse;
import com.stockwise.api.dto.DashboardDtos.InventoryValueTrendResponse;
import com.stockwise.api.dto.DashboardDtos.LowStockWatchlistResponse;
import com.stockwise.api.dto.DashboardDtos.MonthlyRestockResponse;
import com.stockwise.api.dto.DashboardDtos.RecentActivityResponse;
import com.stockwise.api.entity.ActivityLog;
import com.stockwise.api.entity.InventoryItem;
import com.stockwise.api.entity.RestockOrder;
import com.stockwise.api.entity.StockStatus;
import com.stockwise.api.repository.ActivityLogRepository;
import com.stockwise.api.repository.InventoryItemRepository;
import com.stockwise.api.repository.RestockOrderRepository;
import com.stockwise.api.repository.StoreManagerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {
    private static final int TREND_MONTH_COUNT = 6;

    private final InventoryItemRepository inventoryItemRepository;
    private final RestockOrderRepository restockOrderRepository;
    private final StoreManagerRepository storeManagerRepository;
    private final ActivityLogRepository activityLogRepository;

    public DashboardService(
            InventoryItemRepository inventoryItemRepository,
            RestockOrderRepository restockOrderRepository,
            StoreManagerRepository storeManagerRepository,
            ActivityLogRepository activityLogRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.restockOrderRepository = restockOrderRepository;
        this.storeManagerRepository = storeManagerRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        List<InventoryItem> inventory = inventoryItemRepository.findAll();
        int unitsOnHand = inventory.stream().mapToInt(InventoryItem::getQuantity).sum();
        long lowStockItems = countByStatus(inventory, StockStatus.LOW);
        long outOfStockItems = countByStatus(inventory, StockStatus.OUT);
        BigDecimal inventoryValue = inventory.stream()
                .map(InventoryItem::inventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal restockBudget = inventory.stream()
                .filter(item -> item.stockStatus() != StockStatus.HEALTHY)
                .map(item -> item.getUnitCost().multiply(BigDecimal.valueOf(item.suggestedOrderQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeManagers = storeManagerRepository.findAll()
                .stream()
                .filter(manager -> manager.getStatus() == null || !manager.getStatus().equalsIgnoreCase("inactive"))
                .count();

        return new DashboardSummaryResponse(
                unitsOnHand,
                lowStockItems,
                outOfStockItems,
                inventoryValue,
                restockBudget,
                activeManagers
        );
    }

    @Transactional(readOnly = true)
    public InventoryStatusResponse inventoryStatus() {
        List<InventoryItem> inventory = inventoryItemRepository.findAll();
        return new InventoryStatusResponse(
                countByStatus(inventory, StockStatus.HEALTHY),
                countByStatus(inventory, StockStatus.LOW),
                countByStatus(inventory, StockStatus.OUT)
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryStockResponse> categoryStock() {
        Map<String, Long> categoryTotals = new LinkedHashMap<>();
        inventoryItemRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(InventoryItem::getCategory, String.CASE_INSENSITIVE_ORDER))
                .forEach(item -> categoryTotals.merge(item.getCategory(), (long) item.getQuantity(), Long::sum));

        return categoryTotals.entrySet()
                .stream()
                .map(entry -> new CategoryStockResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyRestockResponse> monthlyRestock() {
        Map<YearMonth, Long> monthTotals = emptyMonthMap();
        ZoneId zone = ZoneId.systemDefault();
        restockOrderRepository.findTop100ByOrderByCreatedAtDesc()
                .forEach(order -> {
                    YearMonth month = YearMonth.from(order.getCreatedAt().atZone(zone));
                    if (monthTotals.containsKey(month)) {
                        monthTotals.merge(month, (long) order.getRequestedQuantity(), Long::sum);
                    }
                });

        return monthTotals.entrySet()
                .stream()
                .map(entry -> new MonthlyRestockResponse(monthLabel(entry.getKey()), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryValueTrendResponse> inventoryValueTrend() {
        List<InventoryItem> inventory = inventoryItemRepository.findAll();
        Map<YearMonth, BigDecimal> monthTotals = new LinkedHashMap<>();
        emptyMonthMap().keySet().forEach(month -> monthTotals.put(month, BigDecimal.ZERO));
        ZoneId zone = ZoneId.systemDefault();

        inventory.forEach(item -> {
            YearMonth month = YearMonth.from(item.getUpdatedAt().atZone(zone));
            if (monthTotals.containsKey(month)) {
                monthTotals.merge(month, item.inventoryValue(), BigDecimal::add);
            }
        });

        return monthTotals.entrySet()
                .stream()
                .map(entry -> new InventoryValueTrendResponse(monthLabel(entry.getKey()), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LowStockWatchlistResponse> lowStockWatchlist() {
        return inventoryItemRepository.findRestockCandidates()
                .stream()
                .sorted(Comparator.comparingInt(InventoryItem::getQuantity)
                        .thenComparing(InventoryItem::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(10)
                .map(item -> new LowStockWatchlistResponse(
                        item.getSku(),
                        item.getName(),
                        item.getQuantity(),
                        item.getReorderPoint(),
                        titleCaseStatus(item.stockStatus())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> recentActivity() {
        return activityLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .limit(8)
                .map(this::toRecentActivity)
                .toList();
    }

    private long countByStatus(List<InventoryItem> inventory, StockStatus status) {
        return inventory.stream()
                .filter(item -> item.stockStatus() == status)
                .count();
    }

    private Map<YearMonth, Long> emptyMonthMap() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int index = TREND_MONTH_COUNT - 1; index >= 0; index--) {
            months.add(currentMonth.minusMonths(index));
        }
        Map<YearMonth, Long> monthTotals = new LinkedHashMap<>();
        months.forEach(month -> monthTotals.put(month, 0L));
        return monthTotals;
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private String titleCaseStatus(StockStatus status) {
        return switch (status) {
            case HEALTHY -> "Healthy";
            case LOW -> "Low";
            case OUT -> "Out";
        };
    }

    private RecentActivityResponse toRecentActivity(ActivityLog log) {
        return new RecentActivityResponse(
                log.getId(),
                log.getType().name(),
                log.getMessage(),
                log.getActorEmail(),
                log.getCreatedAt()
        );
    }
}
