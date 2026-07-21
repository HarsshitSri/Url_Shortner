package com.urlshortener.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthProperties oauthProperties;

    public OAuth2LoginFailureHandler(OAuthProperties oauthProperties) {
        this.oauthProperties = oauthProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String message = exception.getMessage() != null ? exception.getMessage() : "oauth_failed";
        String redirect = UriComponentsBuilder
                .fromUriString(oauthProperties.frontendLoginUrl())
                .queryParam("error", message)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirect);
    }
}
