package com.parkminje.pingpong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        List<String> allowedOrigins,
        boolean sampleData,
        Security security,
        BootstrapAdmin bootstrapAdmin
) {

    public record Security(int loginMaxAttempts, int loginLockMinutes) {
    }

    public record BootstrapAdmin(String username, String password, String email) {
    }

    public AppProperties {
        if (name == null || name.isBlank()) {
            name = "탁구 대회";
        }
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream().filter(s -> s != null && !s.isBlank()).toList();
        if (security == null) {
            security = new Security(5, 15);
        }
        if (bootstrapAdmin == null) {
            bootstrapAdmin = new BootstrapAdmin("admin", null, "admin@example.com");
        }
    }
}
