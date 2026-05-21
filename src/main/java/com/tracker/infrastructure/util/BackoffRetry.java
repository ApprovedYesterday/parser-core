package com.tracker.infrastructure.util;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class BackoffRetry {

    public <T> T executeWithRetry(Callable<T> operation, int maxRetries, Duration baseDelay) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return operation.call();
            } catch (RetryableException e) {
                if (attempt == maxRetries - 1) {
                    throw new RuntimeException("All " + maxRetries + " retries exhausted", e);
                }
                long delay = (long) (baseDelay.toMillis() * Math.pow(2, attempt));
                long jitter = ThreadLocalRandom.current().nextLong(delay);
                try {
                    Thread.sleep(delay + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            } catch (Exception e) {
                throw new RuntimeException("Non-retryable error during scraping", e);
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }
}
