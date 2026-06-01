package com.tracker.infrastructure.proxy;

import com.tracker.application.port.out.ProxyPort;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicProxyProvider implements ProxyPort {

    private final List<ProxyConfig> proxies;
    private final AtomicInteger counter = new AtomicInteger(0);

    public DynamicProxyProvider(List<ProxyConfig> proxies) {
        this.proxies = proxies;
    }

    @Override
    public Optional<ProxyConfig> getProxy() {
        return Optional.ofNullable(nextProxy());
    }

    @Override
    public ProxyConfig nextProxy() {
        if (proxies.isEmpty()) return null;
        int index = Math.abs(counter.getAndIncrement()) % proxies.size();
        return proxies.get(index);
    }
}
