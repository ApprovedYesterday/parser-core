package com.tracker.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPriceHistoryRepository extends JpaRepository<PriceHistoryEntity, Long> {
    List<PriceHistoryEntity> findBySkuOrderByRecordedAtAsc(String sku);
}
