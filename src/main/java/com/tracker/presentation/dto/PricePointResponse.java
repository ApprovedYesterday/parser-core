package com.tracker.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PricePointResponse(
    String sku,
    BigDecimal price,
    String currency,
    Instant timestamp
) {}
