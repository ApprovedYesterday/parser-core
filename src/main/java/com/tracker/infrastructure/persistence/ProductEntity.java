package com.tracker.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private String sku;

    private String name;

    private BigDecimal price;

    private String currency;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    protected ProductEntity() {}

    public ProductEntity(String sku, String name, BigDecimal price, String currency, Instant lastUpdated) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
