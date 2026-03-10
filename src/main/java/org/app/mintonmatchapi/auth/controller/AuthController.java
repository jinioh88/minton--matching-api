package org.app.mintonmatchapi.auth.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.dto.AuthResponse;
import org.app.mintonmatchapi.auth.dto.OAuthLoginRequest;
import org.app.mintonmatchapi.auth.service.OAuth2Service;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuth2Service oauth2Service;

    public AuthController(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @PostMapping("/oauth/login")
    public ApiResponse<AuthResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        AuthResponse response = oauth2Service.login(request);
        return ApiResponse.success(response);
    }
}
