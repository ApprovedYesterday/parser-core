# Scrapper — Ozon Price Tracker

---
Backend for tracking product prices on Ozon with anti-bot bypass, price history,
and a browser extension integration API.

## Stack

---
- Java 21, Spring Boot 4.0.6
- Microsoft Playwright for Java (Chromium, stealth-patched)
- PostgreSQL + Spring Data JPA
- Redis (rate limiter — Token Bucket via Lua)
- Testcontainers (integration tests)
- WireMock (Ozon responses mocking)

## Architecture

---
Hexagonal (Ports and Adapters) with strict layer isolation:

| Layer | Package | Dependencies |
|---|---|---|
| Domain | `com.tracker.domain` | None (pure Java records) |
| Application | `com.tracker.application` | Domain only (ports, services, algorithms) |
| Infrastructure | `com.tracker.infrastructure` | Application + Framework |
| Presentation | `com.tracker.presentation` | Application + Spring Web |
| Config | `com.tracker.config` | Spring `@Configuration`, beans wiring |

## Quick start

---
```bash
# Prerequisites: Java 21, Docker, Playwright browsers
mvn clean package -DskipTests
mvn spring-boot:run
```

Integration tests require Docker:
```bash
mvn test
playwright install chromium   # if running OzonPlaywrightAdapterTest
```

## API

---
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/track` | Start tracking product by SKU |
| `GET` | `/api/v1/products/{sku}/history?resolution=N` | Price history (optionally downsampled via LTTB) |

## Project structure

---
```
src/main/java/com/tracker/
├── domain/          # Sku, Money, Product, PricePoint
├── application/
│   ├── port/in/     # TrackProductUseCase
│   ├── port/out/    # ScraperPort, ProductRepositoryPort, RateLimiterPort, ProxyPort, ConfigPort
│   └── service/     # TrackProductService, LttbDownsampler
├── infrastructure/
│   ├── scraping/    # OzonPlaywrightAdapter, ConsistentHashRing
│   ├── persistence/ # ProductEntity, PriceHistoryEntity, PostgresProductRepository
│   ├── ratelimit/   # RedisRateLimiter
│   ├── proxy/       # DynamicProxyProvider
│   └── util/        # BackoffRetry (exponential backoff + jitter)
├── presentation/
│   ├── dto/         # TrackRequest, PricePointResponse
│   └── controller/  # TrackController, ProductHistoryController
└── config/          # PlaywrightConfig, RateLimiterConfig, DataConfig, PlaywrightAdapterConfig
```

## Key design decisions 

---
- **Ozon anti-bot**: Playwright with stealth scripts (`navigator.webdriver` + WebGL override).
  Price extracted via `page.evaluate()` from `window.__INITIAL_STATE__`, not DOM parsing.
- **Proxy rotation**: `DynamicProxyProvider` reads proxy pool from config, round-robins on
  each new browser context.
- **Consistent hashing**: Seller sessions are pinned to `BrowserContext` slots via
  `ConsistentHashRing` — cookies and sessions persist across requests for the same seller.
- **Rate limiting**: Token Bucket in Redis (Lua script atomically refills and consumes tokens).
- **LTTB downsampling**: Largest Triangle Three Buckets algorithm reduces price history
  points for browser extension charts.

