package com.tracker.presentation.controller;

import com.tracker.application.port.out.ProductRepositoryPort;
import com.tracker.application.service.LttbDownsampler;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Sku;
import com.tracker.presentation.dto.PricePointResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductHistoryController {

    private final ProductRepositoryPort productRepository;
    private final LttbDownsampler downsampler;

    public ProductHistoryController(
        ProductRepositoryPort productRepository,
        LttbDownsampler downsampler
    ) {
        this.productRepository = productRepository;
        this.downsampler = downsampler;
    }

    @GetMapping("/{sku}/history")
    public ResponseEntity<List<PricePointResponse>> getHistory(
        @PathVariable String sku,
        @RequestParam(value = "resolution", required = false) Integer resolution
    ) {
        List<PricePoint> history = productRepository.findHistory(new Sku(sku));
        if (resolution != null && resolution > 0) {
            history = downsampler.downsample(history, resolution);
        }
        List<PricePointResponse> response = history.stream()
            .map(p -> new PricePointResponse(
                p.sku().value(),
                p.price().amount(),
                p.price().currency(),
                p.timestamp()
            ))
            .toList();
        return ResponseEntity.ok(response);
    }
}
