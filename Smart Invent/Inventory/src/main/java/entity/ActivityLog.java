package com.stockwise.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 80)
    private String itemSku;

    private Integer quantity;

    private LocalDate lastRestockDate;

    @Column(nullable = false, length = 160)
    private String actorEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityLog() {
    }

    public ActivityLog(ActivityType type, String message, String itemSku, Integer quantity, LocalDate lastRestockDate, String actorEmail) {
        this.type = type;
        this.message = message;
        this.itemSku = itemSku;
        this.quantity = quantity;
        this.lastRestockDate = lastRestockDate;
        this.actorEmail = actorEmail;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getItemSku() {
        return itemSku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDate getLastRestockDate() {
        return lastRestockDate;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
