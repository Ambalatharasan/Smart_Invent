package com.stockwise.api.service;

import com.stockwise.api.dto.RestockDtos.CreateRestockOrderRequest;
import com.stockwise.api.dto.RestockDtos.RestockCsvImportResponse;
import com.stockwise.api.dto.RestockDtos.RestockCandidateResponse;
import com.stockwise.api.dto.RestockDtos.RestockOrderResponse;
import com.stockwise.api.dto.RestockDtos.RestockSummaryResponse;
import com.stockwise.api.dto.RestockDtos.UpdateRestockStatusRequest;
import com.stockwise.api.entity.ActivityType;
import com.stockwise.api.entity.InventoryItem;
import com.stockwise.api.entity.RestockOrder;
import com.stockwise.api.entity.RestockOrderStatus;
import com.stockwise.api.entity.StockStatus;
import com.stockwise.api.repository.InventoryItemRepository;
import com.stockwise.api.repository.RestockOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RestockService {
    private final InventoryItemRepository inventoryItemRepository;
    private final RestockOrderRepository restockOrderRepository;
    private final ActivityLogService activityLogService;

    public RestockService(
            InventoryItemRepository inventoryItemRepository,
            RestockOrderRepository restockOrderRepository,
            ActivityLogService activityLogService
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.restockOrderRepository = restockOrderRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public RestockSummaryResponse summary() {
        List<RestockCandidateResponse> candidates = candidates();
        long outOfStockCount = candidates.stream()
                .filter(candidate -> candidate.status() == StockStatus.OUT)
                .count();
        BigDecimal estimatedBudget = candidates.stream()
                .map(RestockCandidateResponse::estimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RestockSummaryResponse(candidates.size(), outOfStockCount, estimatedBudget, candidates);
    }

    @Transactional(readOnly = true)
    public List<RestockCandidateResponse> candidates() {
        return inventoryItemRepository.findRestockCandidates()
                .stream()
                .map(this::toCandidate)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RestockOrderResponse> orders(String status) {
        List<RestockOrder> orders = status == null || status.isBlank()
                ? restockOrderRepository.findTop100ByOrderByCreatedAtDesc()
                : restockOrderRepository.findByStatusOrderByCreatedAtDesc(RestockOrderStatus.valueOf(status.trim().toUpperCase()));
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public String exportOrdersCsv() {
        List<String> rows = new ArrayList<>();
        rows.add(csvRow(List.of(
                "orderId",
                "itemNum",
                "itemName",
                "requestedQuantity",
                "estimatedCost",
                "status",
                "requestedBy",
                "createdAt",
                "updatedAt",
                "receivedAt"
        )));
        restockOrderRepository.findAllByOrderByCreatedAtDesc()
                .forEach(order -> {
                    InventoryItem item = order.getItem();
                    rows.add(csvRow(List.of(
                            stringValue(order.getId()),
                            item.getSku(),
                            item.getName(),
                            stringValue(order.getRequestedQuantity()),
                            stringValue(order.getEstimatedCost()),
                            order.getStatus().name(),
                            order.getRequestedBy(),
                            stringValue(order.getCreatedAt()),
                            stringValue(order.getUpdatedAt()),
                            stringValue(order.getReceivedAt())
                    )));
                });
        return String.join("\n", rows) + "\n";
    }

    public String importTemplateCsv() {
        return """
                orderId,itemNum,requestedQuantity,status
                ,ITEM-0001,30,DRAFT
                ,ITEM-0002,36,SUBMITTED
                """;
    }

    @Transactional
    public RestockOrderResponse createOrder(CreateRestockOrderRequest request, String actorEmail) {
        InventoryItem item = inventoryItemRepository.findById(request.itemId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
        int quantity = request.requestedQuantity() == null ? item.suggestedOrderQuantity() : request.requestedQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be greater than zero");
        }
        BigDecimal estimatedCost = item.getUnitCost().multiply(BigDecimal.valueOf(quantity));
        RestockOrder order = restockOrderRepository.save(new RestockOrder(item, quantity, estimatedCost, actorEmail));
        activityLogService.log(ActivityType.RESTOCK_ORDERED, "Created restock order for " + item.getSku(), item.getSku(), quantity, null, actorEmail);
        return toResponse(order);
    }

    @Transactional
    public RestockOrderResponse updateStatus(Long id, UpdateRestockStatusRequest request, String actorEmail) {
        RestockOrder order = restockOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restock order not found"));
        applyStatus(order, request.status(), actorEmail);
        return toResponse(order);
    }

    @Transactional
    public RestockCsvImportResponse importOrdersCsv(String content, String actorEmail) {
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        Map<String, Integer> headers = headers(rows.get(0));
        if ((!headers.containsKey("itemnum") && !headers.containsKey("sku")) || !headers.containsKey("requestedquantity")) {
            throw new IllegalArgumentException("CSV must include Item.Num and requestedQuantity columns");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (isBlankRow(row)) {
                continue;
            }
            int rowNumber = index + 1;
            try {
                String orderIdValue = value(row, headers, "orderid");
                String sku = value(row, headers, "itemnum");
                if (sku.isBlank()) {
                    sku = value(row, headers, "sku");
                }
                String quantityValue = value(row, headers, "requestedquantity");
                String statusValue = value(row, headers, "status");
                RestockOrderStatus status = statusValue.isBlank() ? RestockOrderStatus.DRAFT : RestockOrderStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT));

                if (!orderIdValue.isBlank()) {
                    RestockOrder order = restockOrderRepository.findById(Long.parseLong(orderIdValue.trim()))
                            .orElseThrow(() -> new EntityNotFoundException("Restock order not found"));
                    applyStatus(order, status, actorEmail);
                    updated++;
                    continue;
                }

                if (sku.isBlank()) {
                    throw new IllegalArgumentException("Item.Num is required for new restock orders");
                }
                InventoryItem item = inventoryItemRepository.findBySkuIgnoreCase(sku.trim())
                        .orElseThrow(() -> new EntityNotFoundException("Inventory Item.Num not found"));
                int quantity = quantityValue.isBlank() ? item.suggestedOrderQuantity() : Integer.parseInt(quantityValue.trim());
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Requested quantity must be greater than zero");
                }

                BigDecimal estimatedCost = item.getUnitCost().multiply(BigDecimal.valueOf(quantity));
                RestockOrder order = restockOrderRepository.save(new RestockOrder(item, quantity, estimatedCost, actorEmail));
                activityLogService.log(ActivityType.RESTOCK_ORDERED, "Imported restock order for " + item.getSku(), item.getSku(), quantity, null, actorEmail);
                if (status != RestockOrderStatus.DRAFT) {
                    applyStatus(order, status, actorEmail);
                }
                created++;
            } catch (RuntimeException ex) {
                skipped++;
                errors.add("Row " + rowNumber + ": " + ex.getMessage());
            }
        }

        return new RestockCsvImportResponse(created, updated, skipped, errors);
    }

    private void applyStatus(RestockOrder order, RestockOrderStatus status, String actorEmail) {
        RestockOrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        InventoryItem item = order.getItem();
        if (status == RestockOrderStatus.RECEIVED && previousStatus != RestockOrderStatus.RECEIVED) {
            LocalDate receivedDate = LocalDate.now();
            item.setQuantity(item.getQuantity() + order.getRequestedQuantity());
            item.setLastRestockDate(receivedDate);
            item.setLastRestockQuantity(order.getRequestedQuantity());
            activityLogService.log(ActivityType.RESTOCK_RECEIVED, "Received restock order for " + item.getSku(), item.getSku(), order.getRequestedQuantity(), receivedDate, actorEmail);
        } else {
            activityLogService.log(ActivityType.RESTOCK_ORDERED, "Changed restock order " + order.getId() + " to " + status, item.getSku(), order.getRequestedQuantity(), item.getLastRestockDate(), actorEmail);
        }
    }

    private RestockCandidateResponse toCandidate(InventoryItem item) {
        int suggestedQuantity = item.suggestedOrderQuantity();
        return new RestockCandidateResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getCategory(),
                item.getSupplier(),
                item.getQuantity(),
                item.getReorderPoint(),
                item.stockStatus(),
                suggestedQuantity,
                item.getUnitCost().multiply(BigDecimal.valueOf(suggestedQuantity)),
                item.getLeadTimeDays(),
                item.getLastRestockDate(),
                item.getLastRestockQuantity()
        );
    }

    private RestockOrderResponse toResponse(RestockOrder order) {
        InventoryItem item = order.getItem();
        return new RestockOrderResponse(
                order.getId(),
                item.getId(),
                item.getSku(),
                item.getName(),
                order.getRequestedQuantity(),
                order.getEstimatedCost(),
                order.getStatus(),
                order.getRequestedBy(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getReceivedAt()
        );
    }

    private static String csvRow(List<String> cells) {
        return cells.stream()
                .map(RestockService::escapeCsv)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static String escapeCsv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        return value.toString();
    }

    private static Map<String, Integer> headers(List<String> headerRow) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int index = 0; index < headerRow.size(); index++) {
            headers.put(normalizeHeader(headerRow.get(index)), index);
        }
        return headers;
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String value(List<String> row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(header);
        if (index == null || index >= row.size()) {
            return "";
        }
        return row.get(index).trim();
    }

    private static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(cell -> cell == null || cell.isBlank());
    }

    private static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        String text = content == null ? "" : content.replace("\uFEFF", "");
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        cell.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(current);
                }
                continue;
            }
            if (current == '"') {
                quoted = true;
            } else if (current == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (current == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else if (current != '\r') {
                cell.append(current);
            }
        }
        row.add(cell.toString());
        if (!isBlankRow(row)) {
            rows.add(row);
        }
        return rows;
    }
}
