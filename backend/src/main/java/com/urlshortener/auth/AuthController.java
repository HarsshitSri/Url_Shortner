package com.urlshortener.auth;

import com.urlshortener.auth.dto.AuthProvidersResponse;
import com.urlshortener.auth.dto.AuthResponse;
import com.urlshortener.auth.dto.LoginRequest;
import com.urlshortener.auth.dto.RegisterRequest;
import com.urlshortener.auth.dto.UserResponse;
import com.urlshortener.web.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthProperties oauthProperties;

    public AuthController(AuthService authService, OAuthProperties oauthProperties) {
        this.authService = authService;
        this.oauthProperties = oauthProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.currentUserId()));
    }

    @GetMapping("/providers")
    public ApiResponse<AuthProvidersResponse> providers() {
        List<String> providers = new ArrayList<>();
        if (oauthProperties.isGoogleEnabled()) {
            providers.add("google");
        }
        if (oauthProperties.isGithubEnabled()) {
            providers.add("github");
        }
        return ApiResponse.ok(new AuthProvidersResponse(providers));
    }
}
