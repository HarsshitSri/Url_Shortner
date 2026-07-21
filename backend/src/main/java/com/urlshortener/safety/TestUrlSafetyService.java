package com.urlshortener.safety;

import com.urlshortener.domain.SafetyStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class TestUrlSafetyService implements UrlSafetyService {

    @Override
    public SafetyClassification classify(String originalUrl) {
        String lower = originalUrl.toLowerCase();
        if (lower.contains("unavailable-ai")) {
            return SafetyClassification.unavailable();
        }
        if (lower.contains("malware") || lower.contains("phishing")) {
            return new SafetyClassification(SafetyStatus.UNSAFE, true);
        }
        if (lower.contains("suspicious")) {
            return new SafetyClassification(SafetyStatus.SUSPICIOUS, true);
        }
        return new SafetyClassification(SafetyStatus.SAFE, true);
    }
}
