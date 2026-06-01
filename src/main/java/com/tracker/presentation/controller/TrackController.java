package com.tracker.presentation.controller;

import com.tracker.application.port.in.TrackProductUseCase;
import com.tracker.domain.Sku;
import com.tracker.presentation.dto.TrackRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TrackController {

    private final TrackProductUseCase trackProduct;

    public TrackController(TrackProductUseCase trackProduct) {
        this.trackProduct = trackProduct;
    }

    @PostMapping("/track")
    public ResponseEntity<Void> track(@RequestBody TrackRequest request) {
        trackProduct.track(new Sku(request.sku()));
        return ResponseEntity.ok().build();
    }
}
