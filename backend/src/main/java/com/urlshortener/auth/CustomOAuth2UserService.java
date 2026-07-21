package com.urlshortener.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public CustomOAuth2UserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"github".equals(registrationId)) {
            return user;
        }

        String email = user.getAttribute("email");
        if (StringUtils.hasText(email)) {
            return user;
        }

        String fetched = fetchGitHubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
        if (!StringUtils.hasText(fetched)) {
            return user;
        }

        Map<String, Object> attributes = objectMapper.convertValue(user.getAttributes(), new TypeReference<>() {});
        attributes.put("email", fetched);
        return new DefaultOAuth2User(user.getAuthorities(), attributes, "id");
    }

    private String fetchGitHubPrimaryEmail(String accessToken) {
        try {
            RequestEntity<Void> request = RequestEntity
                    .get("https://api.github.com/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .build();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    request,
                    new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> emails = response.getBody();
            if (emails == null || emails.isEmpty()) {
                return null;
            }
            for (Map<String, Object> entry : emails) {
                if (Boolean.TRUE.equals(entry.get("primary")) && Boolean.TRUE.equals(entry.get("verified"))) {
                    return stringValue(entry.get("email"));
                }
            }
            for (Map<String, Object> entry : emails) {
                if (Boolean.TRUE.equals(entry.get("verified"))) {
                    return stringValue(entry.get("email"));
                }
            }
            return stringValue(emails.getFirst().get("email"));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
