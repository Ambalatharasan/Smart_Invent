package com.stockwise.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 120)
    private String supplier;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reorderPoint;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(nullable = false)
    private int leadTimeDays;

    @Column(nullable = false, length = 80)
    private String location;

    @Column(length = 500)
    private String notes;

    private LocalDate lastRestockDate;

    private Integer lastRestockQuantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(
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
            LocalDate lastRestockDate,
            Integer lastRestockQuantity
    ) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.supplier = supplier;
        setQuantity(quantity);
        setReorderPoint(reorderPoint);
        setUnitCost(unitCost);
        setRetailPrice(retailPrice);
        setLeadTimeDays(leadTimeDays);
        this.location = location;
        this.notes = notes;
        this.lastRestockDate = lastRestockDate;
        this.lastRestockQuantity = lastRestockQuantity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public StockStatus stockStatus() {
        if (quantity <= 0) {
            return StockStatus.OUT;
        }
        if (quantity <= reorderPoint) {
            return StockStatus.LOW;
        }
        return StockStatus.HEALTHY;
    }

    public int suggestedOrderQuantity() {
        return Math.max((reorderPoint * 2) - quantity, reorderPoint);
    }

    public BigDecimal inventoryValue() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal retailValue() {
        return retailPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = Math.max(0, reorderPoint);
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost.max(BigDecimal.ZERO);
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice == null ? BigDecimal.ZERO : retailPrice.max(BigDecimal.ZERO);
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = Math.max(0, leadTimeDays);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getLastRestockDate() {
        return lastRestockDate;
    }

    public void setLastRestockDate(LocalDate lastRestockDate) {
        this.lastRestockDate = lastRestockDate;
    }

    public Integer getLastRestockQuantity() {
        return lastRestockQuantity;
    }

    public void setLastRestockQuantity(Integer lastRestockQuantity) {
        this.lastRestockQuantity = lastRestockQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
