package com.tracker.application.port.out;

import java.util.Optional;

public interface ProxyPort {
    Optional<ProxyConfig> getProxy();
    ProxyConfig nextProxy();

    record ProxyConfig(String host, int port, String type) {
        public String toProxyUrl() {
            return type + "://" + host + ":" + port;
        }
    }
}
