package com.urlshortener.web.dto;

import java.util.List;

public record ShortUrlResult(ShortUrlResponse url, List<String> warnings) {
}
