package com.urlshortener.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    private String frontendBaseUrl = "http://localhost:5500";
    private final Provider google = new Provider();
    private final Provider github = new Provider();

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Provider getGoogle() {
        return google;
    }

    public Provider getGithub() {
        return github;
    }

    public boolean isGoogleEnabled() {
        return StringUtils.hasText(google.getClientId()) && StringUtils.hasText(google.getClientSecret());
    }

    public boolean isGithubEnabled() {
        return StringUtils.hasText(github.getClientId()) && StringUtils.hasText(github.getClientSecret());
    }

    public boolean anyEnabled() {
        return isGoogleEnabled() || isGithubEnabled();
    }

    public String frontendLoginUrl() {
        return trimTrailingSlash(frontendBaseUrl) + "/login.html";
    }

    public String frontendCallbackUrl() {
        return trimTrailingSlash(frontendBaseUrl) + "/oauth-callback.html";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5500";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
