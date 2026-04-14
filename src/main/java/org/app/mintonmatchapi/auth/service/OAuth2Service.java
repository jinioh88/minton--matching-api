package org.app.mintonmatchapi.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.auth.dto.AuthResponse;
import org.app.mintonmatchapi.auth.dto.OAuthLoginRequest;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.config.OAuth2Properties;
import org.app.mintonmatchapi.user.entity.Level;
import org.app.mintonmatchapi.user.entity.Provider;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OAuth2Service {

    private final OAuth2Properties oauth2Properties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestClient restClient = RestClient.create();
    private static final String KAKAO_ME_API = "https://kapi.kakao.com/v2/user/me";
    private static final String NAVER_ME_API = "https://openapi.naver.com/v1/nid/me";
    private static final String GOOGLE_ME_API = "https://www.googleapis.com/oauth2/v3/userinfo";

    public OAuth2Service(OAuth2Properties oauth2Properties,
                        ClientRegistrationRepository clientRegistrationRepository,
                        UserRepository userRepository,
                        JwtService jwtService) {
        this.oauth2Properties = oauth2Properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(OAuthLoginRequest request) {
        Map<String, Object> userAttributes;
        if (StringUtils.hasText(request.getSocialAccessToken())) {
            userAttributes = fetchUserInfoWithSocialAccessToken(request.getProvider(), request.getSocialAccessToken());
        } else {
            userAttributes = fetchUserInfoWithAuthorizationCode(request);
        }

        User user = findOrCreateUser(request.getProvider(), userAttributes);
        return buildAuthResponse(user);
    }

    private Map<String, Object> fetchUserInfoWithAuthorizationCode(OAuthLoginRequest request) {
        if (!oauth2Properties.isAllowedRedirectUri(request.getRedirectUri())) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "허용되지 않은 redirect URI입니다.");
        }

        ClientRegistration registration = getClientRegistration(request.getProvider());
        if (registration == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "지원하지 않는 OAuth 제공자입니다.");
        }

        TokenExchangeResult tokenResult = exchangeCodeForToken(registration, request.getAuthorizationCode(), request.getRedirectUri());
        return fetchUserInfo(request.getProvider(), registration, tokenResult);
    }

    private Map<String, Object> fetchUserInfoWithSocialAccessToken(Provider provider, String socialAccessToken) {
        return switch (provider) {
            case KAKAO -> requestUserInfoWithBearerToken(provider, KAKAO_ME_API, socialAccessToken);
            case NAVER -> requestUserInfoWithBearerToken(provider, NAVER_ME_API, socialAccessToken);
            case GOOGLE -> requestUserInfoWithBearerToken(provider, GOOGLE_ME_API, socialAccessToken);
            case APPLE -> throw new BusinessException(ErrorCode.OAUTH_INVALID, "APPLE은 authorization code 로그인만 지원합니다.");
        };
    }

    private Map<String, Object> requestUserInfoWithBearerToken(Provider provider, String userInfoUri, String socialAccessToken) {
        Map<String, Object> userInfo;
        try {
            userInfo = restClient.get()
                    .uri(userInfoUri)
                    .headers(h -> h.setBearerAuth(socialAccessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            log.warn("소셜 AccessToken 검증 실패 provider={}, status={}, body={}",
                    provider,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new BusinessException(
                        ErrorCode.OAUTH_SOCIAL_TOKEN_EXPIRED,
                        buildSocialTokenFailureMessage(provider, e)
                );
            }
            throw new BusinessException(ErrorCode.OAUTH_INVALID, buildSocialTokenFailureMessage(provider, e));
        }
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "소셜 사용자 정보 조회에 실패했습니다.");
        }
        return userInfo;
    }

    private AuthResponse buildAuthResponse(User user) {
        String jwt = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(jwt)
                .user(AuthResponse.UserResponse.from(user))
                .build();
    }

    private ClientRegistration getClientRegistration(Provider provider) {
        String registrationId = provider.name().toLowerCase();
        return clientRegistrationRepository.findByRegistrationId(registrationId);
    }

    private TokenExchangeResult exchangeCodeForToken(ClientRegistration registration, String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", registration.getClientId());
        if (StringUtils.hasText(registration.getClientSecret())) {
            params.add("client_secret", registration.getClientSecret());
        }

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(registration.getProviderDetails().getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            logOAuthTokenExchangeFailure(registration, redirectUri, e);
            throw resolveTokenExchangeException(registration, e);
        }

        if (response == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "토큰 교환에 실패했습니다.");
        }

        Object accessToken = response.get("access_token");
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "토큰 교환에 실패했습니다.");
        }
        return new TokenExchangeResult(accessToken.toString(), response);
    }

    private Map<String, Object> fetchUserInfo(Provider provider, ClientRegistration registration, TokenExchangeResult tokenResult) {
        if (provider == Provider.APPLE) {
            Object idToken = tokenResult.rawResponse().get("id_token");
            if (idToken == null) {
                throw new BusinessException(ErrorCode.OAUTH_INVALID, "Apple ID Token을 받지 못했습니다.");
            }
            return parseAppleIdToken(idToken.toString());
        }

        String userInfoUri = registration.getProviderDetails().getUserInfoEndpoint().getUri();
        if (userInfoUri == null || userInfoUri.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "사용자 정보 API를 지원하지 않는 제공자입니다.");
        }

        Map<String, Object> userInfo;
        try {
            userInfo = restClient.get()
                    .uri(userInfoUri)
                    .headers(h -> h.setBearerAuth(tokenResult.accessToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            log.warn("OAuth 사용자 정보 조회 실패 provider={}, status={}, body={}",
                    registration.getRegistrationId(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "사용자 정보 조회에 실패했습니다.");
        }
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "사용자 정보 조회에 실패했습니다.");
        }
        return userInfo;
    }

    private Map<String, Object> parseAppleIdToken(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "잘못된 Apple ID Token 형식입니다.");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = new ObjectMapper().readValue(payloadBytes, new TypeReference<>() {});
            Map<String, Object> result = new HashMap<>();
            result.put("sub", claims.get("sub"));
            result.put("email", claims.getOrDefault("email", ""));
            result.put("name", claims.getOrDefault("name", ""));
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "Apple ID Token 파싱에 실패했습니다.");
        }
    }

    private void logOAuthTokenExchangeFailure(ClientRegistration registration, String redirectUri, RestClientResponseException e) {
        String provider = registration.getRegistrationId();
        String responseBody = e.getResponseBodyAsString();
        String oauthError = extractOAuthError(responseBody);
        String errorDescription = extractOAuthErrorDescription(responseBody);

        if ("kakao".equalsIgnoreCase(provider)) {
            log.warn("Kakao 토큰 교환 실패 provider={}, status={}, redirectUri={}, tokenUri={}, error={}, errorDescription={}, body={}",
                    provider,
                    e.getStatusCode().value(),
                    redirectUri,
                    registration.getProviderDetails().getTokenUri(),
                    oauthError,
                    errorDescription,
                    responseBody);
            return;
        }

        log.warn("OAuth 토큰 교환 실패 provider={}, status={}, redirectUri={}, tokenUri={}, error={}, errorDescription={}, body={}",
                provider,
                e.getStatusCode().value(),
                redirectUri,
                registration.getProviderDetails().getTokenUri(),
                oauthError,
                errorDescription,
                responseBody);
    }

    private String buildOAuthTokenExchangeFailureMessage(ClientRegistration registration, RestClientResponseException e) {
        String provider = registration.getRegistrationId().toUpperCase();
        String oauthError = extractOAuthError(e.getResponseBodyAsString());
        String errorDescription = extractOAuthErrorDescription(e.getResponseBodyAsString());
        return String.format("%s OAuth 토큰 교환 실패: %s (%s)", provider, oauthError, errorDescription);
    }

    private String extractOAuthError(String responseBody) {
        Map<String, Object> errorBody = parseResponseBodyToMap(responseBody);
        Object error = errorBody.get("error");
        return error != null ? error.toString() : "unknown";
    }

    private String extractOAuthErrorDescription(String responseBody) {
        Map<String, Object> errorBody = parseResponseBodyToMap(responseBody);
        Object errorDescription = errorBody.get("error_description");
        return errorDescription != null ? errorDescription.toString() : "unknown";
    }

    private BusinessException resolveTokenExchangeException(ClientRegistration registration, RestClientResponseException e) {
        String oauthError = extractOAuthError(e.getResponseBodyAsString());
        HttpStatusCode statusCode = e.getStatusCode();
        if (statusCode.value() == 400 && ("invalid_grant".equalsIgnoreCase(oauthError) || "invalid_request".equalsIgnoreCase(oauthError))) {
            return new BusinessException(
                    ErrorCode.OAUTH_AUTHORIZATION_CODE_INVALID,
                    buildOAuthTokenExchangeFailureMessage(registration, e)
            );
        }
        return new BusinessException(ErrorCode.OAUTH_INVALID, buildOAuthTokenExchangeFailureMessage(registration, e));
    }

    private String buildSocialTokenFailureMessage(Provider provider, RestClientResponseException e) {
        String oauthError = extractOAuthError(e.getResponseBodyAsString());
        String errorDescription = extractOAuthErrorDescription(e.getResponseBodyAsString());
        return String.format("%s 소셜 토큰 검증 실패: %s (%s)", provider.name(), oauthError, errorDescription);
    }

    private Map<String, Object> parseResponseBodyToMap(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return Map.of();
        }

        try {
            return new ObjectMapper().readValue(responseBody, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private record TokenExchangeResult(String accessToken, Map<String, Object> rawResponse) {}

    private User findOrCreateUser(Provider provider, Map<String, Object> attributes) {
        String providerId = extractProviderId(provider, attributes);
        if (providerId == null || providerId.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID, "사용자 식별 정보를 가져올 수 없습니다.");
        }
        String email = extractEmail(provider, attributes);
        String nickname = extractNickname(provider, attributes);
        String profileImg = extractProfileImg(provider, attributes);

        User user = userRepository.findByProviderAndProviderId(provider, providerId);
        if (user != null) {
            return user;
        }

        return userRepository.save(User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImg(profileImg)
                .level(Level.BEGINNER)
                .build());
    }

    private String extractProviderId(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> String.valueOf(attributes.get("id"));
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield response != null ? String.valueOf(response.get("id")) : null;
            }
            case GOOGLE -> String.valueOf(attributes.get("sub"));
            case APPLE -> String.valueOf(attributes.get("sub"));
        };
    }

    private String extractEmail(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                yield kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield response != null ? (String) response.get("email") : null;
            }
            case GOOGLE -> (String) attributes.get("email");
            case APPLE -> {
                Object email = attributes.get("email");
                yield email != null ? email.toString() : null;
            }
        };
    }

    private String extractNickname(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount == null) yield null;
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                yield profile != null ? (String) profile.get("nickname") : null;
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield response != null ? (String) response.get("nickname") : null;
            }
            case GOOGLE -> (String) attributes.get("name");
            case APPLE -> {
                Object name = attributes.get("name");
                yield name != null ? name.toString() : null;
            }
        };
    }

    private String extractProfileImg(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount == null) yield null;
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                yield profile != null ? (String) profile.get("profile_image_url") : null;
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield response != null ? (String) response.get("profile_image") : null;
            }
            case GOOGLE -> (String) attributes.get("picture");
            case APPLE -> null;
        };
    }
}
