package com.tracker.application.service;

import com.tracker.application.port.in.TrackProductUseCase;
import com.tracker.application.port.out.ProductRepositoryPort;
import com.tracker.application.port.out.RateLimiterPort;
import com.tracker.application.port.out.ScraperPort;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Product;
import com.tracker.domain.Sku;
import org.springframework.stereotype.Service;

@Service
public class TrackProductService implements TrackProductUseCase {

    private final ScraperPort scraperPort;
    private final ProductRepositoryPort productRepository;
    private final RateLimiterPort rateLimiter;

    public TrackProductService(
        ScraperPort scraperPort,
        ProductRepositoryPort productRepository,
        RateLimiterPort rateLimiter
    ) {
        this.scraperPort = scraperPort;
        this.productRepository = productRepository;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void track(Sku sku) {
        if (!rateLimiter.tryConsume("sku:" + sku.value(), 1)) {
            throw new RuntimeException("Rate limit exceeded for SKU: " + sku.value());
        }
        PricePoint point = scraperPort.fetchPrice(sku);
        productRepository.save(new Product(sku, "Unknown", point.price()));
        productRepository.savePricePoint(point);
    }
}
