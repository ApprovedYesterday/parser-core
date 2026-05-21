package com.tracker.domain;

import java.time.Instant;

public record PricePoint(Sku sku, Money price, Instant timestamp) {}
