package com.tracker.infrastructure.persistence;

import com.tracker.application.port.out.ProductRepositoryPort;
import com.tracker.domain.Money;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Product;
import com.tracker.domain.Sku;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresProductRepository implements ProductRepositoryPort {

    private final SpringDataProductRepository productRepo;
    private final SpringDataPriceHistoryRepository historyRepo;

    public PostgresProductRepository(
        SpringDataProductRepository productRepo,
        SpringDataPriceHistoryRepository historyRepo
    ) {
        this.productRepo = productRepo;
        this.historyRepo = historyRepo;
    }

    @Override
    public void save(Product product) {
        productRepo.save(new ProductEntity(
            product.sku().value(),
            product.name(),
            product.currentPrice().amount(),
            product.currentPrice().currency(),
            Instant.now()
        ));
    }

    @Override
    public void savePricePoint(PricePoint pricePoint) {
        historyRepo.save(new PriceHistoryEntity(
            pricePoint.sku().value(),
            pricePoint.price().amount(),
            pricePoint.price().currency(),
            pricePoint.timestamp()
        ));
    }

    @Override
    public List<PricePoint> findHistory(Sku sku) {
        return historyRepo.findBySkuOrderByRecordedAtAsc(sku.value())
            .stream()
            .map(e -> new PricePoint(
                new Sku(e.getSku()),
                new Money(e.getPrice(), e.getCurrency()),
                e.getRecordedAt()
            ))
            .toList();
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return productRepo.findById(sku.value())
            .map(e -> new Product(
                new Sku(e.getSku()),
                e.getName(),
                new Money(e.getPrice(), e.getCurrency())
            ));
    }
}
