package com.urlshortener.web;

import com.urlshortener.domain.SafetyStatus;
import com.urlshortener.domain.UrlStatus;
import com.urlshortener.service.UrlService;
import com.urlshortener.tour.TourService;
import com.urlshortener.web.dto.ApiResponse;
import com.urlshortener.web.dto.CreateShortUrlRequest;
import com.urlshortener.web.dto.PageMeta;
import com.urlshortener.web.dto.ShortUrlResponse;
import com.urlshortener.web.dto.ShortUrlResult;
import com.urlshortener.web.dto.TourResponse;
import com.urlshortener.web.dto.UpdateShortUrlRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    private final UrlService urlService;
    private final TourService tourService;

    public UrlController(UrlService urlService, TourService tourService) {
        this.urlService = urlService;
        this.tourService = tourService;
    }

    @PostMapping("/urls")
    public ResponseEntity<ApiResponse<ShortUrlResponse>> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResult result = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result.url(), result.warnings()));
    }

    @GetMapping("/urls")
    public ApiResponse<List<ShortUrlResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UrlStatus status,
            @RequestParam(required = false) SafetyStatus safetyStatus,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShortUrlResponse> page = urlService.listUrls(q, status, safetyStatus, pageable);
        PageMeta meta = urlService.toPageMeta(page, page.getPageable());
        return ApiResponse.ok(page.getContent(), meta);
    }

    @GetMapping("/urls/{shortCode}")
    public ApiResponse<ShortUrlResponse> getMetadata(@PathVariable String shortCode) {
        return ApiResponse.ok(urlService.getByShortCode(shortCode));
    }

    @PatchMapping("/urls/{shortCode}")
    public ApiResponse<ShortUrlResponse> update(
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateShortUrlRequest request) {
        ShortUrlResult result = urlService.updateShortUrl(shortCode, request);
        return ApiResponse.ok(result.url(), result.warnings());
    }

    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String shortCode) {
        urlService.deleteShortUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/tour")
    public ApiResponse<TourResponse> tour() {
        return ApiResponse.ok(tourService.getTour());
    }
}
