package com.urlshortener.repository;

import com.urlshortener.domain.SafetyStatus;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.domain.UrlStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ShortUrlSpecifications {

    private ShortUrlSpecifications() {
    }

    public static Specification<ShortUrl> withFilters(String q, UrlStatus status, SafetyStatus safetyStatus) {
        return Specification.where(matchesQuery(q))
                .and(hasStatus(status))
                .and(hasSafetyStatus(safetyStatus));
    }

    private static Specification<ShortUrl> matchesQuery(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return cb.conjunction();
            }
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("shortCode")), pattern),
                    cb.like(cb.lower(root.get("originalUrl")), pattern));
        };
    }

    private static Specification<ShortUrl> hasStatus(UrlStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<ShortUrl> hasSafetyStatus(SafetyStatus safetyStatus) {
        return (root, query, cb) -> safetyStatus == null
                ? cb.conjunction()
                : cb.equal(root.get("safetyStatus"), safetyStatus);
    }
}
