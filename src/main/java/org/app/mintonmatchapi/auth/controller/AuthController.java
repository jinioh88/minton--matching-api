package org.app.mintonmatchapi.auth.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.dto.AuthResponse;
import org.app.mintonmatchapi.auth.dto.OAuthLoginRequest;
import org.app.mintonmatchapi.auth.dto.OAuthStateResponse;
import org.app.mintonmatchapi.auth.service.OAuth2Service;
import org.app.mintonmatchapi.auth.service.OAuthStateService;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.user.entity.Provider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuth2Service oauth2Service;
    private final OAuthStateService oauthStateService;

    public AuthController(OAuth2Service oauth2Service, OAuthStateService oauthStateService) {
        this.oauth2Service = oauth2Service;
        this.oauthStateService = oauthStateService;
    }

    @PostMapping("/oauth/login")
    public ApiResponse<AuthResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        AuthResponse response = oauth2Service.login(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/oauth/naver/state")
    public ApiResponse<OAuthStateResponse> issueNaverState(@RequestParam Provider provider) {
        if (provider != Provider.NAVER) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "NAVER provider만 state 발급이 가능합니다.");
        }

        String state = oauthStateService.issueState(provider);
        OAuthStateResponse response = OAuthStateResponse.builder()
                .state(state)
                .expiresInSeconds(oauthStateService.getStateTtlSeconds())
                .build();
        return ApiResponse.success(response);
    }

    @GetMapping("/oauth/google/state")
    public ApiResponse<OAuthStateResponse> issueGoogleState(@RequestParam Provider provider) {
        if (provider != Provider.GOOGLE) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "GOOGLE provider만 state 발급이 가능합니다.");
        }

        String state = oauthStateService.issueState(provider);
        OAuthStateResponse response = OAuthStateResponse.builder()
                .state(state)
                .expiresInSeconds(oauthStateService.getStateTtlSeconds())
                .build();
        return ApiResponse.success(response);
    }
}
