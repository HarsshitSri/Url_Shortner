package com.urlshortener.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Normalizes Neon / libpq {@code DATABASE_URL} values into a JDBC URL Spring Boot accepts.
 * Strips embedded userinfo and unsupported query params such as {@code channel_binding}.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("spring.datasource.url"));
        if (!StringUtils.hasText(raw)) {
            return;
        }

        NormalizedDatabaseUrl normalized = normalize(raw);
        if (normalized == null) {
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", normalized.jdbcUrl());
        if (StringUtils.hasText(normalized.username())
                && !StringUtils.hasText(environment.getProperty("DATABASE_USERNAME"))
                && !StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
            props.put("spring.datasource.username", normalized.username());
        }
        if (StringUtils.hasText(normalized.password())
                && !StringUtils.hasText(environment.getProperty("DATABASE_PASSWORD"))
                && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
            props.put("spring.datasource.password", normalized.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    static NormalizedDatabaseUrl normalize(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String working = trimmed;
        if (working.startsWith("postgres://") || working.startsWith("postgresql://")) {
            working = "jdbc:postgresql://" + working.substring(working.indexOf("://") + 3);
        }
        if (!working.startsWith("jdbc:postgresql://")) {
            return null;
        }

        String withoutJdbc = working.substring("jdbc:".length()); // postgresql://...
        URI uri;
        try {
            uri = URI.create(withoutJdbc);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        String userInfo = uri.getRawUserInfo();
        String username = null;
        String password = null;
        if (StringUtils.hasText(userInfo)) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            } else {
                username = decode(userInfo);
            }
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            return null;
        }
        int port = uri.getPort();
        String path = uri.getRawPath();
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            path = "/neondb";
        }

        String query = sanitizeQuery(uri.getRawQuery(), host);
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
        if (port > 0) {
            jdbc.append(':').append(port);
        }
        jdbc.append(path);
        if (StringUtils.hasText(query)) {
            jdbc.append('?').append(query);
        }

        return new NormalizedDatabaseUrl(jdbc.toString(), username, password);
    }

    private static String sanitizeQuery(String rawQuery, String host) {
        boolean neonHost = host != null && host.contains("neon.tech");
        if (!StringUtils.hasText(rawQuery)) {
            return neonHost ? "sslmode=require" : null;
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            String normalizedKey = key.trim();
            if (!StringUtils.hasText(normalizedKey)) {
                continue;
            }
            // libpq uses channel_binding; JDBC rejects it.
            if ("channel_binding".equalsIgnoreCase(normalizedKey)) {
                continue;
            }
            if ("channelBinding".equalsIgnoreCase(normalizedKey)) {
                continue;
            }
            params.put(normalizedKey, value);
        }
        if (neonHost && !params.containsKey("sslmode")) {
            params.put("sslmode", "require");
        }

        List<String> parts = new ArrayList<>();
        params.forEach((k, v) -> parts.add(k + "=" + v));
        return String.join("&", parts);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    record NormalizedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
