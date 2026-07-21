package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.exception.ShortCodeGenerationException;
import com.urlshortener.repository.ShortUrlRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final ShortUrlRepository shortUrlRepository;
    private final AppProperties appProperties;

    public ShortCodeGenerator(ShortUrlRepository shortUrlRepository, AppProperties appProperties) {
        this.shortUrlRepository = shortUrlRepository;
        this.appProperties = appProperties;
    }

    public String generateUniqueCode() {
        int length = appProperties.getShortCode().getLength();
        int maxAttempts = appProperties.getShortCode().getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String candidate = randomCode(length);
            if (!shortUrlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }

        throw new ShortCodeGenerationException(
                "Unable to generate a unique short code after " + maxAttempts + " attempts");
    }

    private String randomCode(int length) {
        char[] code = new char[length];
        for (int i = 0; i < length; i++) {
            code[i] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
