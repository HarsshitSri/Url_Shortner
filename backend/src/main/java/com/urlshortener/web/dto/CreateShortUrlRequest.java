package com.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
        @NotBlank(message = "originalUrl is required")
        @Size(max = 2048, message = "originalUrl must be at most 2048 characters")
        @Pattern(
                regexp = "^https?://.+",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "originalUrl must start with http:// or https://")
        String originalUrl
) {
}
