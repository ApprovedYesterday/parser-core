package com.tracker.infrastructure.scraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Playwright;
import com.tracker.application.port.out.ConfigPort;
import com.tracker.application.port.out.ProxyPort;
import com.tracker.config.PlaywrightConfig;
import com.tracker.config.PlaywrightConfig.BrowserConfig;
import com.tracker.config.PlaywrightConfig.StealthConfig;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Sku;
import com.tracker.infrastructure.util.BackoffRetry;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.wiremock.WireMockContainer;

@Testcontainers
@Tag("integration")
class OzonPlaywrightAdapterTest {

    @Container
    static WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:3.9.2");

    @BeforeAll
    static void setupWireMockStubs() {
        wiremock.stubFor(
            get(urlPathMatching("/product/.*"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "text/html")
                        .withBody(htmlWithInitialState())
                )
        );
    }

    @Test
    void shouldExtractPriceFromInitialState() {
        try (Playwright playwright = Playwright.create()) {
            ConfigPort configPort = () ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
            ProxyPort proxyPort = () -> Optional.empty();
            BackoffRetry backoffRetry = new BackoffRetry();
            ObjectMapper mapper = new ObjectMapper();
            var config = new PlaywrightConfig(
                wiremock.getBaseUrl(),
                new BrowserConfig(true, null),
                new StealthConfig(true, true),
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            );

            var adapter = new OzonPlaywrightAdapter(
                playwright, proxyPort, configPort, backoffRetry, mapper, config
            );

            PricePoint result = adapter.fetchPrice(new Sku("TEST-SKU-001"));

            assertNotNull(result);
            assertEquals("TEST-SKU-001", result.sku().value());
            assertEquals(new BigDecimal("1499.00"), result.price().amount());
            assertEquals("RUB", result.price().currency());
        }
    }

    private static String htmlWithInitialState() {
        return """
            <!DOCTYPE html>
            <html>
            <head><title>Test Product</title></head>
            <body>
            <script>
            window.__INITIAL_STATE__ = {
                "widgets": {
                    "catalog": {
                        "products": [{
                            "price": {
                                "price": "1499.00",
                                "currency": "RUB"
                            }
                        }]
                    }
                }
            };
            </script>
            <h1>Test Product</h1>
            </body>
            </html>
            """;
    }
}
