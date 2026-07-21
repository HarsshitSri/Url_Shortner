package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.SafetyStatus;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.domain.UrlStatus;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.safety.GeminiUrlSafetyService;
import com.urlshortener.safety.SafetyClassification;
import com.urlshortener.safety.UrlSafetyService;
import com.urlshortener.web.dto.CreateShortUrlRequest;
import com.urlshortener.web.dto.ShortUrlResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlSafetyService urlSafetyService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:8080");
        urlService = new UrlService(shortUrlRepository, shortCodeGenerator, urlSafetyService, props);
    }

    @Test
    void createShortUrl_persistsAndReturnsWarningsForHttpAndAiDown() {
        when(urlSafetyService.classify(anyString())).thenReturn(SafetyClassification.unavailable());
        when(shortCodeGenerator.generateUniqueCode()).thenReturn("abc123");
        when(shortUrlRepository.save(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = urlService.createShortUrl(
                new CreateShortUrlRequest("http://example.com/path"));

        assertThat(response.shortCode()).isEqualTo("abc123");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/abc123");
        assertThat(response.safetyStatus()).isEqualTo(SafetyStatus.UNKNOWN);
        assertThat(response.warnings())
                .contains(GeminiUrlSafetyService.HTTP_WARNING, GeminiUrlSafetyService.AI_DOWN_WARNING);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        org.mockito.Mockito.verify(shortUrlRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UrlStatus.ACTIVE);
    }

    @Test
    void createShortUrl_doesNotBlockUnsafeClassification() {
        when(urlSafetyService.classify(anyString()))
                .thenReturn(new SafetyClassification(SafetyStatus.UNSAFE, true));
        when(shortCodeGenerator.generateUniqueCode()).thenReturn("unsafe1");
        when(shortUrlRepository.save(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = urlService.createShortUrl(
                new CreateShortUrlRequest("https://example.com/malware"));

        assertThat(response.safetyStatus()).isEqualTo(SafetyStatus.UNSAFE);
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    void getByShortCode_throwsWhenMissing() {
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getByShortCode("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void resolveRedirectUrl_returnsOriginal() {
        ShortUrl entity = new ShortUrl(
                UUID.randomUUID(),
                "abc123",
                "https://example.com",
                UrlStatus.ACTIVE,
                SafetyStatus.SAFE);
        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));

        assertThat(urlService.resolveRedirectUrl("abc123")).isEqualTo("https://example.com");
    }
}
