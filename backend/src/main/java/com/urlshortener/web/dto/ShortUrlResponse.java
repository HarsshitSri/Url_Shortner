package com.urlshortener.web.dto;

import com.urlshortener.domain.SafetyStatus;
import com.urlshortener.domain.UrlStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        UrlStatus status,
        SafetyStatus safetyStatus,
        Instant createdAt,
        List<String> warnings
) {
}
