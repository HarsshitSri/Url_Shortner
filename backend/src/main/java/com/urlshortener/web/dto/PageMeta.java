package com.urlshortener.web.dto;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {
}
