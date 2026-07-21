package com.urlshortener.web.dto;

import com.urlshortener.domain.UrlStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateShortUrlRequest(
        @Size(max = 2048, message = "originalUrl must be at most 2048 characters")
        @Pattern(
                regexp = "^https?://.+",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "originalUrl must start with http:// or https://")
        String originalUrl,

        UrlStatus status
) {
}
