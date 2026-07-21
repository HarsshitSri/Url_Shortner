package com.urlshortener.auth.dto;

import java.util.List;

public record AuthProvidersResponse(List<String> providers) {
}
