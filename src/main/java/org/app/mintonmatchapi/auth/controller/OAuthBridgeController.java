package org.app.mintonmatchapi.auth.controller;

import org.app.mintonmatchapi.auth.service.OAuthStateService;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.config.OAuth2Properties;
import org.app.mintonmatchapi.user.entity.Provider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
public class OAuthBridgeController {

    private final OAuth2Properties oauth2Properties;
    private final OAuthStateService oauthStateService;

    public OAuthBridgeController(OAuth2Properties oauth2Properties, OAuthStateService oauthStateService) {
        this.oauth2Properties = oauth2Properties;
        this.oauthStateService = oauthStateService;
    }

    @GetMapping("/oauth/naver/callback")
    public ResponseEntity<Void> bridgeNaverCallback(@RequestParam String code,
                                                    @RequestParam(required = false) String state) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "네이버 인가 코드(code)가 없습니다.");
        }
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "네이버 state가 없습니다.");
        }
        oauthStateService.validateState(state, Provider.NAVER);

        URI appCallbackUri = UriComponentsBuilder
                .fromUriString(oauth2Properties.getMobileCallbackUri())
                .queryParam("code", code)
                .queryParam("state", state)
                .build(true)
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, appCallbackUri.toString())
                .build();
    }

    @GetMapping("/oauth/google/callback")
    public ResponseEntity<Void> bridgeGoogleCallback(@RequestParam String code,
                                                     @RequestParam(required = false) String state) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "구글 인가 코드(code)가 없습니다.");
        }
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "구글 state가 없습니다.");
        }
        oauthStateService.validateState(state, Provider.GOOGLE);

        URI appCallbackUri = UriComponentsBuilder
                .fromUriString(oauth2Properties.getMobileCallbackUri())
                .queryParam("code", code)
                .queryParam("state", state)
                .build(true)
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, appCallbackUri.toString())
                .build();
    }
}
