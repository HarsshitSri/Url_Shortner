package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.domain.UrlStatus;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.safety.GeminiUrlSafetyService;
import com.urlshortener.safety.SafetyClassification;
import com.urlshortener.safety.UrlSafetyService;
import com.urlshortener.web.dto.CreateShortUrlRequest;
import com.urlshortener.web.dto.ShortUrlResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlSafetyService urlSafetyService;
    private final AppProperties appProperties;

    public UrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            UrlSafetyService urlSafetyService,
            AppProperties appProperties) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlSafetyService = urlSafetyService;
        this.appProperties = appProperties;
    }

    @Transactional
    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        String originalUrl = request.originalUrl().trim();
        List<String> warnings = new ArrayList<>();

        if (originalUrl.toLowerCase().startsWith("http://")) {
            warnings.add(GeminiUrlSafetyService.HTTP_WARNING);
        }

        SafetyClassification classification = urlSafetyService.classify(originalUrl);
        if (!classification.providerAvailable()) {
            warnings.add(GeminiUrlSafetyService.AI_DOWN_WARNING);
        }

        String shortCode = shortCodeGenerator.generateUniqueCode();
        ShortUrl entity = new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                originalUrl,
                UrlStatus.ACTIVE,
                classification.status());

        ShortUrl saved = shortUrlRepository.save(entity);
        return toResponse(saved, warnings);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getByShortCode(String shortCode) {
        ShortUrl shortUrl = findActiveOrThrow(shortCode);
        return toResponse(shortUrl, List.of());
    }

    @Transactional(readOnly = true)
    public String resolveRedirectUrl(String shortCode) {
        return findActiveOrThrow(shortCode).getOriginalUrl();
    }

    private ShortUrl findActiveOrThrow(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (shortUrl.getStatus() != UrlStatus.ACTIVE) {
            throw new ShortUrlNotFoundException(shortCode);
        }
        return shortUrl;
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl, List<String> warnings) {
        String baseUrl = trimTrailingSlash(appProperties.getBaseUrl());
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                baseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getSafetyStatus(),
                shortUrl.getCreatedAt(),
                List.copyOf(warnings));
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
