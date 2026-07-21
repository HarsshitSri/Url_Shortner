package com.urlshortener.web;

import com.urlshortener.service.UrlService;
import com.urlshortener.tour.TourService;
import com.urlshortener.web.dto.CreateShortUrlRequest;
import com.urlshortener.web.dto.ShortUrlResponse;
import com.urlshortener.web.dto.TourResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.createShortUrl(request));
    }

    @GetMapping("/urls/{shortCode}")
    public ShortUrlResponse getMetadata(@PathVariable String shortCode) {
        return urlService.getByShortCode(shortCode);
    }

    @GetMapping("/tour")
    public TourResponse tour() {
        return tourService.getTour();
    }
}
