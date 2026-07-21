package com.urlshortener.web.dto;

import java.util.List;

public record ErrorBody(
        int status,
        String message,
        String path,
        List<String> details
) {
}
