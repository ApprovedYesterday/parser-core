package com.tracker.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_history")
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;

    private BigDecimal price;

    private String currency;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    protected PriceHistoryEntity() {}

    public PriceHistoryEntity(String sku, BigDecimal price, String currency, Instant recordedAt) {
        this.sku = sku;
        this.price = price;
        this.currency = currency;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
