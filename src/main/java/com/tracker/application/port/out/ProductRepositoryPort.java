package com.tracker.application.port.out;

import com.tracker.domain.PricePoint;
import com.tracker.domain.Product;
import com.tracker.domain.Sku;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    void save(Product product);
    void savePricePoint(PricePoint pricePoint);
    List<PricePoint> findHistory(Sku sku);
    Optional<Product> findBySku(Sku sku);
}
