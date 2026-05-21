package com.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ozon.scraping")
public record PlaywrightConfig(
    String baseUrl,
    BrowserConfig browser,
    StealthConfig stealth,
    String userAgent
) {
    public record BrowserConfig(boolean headless, String executablePath) {}
    public record StealthConfig(boolean maskWebdriver, boolean maskWebgl) {}
}
