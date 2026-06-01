package com.tracker.infrastructure.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.tracker.application.port.out.ConfigPort;
import com.tracker.application.port.out.ProxyPort;
import com.tracker.application.port.out.ScraperPort;
import com.tracker.config.PlaywrightConfig;
import com.tracker.domain.Money;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Sku;
import com.tracker.infrastructure.util.BackoffRetry;
import com.tracker.infrastructure.util.RetryableException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class OzonPlaywrightAdapter implements ScraperPort, AutoCloseable {

    private static final String STEALTH_SCRIPT =
        """
        Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
        const origGetParameter = WebGLRenderingContext.prototype.getParameter;
        WebGLRenderingContext.prototype.getParameter = function(param) {
            if (param === 37445) return 'Intel Inc.';
            if (param === 37446) return 'Intel Iris OpenGL Engine';
            return origGetParameter.call(this, param);
        };
        """;

    static final String EXTRACT_PRICE_SCRIPT =
        """
        (() => {
            const state = window.__INITIAL_STATE__ || window.__NEXT_DATA__;
            if (!state) return null;
            try {
                const data = typeof state === 'string' ? JSON.parse(state) : state;
                const price =
                    data?.widgets?.catalog?.products?.[0]?.price?.price ??
                    data?.product?.price?.price ??
                    data?.product?.price ??
                    data?.props?.pageProps?.product?.price;
                return price != null ? String(price) : null;
            } catch(e) {
                return null;
            }
        })()
        """;

    private final Playwright playwright;
    private final Browser browser;
    private final ProxyPort proxyPort;
    private final ConfigPort configPort;
    private final BackoffRetry backoffRetry;
    private final ObjectMapper objectMapper;
    private final PlaywrightConfig config;
    private final ConsistentHashRing<Integer> hashRing;
    private final Map<Integer, BrowserContext> contextPool = new ConcurrentHashMap<>();

    public OzonPlaywrightAdapter(
        Playwright playwright,
        ProxyPort proxyPort,
        ConfigPort configPort,
        BackoffRetry backoffRetry,
        ObjectMapper objectMapper,
        PlaywrightConfig config
    ) {
        this.playwright = playwright;
        this.proxyPort = proxyPort;
        this.configPort = configPort;
        this.backoffRetry = backoffRetry;
        this.objectMapper = objectMapper;
        this.config = config;

        var launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(config.browser().headless());
        if (config.browser().executablePath() != null) {
            launchOptions.setExecutablePath(
                java.nio.file.Paths.get(config.browser().executablePath())
            );
        }
        this.browser = playwright.chromium().launch(launchOptions);

        int poolSize = Math.max(config.sellerPoolSize(), 1);
        this.hashRing = new ConsistentHashRing<>(100);
        for (int i = 0; i < poolSize; i++) {
            hashRing.addNode(i);
        }
    }

    @Override
    public PricePoint fetchPrice(Sku sku) {
        try {
            return doFetch(sku);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch price for SKU: " + sku.value(), e);
        }
    }

    @Override
    public void close() {
        for (BrowserContext ctx : contextPool.values()) {
            ctx.close();
        }
        if (browser != null) browser.close();
    }

    private PricePoint doFetch(Sku sku) {
        Callable<PricePoint> task = () -> fetchWithBrowser(sku);
        return backoffRetry.executeWithRetry(task, 3, Duration.ofSeconds(2));
    }

    private PricePoint fetchWithBrowser(Sku sku) {
        String sellerId = extractSellerId(sku);
        int slot = hashRing.getNode(sellerId);
        BrowserContext context = getOrCreateContext(slot);
        Page page = context.newPage();
        try {
            String url = config.baseUrl() + "/product/" + sku.value();
            Response response = page.navigate(url);

            if (response != null && (response.status() == 429 || response.status() == 403)) {
                throw new RetryableException("HTTP " + response.status() + " for SKU: " + sku.value());
            }

            Object rawPrice = page.evaluate(EXTRACT_PRICE_SCRIPT);
            if (rawPrice == null) {
                throw new RuntimeException(
                    "Could not extract price from page for SKU: " + sku.value()
                );
            }

            BigDecimal amount = new BigDecimal(rawPrice.toString());
            Money money = new Money(amount, "RUB");
            return new PricePoint(sku, money, Instant.now());
        } finally {
            page.close();
        }
    }

    private BrowserContext getOrCreateContext(int slot) {
        return contextPool.computeIfAbsent(slot, k -> {
            var contextOptions = new Browser.NewContextOptions();
            String userAgent = configPort.getUserAgent();
            if (userAgent != null && !userAgent.isBlank()) {
                contextOptions.setUserAgent(userAgent);
            }
            ProxyPort.ProxyConfig proxy = proxyPort.nextProxy();
            if (proxy != null) {
                contextOptions.setProxy(proxy.toProxyUrl());
            }
            BrowserContext ctx = browser.newContext(contextOptions);
            ctx.addInitScript(STEALTH_SCRIPT);
            return ctx;
        });
    }

    private String extractSellerId(Sku sku) {
        String value = sku.value();
        if (value.matches("\\d+") && value.length() > 4) {
            return value.substring(0, 4);
        }
        return value;
    }
}
