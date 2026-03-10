# OAuth2 개념 및 Spring Boot 연동 가이드

> **대상**: minton-match-api 프로젝트 (Spring Boot 4.x, Java 21)  
> **목적**: OAuth2 소셜 로그인 구현 전 필수 개념 이해

---

## 1. OAuth2 기본 개념

### 1.1 OAuth2란?

**OAuth 2.0**은 **인가(Authorization)** 프레임워크입니다. 사용자가 비밀번호를 제3자 앱에 노출하지 않고, 다른 서비스의 리소스에 대한 접근 권한을 부여할 수 있게 합니다.

- **인증(Authentication)**: "당신이 누구인가?" → 로그인
- **인가(Authorization)**: "이 앱이 당신의 데이터에 접근해도 되는가?" → 권한 부여

소셜 로그인(카카오, 네이버, 구글, 애플)은 OAuth2를 활용해 **"카카오/네이버 등이 사용자 인증을 대신 해주고, 우리 앱에 사용자 정보 접근 권한을 부여"**하는 방식입니다.

### 1.2 OAuth2의 4가지 역할

| 역할 | 설명 | 예시 |
|------|------|------|
| **Resource Owner** | 리소스 소유자(사용자) | 로그인하는 사용자 |
| **Client** | 리소스에 접근하려는 앱 | 우리 배드민턴 매칭 앱 |
| **Authorization Server** | 인증·토큰 발급 서버 | 카카오, 네이버, 구글, 애플 |
| **Resource Server** | 보호된 리소스가 있는 서버 | 카카오 사용자 정보 API 등 |

---

## 2. Authorization Code Flow (권한 부여 코드 플로우)

소셜 로그인에서 가장 많이 사용하는 방식입니다. **Authorization Code**를 먼저 받고, 이를 **Access Token**으로 교환하는 2단계 구조입니다.

### 2.1 전체 흐름 (5단계)

```
[사용자]          [우리 앱]              [카카오/네이버 등]
   |                   |                        |
   |  1. 로그인 클릭    |                        |
   |------------------>|                        |
   |                   |  2. 인증 페이지로 리다이렉트  |
   |                   |------------------------>|
   |                   |                        |
   |  3. 사용자가 카카오에서 로그인·동의              |
   |<------------------------------------------->|
   |                   |                        |
   |                   |  4. code를 담아 리다이렉트  |
   |<------------------|------------------------|
   |                   |                        |
   |  5. code를 백엔드에 전달                      |
   |------------------>|                        |
   |                   |  6. code → access_token 교환 |
   |                   |------------------------>|
   |                   |  7. access_token 수신    |
   |                   |<------------------------|
   |                   |                        |
   |                   |  8. 사용자 정보 API 호출   |
   |                   |------------------------>|
   |                   |  9. 사용자 정보 수신      |
   |                   |<------------------------|
   |                   |                        |
   |  10. JWT 발급·로그인 완료                      |
   |<------------------|                        |
```

### 2.2 핵심 용어

| 용어 | 설명 |
|------|------|
| **Authorization Code** | 인증 서버가 발급하는 **일회용·단기 유효** 코드. 액세스 토큰으로 교환됨. |
| **Access Token** | 리소스 서버 API(예: 사용자 정보) 호출 시 사용하는 토큰. |
| **Redirect URI** | 인증 후 사용자를 돌려보낼 URL. **사전 등록** 필수. |
| **Client ID / Client Secret** | 앱 식별자. Secret은 서버에만 보관. |
| **Scope** | 요청하는 권한 범위 (예: `profile`, `email`). |
| **State** | CSRF 방지를 위한 임의 값. 요청 시 보내고, 콜백 시 동일한지 검증. |

### 2.3 왜 Code를 바로 쓰지 않고 Token으로 교환할까?

- **Authorization Code**는 URL에 노출될 수 있어 탈취 위험이 있음.
- **Access Token**은 서버 간 직접 통신으로만 교환되므로 상대적으로 안전함.
- Code는 **일회용·단기**이므로, 탈취되어도 곧 무효화됨.

---

## 3. 우리 프로젝트의 OAuth2 플로우 (Flutter 앱 + 백엔드)

### 3.1 아키텍처

```
[Flutter 앱]                    [우리 백엔드]              [카카오/네이버/구글/애플]
     |                               |                            |
     | 1. 웹뷰/브라우저로 OAuth 인증   |                            |
     |------------------------------------------->|
     | 2. 사용자 로그인·동의                        |
     |<-------------------------------------------|
     | 3. redirect_uri?code=xxx&state=yyy          |
     |    (앱이 code 추출)                         |
     |                               |                            |
     | 4. POST /api/auth/oauth/login  |                            |
     |    { provider, authorizationCode, redirectUri }            |
     |------------------------------>|                            |
     |                               | 5. redirectUri 검증         |
     |                               | 6. code → token 교환        |
     |                               |--------------------------->|
     |                               | 7. access_token 수신       |
     |                               |<---------------------------|
     |                               | 8. 사용자 정보 API 호출     |
     |                               |--------------------------->|
     |                               | 9. 사용자 정보 수신         |
     |                               |<---------------------------|
     |                               | 10. User 조회/생성          |
     |                               | 11. JWT 발급               |
     | 12. { accessToken, user }     |                            |
     |<------------------------------|                            |
```

### 3.2 Spring 기본 OAuth2 Login과의 차이

| 구분 | Spring 기본 OAuth2 Login | 우리 프로젝트 방식 |
|------|--------------------------|-------------------|
| **진입점** | 브라우저가 `/oauth2/authorization/kakao` 등으로 접속 | Flutter 앱이 웹뷰로 OAuth 후 **code만** 백엔드에 전달 |
| **리다이렉트** | 카카오 → **우리 백엔드** URL로 리다이렉트 | 카카오 → **Flutter 앱**의 커스텀 스킴/딥링크 |
| **역할** | Spring이 인증 페이지 리다이렉트·콜백·세션 처리 | 백엔드는 **code → token → 사용자 정보 → JWT**만 처리 |
| **세션** | 서버 세션 기반 로그인 | **JWT 기반** 무상태 인증 |

