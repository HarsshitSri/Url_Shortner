package com.urlshortener.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccountService;
    private final JwtService jwtService;
    private final OAuthProperties oauthProperties;

    public OAuth2LoginSuccessHandler(
            OAuthAccountService oauthAccountService,
            JwtService jwtService,
            OAuthProperties oauthProperties) {
        this.oauthAccountService = oauthAccountService;
        this.jwtService = jwtService;
        this.oauthProperties = oauthProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            redirectWithError(response, "unsupported_authentication");
            return;
        }

        try {
            AuthProvider provider = resolveProvider(token.getAuthorizedClientRegistrationId());
            OAuth2User oauthUser = token.getPrincipal();
            String subject = resolveSubject(provider, oauthUser);
            String email = oauthUser.getAttribute("email");

            User user = oauthAccountService.findOrCreate(provider, subject, email);
            String jwt = jwtService.generateToken(user);

            String redirect = UriComponentsBuilder
                    .fromUriString(oauthProperties.frontendCallbackUrl())
                    .queryParam("token", jwt)
                    .queryParam("email", user.getEmail())
                    .queryParam("id", user.getId().toString())
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(redirect);
        } catch (Exception ex) {
            redirectWithError(response, ex.getMessage() != null ? ex.getMessage() : "oauth_failed");
        }
    }

    private static AuthProvider resolveProvider(String registrationId) {
        return switch (registrationId) {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> throw new IllegalArgumentException("Unknown OAuth provider: " + registrationId);
        };
    }

    private static String resolveSubject(AuthProvider provider, OAuth2User oauthUser) {
        Object subject = switch (provider) {
            case GOOGLE -> oauthUser.getAttribute("sub");
            case GITHUB -> oauthUser.getAttribute("id");
            default -> null;
        };
        return subject == null ? null : String.valueOf(subject);
    }

    private void redirectWithError(HttpServletResponse response, String message) throws IOException {
        String redirect = UriComponentsBuilder
                .fromUriString(oauthProperties.frontendLoginUrl())
                .queryParam("error", message)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirect);
    }
}
