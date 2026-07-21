package com.urlshortener.auth;

import com.urlshortener.exception.InvalidCredentialsException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OAuthAccountService {

    private final UserRepository userRepository;

    public OAuthAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreate(AuthProvider provider, String providerSubject, String email) {
        if (provider == null || provider == AuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth provider is required");
        }
        if (!StringUtils.hasText(providerSubject)) {
            throw new InvalidCredentialsException("OAuth provider subject is missing");
        }
        if (!StringUtils.hasText(email)) {
            throw new InvalidCredentialsException(
                    "No email returned by " + provider.name().toLowerCase()
                            + ". Make sure your account email is visible to the app.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String subject = providerSubject.trim();

        return userRepository.findByAuthProviderAndProviderSubject(provider, subject)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseGet(() -> userRepository.save(
                        User.oauth(UUID.randomUUID(), normalizedEmail, provider, subject)));
    }
}
