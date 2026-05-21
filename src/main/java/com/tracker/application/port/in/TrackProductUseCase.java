package com.tracker.application.port.in;

import com.tracker.domain.Sku;

public interface TrackProductUseCase {
    void track(Sku sku);
}
