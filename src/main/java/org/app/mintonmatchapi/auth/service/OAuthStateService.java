package org.app.mintonmatchapi.auth.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.config.OAuth2Properties;
import org.app.mintonmatchapi.user.entity.Provider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class OAuthStateService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OAuth2Properties oauth2Properties;

    public OAuthStateService(OAuth2Properties oauth2Properties) {
        this.oauth2Properties = oauth2Properties;
    }

    public String issueState(Provider provider) {
        validateSecretConfigured();

        long issuedAtEpochSeconds = Instant.now().getEpochSecond();
        long expiresAtEpochSeconds = issuedAtEpochSeconds + oauth2Properties.getStateTtlSeconds();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = provider.name() + "|" + expiresAtEpochSeconds + "|" + nonce;
        byte[] signature = hmacSha256(payload);

        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature);

        return encodedPayload + "." + encodedSignature;
    }

    public void validateState(String state, Provider expectedProvider) {
        validateSecretConfigured();

        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state가 없습니다.");
        }

        String[] parts = state.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state 형식이 올바르지 않습니다.");
        }

        String payload = decodeBase64Url(parts[0]);
        byte[] providedSignature = decodeBase64UrlBytes(parts[1]);
        byte[] expectedSignature = hmacSha256(payload);
        if (!MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state 서명이 유효하지 않습니다.");
        }

        String[] payloadParts = payload.split("\\|");
        if (payloadParts.length != 3) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state payload가 올바르지 않습니다.");
        }

        Provider provider;
        try {
            provider = Provider.valueOf(payloadParts[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state provider가 올바르지 않습니다.");
        }
        if (provider != expectedProvider) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state provider가 일치하지 않습니다.");
        }

        long expiresAtEpochSeconds;
        try {
            expiresAtEpochSeconds = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state 만료 정보가 올바르지 않습니다.");
        }
        if (Instant.now().getEpochSecond() > expiresAtEpochSeconds) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state가 만료되었습니다.");
        }
    }

    public long getStateTtlSeconds() {
        return oauth2Properties.getStateTtlSeconds();
    }

    private byte[] hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    oauth2Properties.getStateSigningSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "OAuth state 서명 생성에 실패했습니다.");
        }
    }

    private String decodeBase64Url(String input) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state payload 디코딩에 실패했습니다.");
        }
    }

    private byte[] decodeBase64UrlBytes(String input) {
        try {
            return Base64.getUrlDecoder().decode(input);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "OAuth state 서명 디코딩에 실패했습니다.");
        }
    }

    private void validateSecretConfigured() {
        if (!StringUtils.hasText(oauth2Properties.getStateSigningSecret())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "OAuth state signing secret이 설정되지 않았습니다.");
        }
    }
}
