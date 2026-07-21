package com.urlshortener.auth.dto;

public record AuthResponse(String token, String tokenType, UserResponse user) {
}
