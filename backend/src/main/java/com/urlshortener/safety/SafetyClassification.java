package com.urlshortener.safety;

import com.urlshortener.domain.SafetyStatus;

public record SafetyClassification(SafetyStatus status, boolean providerAvailable) {

    public static SafetyClassification unavailable() {
        return new SafetyClassification(SafetyStatus.UNKNOWN, false);
    }
}
