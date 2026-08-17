package com.startupgraph.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cognodb")
public record DbProperties(String uri, String user, String password, boolean seed) {

    public boolean configured() {
        return uri != null && !uri.isBlank();
    }
}
