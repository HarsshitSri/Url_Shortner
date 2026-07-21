package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.urlshortener.auth.UserPrincipal;
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
import com.urlshortener.web.dto.ShortUrlResult;
import com.urlshortener.web.dto.UpdateShortUrlRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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

        UserPrincipal principal = new UserPrincipal(OWNER_ID, "owner@example.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShortUrl_setsOwnerAndReturnsWarnings() {
        when(urlSafetyService.classify(anyString())).thenReturn(SafetyClassification.unavailable());
        when(shortCodeGenerator.generateUniqueCode()).thenReturn("abc123");
        when(shortUrlRepository.save(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResult result = urlService.createShortUrl(
                new CreateShortUrlRequest("http://example.com/path"));

        assertThat(result.url().shortCode()).isEqualTo("abc123");
        assertThat(result.url().ownerId()).isEqualTo(OWNER_ID);
        assertThat(result.warnings())
                .contains(GeminiUrlSafetyService.HTTP_WARNING, GeminiUrlSafetyService.AI_DOWN_WARNING);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        org.mockito.Mockito.verify(shortUrlRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    void updateShortUrl_requiresOwnership() {
        when(shortUrlRepository.findByShortCodeAndOwnerId("abc123", OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.updateShortUrl(
                        "abc123",
                        new UpdateShortUrlRequest(null, UrlStatus.DISABLED)))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void updateShortUrl_canDisableOwnedLink() {
        ShortUrl entity = new ShortUrl(
                UUID.randomUUID(),
                "abc123",
                "https://example.com",
                UrlStatus.ACTIVE,
                SafetyStatus.SAFE,
                OWNER_ID);
        when(shortUrlRepository.findByShortCodeAndOwnerId("abc123", OWNER_ID))
                .thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResult result = urlService.updateShortUrl(
                "abc123",
                new UpdateShortUrlRequest(null, UrlStatus.DISABLED));

        assertThat(result.url().status()).isEqualTo(UrlStatus.DISABLED);
    }

    @Test
    void resolveRedirectUrl_returnsOriginalWithoutOwnershipCheck() {
        ShortUrl entity = new ShortUrl(
                UUID.randomUUID(),
                "abc123",
                "https://example.com",
                UrlStatus.ACTIVE,
                SafetyStatus.SAFE,
                OWNER_ID);
        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(entity));

        assertThat(urlService.resolveRedirectUrl("abc123")).isEqualTo("https://example.com");
    }
}
