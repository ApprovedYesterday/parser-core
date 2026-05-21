package com.tracker.application.port.out;

import com.tracker.domain.PricePoint;
import com.tracker.domain.Sku;

public interface ScraperPort {
    PricePoint fetchPrice(Sku sku);
}
