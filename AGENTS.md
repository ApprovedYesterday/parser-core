# AGENTS.md — parser-core (Ozon Price Tracker)

Spring Boot 4.0.6 / Java 21 Maven project.
**Architecture:** Hexagonal (Ports and Adapters) / Domain-Driven Design (DDD).

## 🤖 AI Agent Directives (Rules)
1. **Strict Hexagonal Architecture:** Domain layer (`domain`) MUST NOT import anything from `infrastructure`, `presentation`, or `spring` packages. Dependencies point inwards.
2. **Immutability:** Use Java `record` for all DTOs, Value Objects, and API responses.
3. **Concurrency:** Use Java Virtual Threads (`Thread.ofVirtual()`) for I/O bound scraping tasks and Playwright execution.
4. **Testing:** Every new Infrastructure Adapter (e.g., Postgres, Playwright) must have an integration test using `Testcontainers`.
5. **Anti-Bot First:** Never hardcode User-Agents, Cookies, or Proxy lists. Fetch them dynamically via `ProxyPort` and `ConfigPort`.
6. **No Custom JSON Parsers:** Use `Jackson` for Ozon's internal JSON APIs. Do not use parser-combinators for standard JSON/HTML parsing.

## Commands

All commands run from `parser-core/`:

- **Build:** `mvn clean package -DskipTests`
- **Test:** `mvn test` *(Requires Docker running for Testcontainers)*
- **Run:** `mvn spring-boot:run`
- **Format:** `mvn spotless:apply`

## 🏗 Architecture & Project Structure

The project follows a strict Hexagonal structure to ensure easy transition to microservices later.

| Path | Purpose |
|---|---|
| `src/main/java/com/tracker/domain/` | **Core:** Entities (`Product`, `PricePoint`), Value Objects (`Sku`, `Money`), Domain Exceptions. Zero framework dependencies. |
| `src/main/java/com/tracker/application/` | **Use Cases:** Services orchestrating domain logic. Defines `InboundPorts` (interfaces driven by API). Contains algorithms (LTTB, Rate Limiting). |
| `src/main/java/com/tracker/infrastructure/` | **Adapters:** `OzonPlaywrightAdapter`, `PostgresRepository`, `RedisRateLimiter`. Implements `OutboundPorts` defined in Application. |
| `src/main/java/com/tracker/presentation/` | **API:** REST Controllers (`/api/v1/track`), WebSockets/Server-Sent Events for the Browser Extension. |
| `src/main/java/com/tracker/config/` | **Config:** Spring `@Configuration`, Playwright Bean setup, Security, Jackson config. |
| `src/main/resources/application.yml` | App config, DB credentials, Playwright stealth settings, Proxy pool URLs. |
| `src/test/java/.../integration/` | Testcontainers tests for DB, Redis, and mocked Ozon responses. |

## 🗺 Implementation Plan (Sequence)

### Phase 1: Domain & Ports (The Core)
- [ ] Define `Product` and `PriceHistory` aggregates in `domain`.
- [ ] Define `ScraperPort` (Outbound) with method `fetchPrice(Sku sku)`.
- [ ] Define `TrackProductUseCase` (Inbound) in `application`.
- [ ] Implement **LTTB (Largest Triangle Three Buckets)** algorithm in `application` to downsample price history for the extension's charts.

### Phase 2: Infrastructure & Scraping Agent
- [ ] Implement `OzonPlaywrightAdapter` using Microsoft Playwright for Java.
- [ ] Add logic to intercept XHR/Fetch requests via Playwright `route()` to catch Ozon's internal GraphQL/JSON API before DOM rendering.
- [ ] Implement **Exponential Backoff with Jitter** for handling 429 (Too Many Requests) and 403 HTTP responses.
- [ ] Inject stealth scripts to mask `navigator.webdriver` and WebGL fingerprints.

### Phase 3: Persistence & API (Browser Extension Integration)
- [ ] Implement `SpringDataProductRepository` (PostgreSQL).
- [ ] Create REST endpoints for the Browser Extension (`POST /api/v1/track`, `GET /api/v1/products/{sku}/history`).
- [ ] Implement **Token Bucket** algorithm using Redis to rate-limit requests per Ozon category/seller.

### Phase 4: Anti-Bot & Scaling (Pre-Microservices)
- [ ] Add Proxy rotation logic inside the Playwright Adapter (Residential/Mobile proxies).
- [ ] Implement **Consistent Hashing** to bind specific Ozon sellers to specific scraper worker threads to reuse cookies/sessions.

## 📝 Notes & Gotchas
- **Ozon Anti-Bot:** Ozon uses advanced WAF (QRATES / Cloudflare). Simple `OkHttp` or `WebClient` requests will fail with 403. Playwright with stealth patches is mandatory for the MVP.
- **Data Extraction:** Ozon embeds initial state in `<script id="__NEXT_DATA__">` or `window.__INITIAL_STATE__`. Extract this JSON via Playwright `page.evaluate()` rather than parsing raw HTML DOM nodes, as DOM classes change frequently due to A/B testing.
- **Browser Extension Role:** The extension acts as a "Spy". It sends the current URL and DOM-extracted SKU to the Core. The Core does the heavy lifting. Do not put heavy parsing logic in the extension.
- **Package Structure:** Ensure POM `groupId` is `com.tracker` and base package is `com.tracker`. Fix any Initializr mismatches immediately.
