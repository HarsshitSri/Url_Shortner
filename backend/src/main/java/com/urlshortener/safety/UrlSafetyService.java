package com.urlshortener.safety;

public interface UrlSafetyService {

    /**
     * Soft classification: never blocks shortening.
     * When the AI provider is unavailable, returns UNKNOWN with providerAvailable=false.
     */
    SafetyClassification classify(String originalUrl);
}