우리 프로젝트는 **모바일 앱(Flutter)** 이 OAuth UI를 담당하고, 백엔드는 **REST API**로 code를 받아 토큰 교환·사용자 조회·JWT 발급만 수행합니다.

---

## 4. Spring Security OAuth2 관련 기능

### 4.1 Spring Boot 4.x / Spring Security 7.x 기준

Spring Security는 OAuth2를 위해 다음 컴포넌트를 제공합니다.

| 컴포넌트 | 용도 |
|----------|------|
| **OAuth2 Client** | Authorization Server(카카오 등)와 통신. code → token 교환, 사용자 정보 API 호출 |
| **OAuth2 Login** | 웹 앱용. `/oauth2/authorization/{registrationId}` 리다이렉트, 콜백 처리, 세션 로그인 |
| **OAuth2 Resource Server** | JWT 등으로 보호된 API 검증 (우리 JWT 검증에 활용 가능) |

### 4.2 우리가 사용할 부분

- **OAuth2 Client**: `ClientRegistration`, `OAuth2AccessTokenResponseClient` 등으로 **code → token** 교환
- **WebClient / RestClient**: 카카오/네이버/구글/애플 **REST API** 호출
- **직접 구현**: `ClientRegistration` 없이 각 제공자 API 스펙에 맞춰 HTTP 요청 (또는 Spring OAuth2 Client 설정 활용)

### 4.3 ClientRegistration이란?

Spring이 OAuth2 제공자(카카오, 네이버 등) 정보를 담는 설정 객체입니다.

```yaml
# application.yml 예시 (개념)
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: profile_nickname, account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
```

- `registration`: 클라이언트(우리 앱) 정보
- `provider`: Authorization Server(카카오 등)의 엔드포인트 URL

우리 프로젝트는 **Flutter가 code를 받아 전달**하므로, `redirect-uri`는 Flutter 앱의 스킴이 되고, 백엔드 설정에는 **허용 redirect URI 목록**만 두어 검증에 사용합니다.

---

## 5. 구현 시 필요한 작업 (Step 3~4 요약)

### 5.1 Step 3: OAuth2 설정 및 의존성

1. **의존성 추가**
   - `spring-boot-starter-oauth2-client`: 토큰 교환, 사용자 정보 API 호출
   - `spring-boot-starter-security`: 필터, 보안 설정
   - `jjwt` 또는 `spring-boot-starter-oauth2-resource-server`: JWT 발급/검증

2. **application.yml 설정**
   - 카카오, 네이버, 구글, 애플별 `client-id`, `client-secret`
   - `token-uri`, `user-info-uri` (제공자별 상이)
   - **허용 redirect URI 목록**: Flutter 앱 등에서 사용하는 URI만 허용

3. **토큰 교환 방식**
   - `RestClient` 또는 `WebClient`로 각 제공자 Token API에 POST
   - 또는 `OAuth2AuthorizationCodeGrantRequest` + `RestClientAuthorizationCodeTokenResponseClient` 활용

### 5.2 Step 4: OAuth2 로그인 API

1. **POST /api/auth/oauth/login**
   - Request: `{ provider, authorizationCode, redirectUri }`
   - `redirectUri`가 허용 목록에 있는지 검증
   - provider별 Token API 호출 → access_token 획득
   - provider별 User Info API 호출 → 이메일, 닉네임, 프로필 이미지 등 수집
   - `UserRepository.findByProviderAndProviderId()`로 기존 사용자 조회 또는 신규 생성
   - JWT 발급 후 `{ accessToken, user }` 반환

2. **JWT**
   - 클레임: `userId`, `role`, `nickname`
   - `application.yml`에 secret, expiration 설정

---

## 6. 제공자별 API 차이 (참고)

| 제공자 | Token URI | User Info URI | 응답 형식 |
|--------|-----------|---------------|-----------|
| 카카오 | `https://kauth.kakao.com/oauth/token` | `https://kapi.kakao.com/v2/user/me` | JSON (kakao_account 등) |
| 네이버 | `https://nid.naver.com/oauth2.0/token` | `https://openapi.naver.com/v1/nid/me` | JSON |
| 구글 | `https://oauth2.googleapis.com/token` | `https://www.googleapis.com/oauth2/v3/userinfo` | JSON (표준 OIDC) |
| 애플 | `https://appleid.apple.com/auth/token` | ID Token JWT 내 claims | JWT 디코딩 |

각 제공자마다 **요청 파라미터명**, **응답 구조**, **User Info 추출 방식**이 다르므로 provider별 분기 처리가 필요합니다.

---

## 7. 정리

1. **OAuth2**는 비밀번호 없이 제3자(카카오 등)를 통해 인증·인가를 수행하는 프레임워크이다.
2. **Authorization Code Flow**는 code → token 교환을 통해 상대적으로 안전하게 액세스 토큰을 얻는 방식이다.
3. 우리 프로젝트는 **Flutter가 OAuth UI를 담당**하고, **백엔드는 code를 받아 token 교환·사용자 조회·JWT 발급**만 한다.
4. Spring의 **OAuth2 Client** 컴포넌트를 활용해 토큰 교환과 API 호출을 구현할 수 있다.
5. 제공자별 API 스펙 차이를 고려해 **provider별 분기**가 필요하다.

이 문서를 이해하셨다면 Step 3(OAuth2 설정 및 의존성) 개발을 진행할 수 있습니다.
