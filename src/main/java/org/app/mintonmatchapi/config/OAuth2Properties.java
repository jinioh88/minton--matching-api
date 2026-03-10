package org.app.mintonmatchapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    /**
     * OAuth2 Redirect URI 허용 목록.
     * Flutter 앱, React 웹 등 클라이언트별 redirect URI를 등록.
     * Request의 redirectUri가 이 목록에 있는지 검증 후 토큰 교환 진행.
     */
    private List<String> allowedRedirectUris = new ArrayList<>();

    public List<String> getAllowedRedirectUris() {
        return allowedRedirectUris;
    }

    public void setAllowedRedirectUris(List<String> allowedRedirectUris) {
        this.allowedRedirectUris = allowedRedirectUris;
    }

    public boolean isAllowedRedirectUri(String redirectUri) {
        return redirectUri != null && allowedRedirectUris.contains(redirectUri);
    }
}
