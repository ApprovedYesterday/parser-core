package com.tracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Playwright;
import com.tracker.application.port.out.ConfigPort;
import com.tracker.application.port.out.ProxyPort;
import com.tracker.infrastructure.proxy.DynamicProxyProvider;
import com.tracker.infrastructure.scraping.OzonPlaywrightAdapter;
import com.tracker.infrastructure.util.BackoffRetry;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlaywrightConfig.class)
public class PlaywrightAdapterConfig {

    @Bean(destroyMethod = "close")
    Playwright playwright() {
        return Playwright.create();
    }

    @Bean
    ConfigPort configPort(PlaywrightConfig config) {
        return () -> config.userAgent();
    }

    @Bean
    ProxyPort proxyPort(PlaywrightConfig config) {
        List<ProxyPort.ProxyConfig> proxies = config.proxies().stream()
            .map(p -> new ProxyPort.ProxyConfig(p.host(), p.port(), p.type()))
            .toList();
        return new DynamicProxyProvider(proxies);
    }

    @Bean
    BackoffRetry backoffRetry() {
        return new BackoffRetry();
    }

    @Bean(destroyMethod = "close")
    OzonPlaywrightAdapter ozonPlaywrightAdapter(
        Playwright playwright,
        ProxyPort proxyPort,
        ConfigPort configPort,
        BackoffRetry backoffRetry,
        ObjectMapper objectMapper,
        PlaywrightConfig config
    ) {
        return new OzonPlaywrightAdapter(
            playwright, proxyPort, configPort, backoffRetry, objectMapper, config
        );
    }
}
