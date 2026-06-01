package com.tracker.infrastructure.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tracker.application.port.out.ProxyPort.ProxyConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class DynamicProxyProviderTest {

    @Test
    void shouldRotateProxiesInOrder() {
        var provider = new DynamicProxyProvider(List.of(
            new ProxyConfig("a.com", 80, "http"),
            new ProxyConfig("b.com", 80, "http"),
            new ProxyConfig("c.com", 80, "http")
        ));

        assertEquals("a.com", provider.nextProxy().host());
        assertEquals("b.com", provider.nextProxy().host());
        assertEquals("c.com", provider.nextProxy().host());
        assertEquals("a.com", provider.nextProxy().host());
    }

    @Test
    void shouldReturnNullWhenEmpty() {
        var provider = new DynamicProxyProvider(List.of());
        assertNull(provider.nextProxy());
    }

    @Test
    void shouldReturnSingleProxyRepeatedly() {
        var provider = new DynamicProxyProvider(List.of(
            new ProxyConfig("s.com", 8080, "http")
        ));

        assertNotNull(provider.nextProxy());
        assertNotNull(provider.nextProxy());
    }

    @Test
    void getProxyShouldMatchNextProxy() {
        var provider = new DynamicProxyProvider(List.of(
            new ProxyConfig("x.com", 3128, "http")
        ));

        assertEquals(provider.nextProxy(), provider.getProxy().orElse(null));
    }
}
