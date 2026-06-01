package com.tracker.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ozon.scraping")
public record PlaywrightConfig(
    String baseUrl,
    BrowserConfig browser,
    StealthConfig stealth,
    String userAgent,
    List<ScrapingProxy> proxies,
    int sellerPoolSize
) {
    public record BrowserConfig(boolean headless, String executablePath) {}
    public record StealthConfig(boolean maskWebdriver, boolean maskWebgl) {}
    public record ScrapingProxy(String host, int port, String type) {}
}
