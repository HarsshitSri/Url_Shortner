package com.urlshortener.auth;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthClientConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OAuthProperties oauthProperties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (oauthProperties.isGoogleEnabled()) {
            registrations.add(googleClientRegistration(oauthProperties));
        }
        if (oauthProperties.isGithubEnabled()) {
            registrations.add(githubClientRegistration(oauthProperties));
        }
        if (registrations.isEmpty()) {
            // Placeholder so the bean always exists; SecurityConfig skips oauth2Login when none enabled.
            return new InMemoryClientRegistrationRepository(List.of(disabledRegistration()));
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static ClientRegistration googleClientRegistration(OAuthProperties props) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(props.getGoogle().getClientId())
                .clientSecret(props.getGoogle().getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    private static ClientRegistration githubClientRegistration(OAuthProperties props) {
        return ClientRegistration.withRegistrationId("github")
                .clientId(props.getGithub().getClientId())
                .clientSecret(props.getGithub().getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
    }

    private static ClientRegistration disabledRegistration() {
        return ClientRegistration.withRegistrationId("disabled")
                .clientId("disabled")
                .clientSecret("disabled")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("sub")
                .clientName("Disabled")
                .build();
    }
}
