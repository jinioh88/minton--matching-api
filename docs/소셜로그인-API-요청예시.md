# 소셜 로그인 API 요청 예시

## 엔드포인트
- `POST /api/auth/oauth/login`

## 요청 규칙
- 공통 필수: `provider`
- 앱 네이티브 SDK 로그인: `socialAccessToken` 전달
- 웹 Authorization Code 로그인: `authorizationCode` + `redirectUri` 전달
- 유효성 규칙: `socialAccessToken` 또는 `authorizationCode+redirectUri` 조합 중 하나는 반드시 필요

## 1) 앱 네이티브 SDK 로그인 예시

### Kakao
```json
{
  "provider": "KAKAO",
  "socialAccessToken": "kakao_access_token_from_sdk"
}
```

### Google
```json
{
  "provider": "GOOGLE",
  "socialAccessToken": "google_access_token_from_sdk"
}
```

### Naver
```json
{
  "provider": "NAVER",
  "socialAccessToken": "naver_access_token_from_sdk"
}
```

## 2) 웹 Authorization Code 로그인 예시
```json
{
  "provider": "KAKAO",
  "authorizationCode": "authorization_code_from_provider",
  "redirectUri": "http://localhost:3000/oauth/callback"
}
```

## 성공 응답 형식 (앱/웹 동일)
```json
{
  "success": true,
  "data": {
    "accessToken": "our_service_jwt",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "minton",
      "profileImg": "https://...",
      "profileComplete": true,
      "joinedAt": "2026-04-14T09:00:00"
    }
  }
}
```

## 참고
- 앱 소셜 토큰 검증 대상 provider: `KAKAO`, `GOOGLE`, `NAVER`
- `APPLE`은 현재 authorization code 기반 로그인만 지원

## 에러 응답 구분 가이드
- `OAUTH_SOCIAL_TOKEN_EXPIRED` (HTTP 401): 앱이 보낸 소셜 `socialAccessToken` 만료/무효
- `OAUTH_AUTHORIZATION_CODE_INVALID` (HTTP 400): 웹 인가 코드(`authorizationCode`) 오류
- `OAUTH_INVALID` (HTTP 400): 그 외 OAuth 검증 실패

예시:
```json
{
  "success": false,
  "message": "GOOGLE 소셜 토큰 검증 실패: invalid_token (Token expired)",
  "code": "OAUTH_SOCIAL_TOKEN_EXPIRED"
}
```

## Google 토큰 타입 가이드 (앱 연동 필수)
- 현재 백엔드는 Google `userinfo` API 호출 방식이므로 `socialAccessToken`에는 **Access Token**을 전달해야 한다.
- 즉, Google 네이티브 SDK에서 `IdToken`만 받은 경우 현재 API로는 로그인할 수 없다.
- `IdToken` 기반 로그인이 필요하면 서버에서 Google 공개키(JWK)로 서명 검증하는 별도 경로를 추가해야 한다.
