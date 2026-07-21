package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.SafetyStatus;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.domain.UrlStatus;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.repository.ShortUrlSpecifications;
import com.urlshortener.safety.GeminiUrlSafetyService;
import com.urlshortener.safety.SafetyClassification;
import com.urlshortener.safety.UrlSafetyService;
import com.urlshortener.web.dto.CreateShortUrlRequest;
import com.urlshortener.web.dto.PageMeta;
import com.urlshortener.web.dto.ShortUrlResponse;
import com.urlshortener.web.dto.ShortUrlResult;
import com.urlshortener.web.dto.UpdateShortUrlRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UrlService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "shortCode", "status", "safetyStatus");
    private static final int MAX_PAGE_SIZE = 50;

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
    public ShortUrlResult createShortUrl(CreateShortUrlRequest request) {
        String originalUrl = request.originalUrl().trim();
        List<String> warnings = new ArrayList<>();
        SafetyStatus safetyStatus = classifyWithWarnings(originalUrl, warnings);

        String shortCode = shortCodeGenerator.generateUniqueCode();
        ShortUrl entity = new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                originalUrl,
                UrlStatus.ACTIVE,
                safetyStatus);

        ShortUrl saved = shortUrlRepository.save(entity);
        return new ShortUrlResult(toResponse(saved), warnings);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getByShortCode(String shortCode) {
        return toResponse(findActiveOrThrow(shortCode));
    }

    @Transactional(readOnly = true)
    public String resolveRedirectUrl(String shortCode) {
        return findActiveOrThrow(shortCode).getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> listUrls(String q, UrlStatus status, SafetyStatus safetyStatus, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return shortUrlRepository
                .findAll(ShortUrlSpecifications.withFilters(q, status, safetyStatus), safePageable)
                .map(this::toResponse);
    }

    public PageMeta toPageMeta(Page<?> page, Pageable pageable) {
        String sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .findFirst()
                .orElse("createdAt,desc");
        return new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sort);
    }

    @Transactional
    public ShortUrlResult updateShortUrl(String shortCode, UpdateShortUrlRequest request) {
        if (request.originalUrl() == null && request.status() == null) {
            throw new IllegalArgumentException("Provide originalUrl and/or status to update");
        }

        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        List<String> warnings = new ArrayList<>();

        if (request.status() != null) {
            if (request.status() != UrlStatus.ACTIVE && request.status() != UrlStatus.DISABLED) {
                throw new IllegalArgumentException("status must be ACTIVE or DISABLED");
            }
            entity.setStatus(request.status());
        }

        if (StringUtils.hasText(request.originalUrl())) {
            String originalUrl = request.originalUrl().trim();
            SafetyStatus safetyStatus = classifyWithWarnings(originalUrl, warnings);
            entity.setOriginalUrl(originalUrl);
            entity.setSafetyStatus(safetyStatus);
        }

        ShortUrl saved = shortUrlRepository.save(entity);
        return new ShortUrlResult(toResponse(saved), warnings);
    }

    @Transactional
    public void deleteShortUrl(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        shortUrlRepository.delete(entity);
    }

    private SafetyStatus classifyWithWarnings(String originalUrl, List<String> warnings) {
        if (originalUrl.toLowerCase().startsWith("http://")) {
            warnings.add(GeminiUrlSafetyService.HTTP_WARNING);
        }
        SafetyClassification classification = urlSafetyService.classify(originalUrl);
        if (!classification.providerAvailable()) {
            warnings.add(GeminiUrlSafetyService.AI_DOWN_WARNING);
        }
        return classification.status();
    }

    private ShortUrl findActiveOrThrow(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (shortUrl.getStatus() != UrlStatus.ACTIVE) {
            throw new ShortUrlNotFoundException(shortCode);
        }
        return shortUrl;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() < 1 ? 10 : Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            List<Sort.Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                    throw new IllegalArgumentException(
                            "Unsupported sort field: " + order.getProperty()
                                    + ". Allowed: " + ALLOWED_SORT_FIELDS);
                }
                orders.add(order);
            }
            sort = Sort.by(orders);
        }
        return PageRequest.of(page, size, sort);
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        String baseUrl = trimTrailingSlash(appProperties.getBaseUrl());
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                baseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getSafetyStatus(),
                shortUrl.getOwnerId(),
                shortUrl.getCreatedAt());
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
