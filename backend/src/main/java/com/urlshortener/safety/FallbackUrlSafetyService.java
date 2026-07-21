package com.urlshortener.safety;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Used when Gemini ChatModel is not configured (local/dev without API key).
 */
@Service
@Profile("!test")
public class FallbackUrlSafetyService implements UrlSafetyService {

    @Override
    public SafetyClassification classify(String originalUrl) {
        return SafetyClassification.unavailable();
    }
}
