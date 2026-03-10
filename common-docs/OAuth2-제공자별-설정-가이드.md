# OAuth2 제공자별 설정 가이드

> `application.yml`에 들어갈 OAuth2 설정 값을 각 플랫폼 개발자 콘솔에서 발급받는 방법입니다.

---

## 공통 사항

### 필요한 설정 값

| 설정 | 설명 | application.yml 매핑 |
|------|------|----------------------|
| Client ID | 앱 식별자 | `client-id` |
| Client Secret | 앱 비밀키 (서버에만 보관) | `client-secret` |
| Redirect URI | 인증 후 돌아올 URL | `redirect-uri`, `app.oauth2.allowed-redirect-uris` |

### Redirect URI 예시

| 환경 | Redirect URI |
|------|--------------|
| 로컬 (웹) | `http://localhost:3000/oauth/callback` |
| 로컬 (127.0.0.1) | `http://127.0.0.1:3000/oauth/callback` |
| Flutter 앱 | `com.mintonmatch.app://oauth/callback` |

---

## 1. 카카오 (Kakao)

### 1.1 접속

- **URL**: https://developers.kakao.com/
- **로그인**: 카카오 계정

### 1.2 앱 생성

1. 상단 **내 애플리케이션** 클릭
2. **애플리케이션 추가하기** → 앱 이름 입력 후 저장

### 1.3 앱 키 확인

1. 생성한 앱 선택
2. **앱 키** 탭 이동
3. 확인할 값:
   - **REST API 키** → `KAKAO_CLIENT_ID`
   - **클라이언트 시크릿** → `KAKAO_CLIENT_SECRET`  
     (REST API 키 옆 **클라이언트 시크릿** → **코드 생성** → 생성된 값 복사)

### 1.4 Redirect URI 등록

1. **앱 키** 탭 → **REST API 키** 영역
2. **Redirect URI** 섹션에서 **Redirect URI 등록** 클릭
3. 사용할 URI 추가 (예: `http://localhost:3000/oauth/callback`, `com.mintonmatch.app://oauth/callback`)

### 1.5 동의 항목 설정

1. **제품 설정** → **카카오 로그인** → **동의항목** 탭
2. **닉네임**, **프로필 사진**, **카카오계정(이메일)** 등 필요한 항목 **동의 요청**으로 설정

### 1.6 application.yml 매핑

| 발급 값 | 환경변수 | application.yml |
|---------|----------|-----------------|
| REST API 키 | KAKAO_CLIENT_ID | client-id |
| 클라이언트 시크릿 | KAKAO_CLIENT_SECRET | client-secret |

### 1.7 Scope (동의 항목 ID)

- `profile_nickname`: 닉네임
- `account_email`: 이메일
- `profile_image`: 프로필 이미지

---

## 2. 네이버 (Naver)

### 2.1 접속

- **URL**: https://developers.naver.com/
- **로그인**: 네이버 계정

### 2.2 애플리케이션 등록

1. **Application** → **애플리케이션 등록** 클릭
2. **이용 API**: **네이버 로그인** 선택
3. **애플리케이션 이름**: 서비스명 입력 (예: 배드민턴 매칭)
4. **사용 API**: **네이버 로그인** 체크

### 2.3 서비스 환경 설정

**웹** (웹/React 사용 시):

- **서비스 URL**: `http://localhost:3000` (로컬) 또는 실제 도메인
- **Callback URL**: `http://localhost:3000/oauth/callback` (등록한 Redirect URI와 동일)

**iOS** (Flutter iOS):

- **URL Scheme**: `com.mintonmatch.app`

**Android** (Flutter Android):

- **패키지 이름**: `org.app.mintonmatch` (실제 패키지명으로 변경)

### 2.4 Client ID / Client Secret 확인

1. **Application** → **내 애플리케이션**
2. 등록한 앱 선택
3. **Client ID**, **Client Secret** 확인

### 2.5 application.yml 매핑

| 발급 값 | 환경변수 | application.yml |
|---------|----------|-----------------|
| Client ID | NAVER_CLIENT_ID | client-id |
| Client Secret | NAVER_CLIENT_SECRET | client-secret |

### 2.6 API 권한

- **API 권한관리** 탭에서 **네이버 로그인** 관련 API 체크

---

## 3. 구글 (Google)

### 3.1 접속

- **URL**: https://console.cloud.google.com/
- **로그인**: Google 계정

### 3.2 프로젝트 생성/선택

1. 상단 프로젝트 선택 → **새 프로젝트** 또는 기존 프로젝트 선택
2. 프로젝트 이름 입력 후 **만들기**

### 3.3 OAuth 동의 화면 설정

1. **API 및 서비스** → **OAuth 동의 화면**
2. **외부** (일반 사용자) 또는 **내부** (테스트용) 선택
3. **앱 이름**, **사용자 지원 이메일**, **개발자 연락처 정보** 입력
4. **범위 추가** → `email`, `profile`, `openid` 선택
5. **저장 후 계속** → **테스트 사용자** (개발 중) 추가 후 저장

### 3.4 사용자 인증 정보 생성

1. **API 및 서비스** → **사용자 인증 정보**
2. **+ 사용자 인증 정보 만들기** → **OAuth 클라이언트 ID**
3. **애플리케이션 유형**: **웹 애플리케이션** (백엔드용)
4. **이름**: 예) "배드민턴 매칭 API"
5. **승인된 리디렉션 URI** 추가:
   - `http://localhost:3000/oauth/callback`
   - `com.mintonmatch.app://oauth/callback` (앱 스킴은 구글에서 지원하는 경우)
   - 실제 서비스 URL

### 3.5 Client ID / Client Secret 확인

1. 생성 후 표시되는 **클라이언트 ID**, **클라이언트 보안 비밀** 복사
2. **클라이언트 보안 비밀**은 한 번만 표시되므로 안전한 곳에 저장

### 3.6 application.yml 매핑

| 발급 값 | 환경변수 | application.yml |
|---------|----------|-----------------|
| 클라이언트 ID | GOOGLE_CLIENT_ID | client-id |
| 클라이언트 보안 비밀 | GOOGLE_CLIENT_SECRET | client-secret |

### 3.7 Scope

- `openid`, `email`, `profile`

---

## 4. 애플 (Apple)

### 4.1 접속

- **URL**: https://developer.apple.com/
- **로그인**: Apple Developer 계정 (유료 멤버십 필요)

### 4.2 App ID 설정

1. **Certificates, Identifiers & Profiles** → **Identifiers**
2. **+** 버튼 → **App IDs** 선택
3. **App** 선택 후 **Continue**
4. **Description**, **Bundle ID** 입력 (예: `org.app.mintonmatch`)
5. **Sign in with Apple** 체크 후 **Register**

### 4.3 Service ID 생성 (웹/Android용)

1. **Identifiers** → **+** 버튼 → **Services IDs** 선택
2. **Identifier**: 예) `org.app.mintonmatch.signin`
3. **Description**: 예) "Sign in with Apple"
4. **Configure** 클릭:
   - **Primary App ID**: 위에서 만든 App ID 선택
   - **Domains and Subdomains**: 실제 도메인 (예: `api.example.com`)
   - **Return URLs**: Redirect URI (예: `https://api.example.com/oauth/callback`, `com.mintonmatch.app://oauth/callback`)

### 4.4 Sign in with Apple Key 생성

1. **Keys** → **+** 버튼
2. **Key Name**: 예) "Sign in with Apple Key"
3. **Sign in with Apple** 체크 → **Configure** → App ID 선택
4. **Continue** → **Register**
5. **.p8 파일 다운로드** (1회만 가능, 안전하게 보관)
6. **Key ID** 10자리 복사

### 4.5 Client Secret (JWT) 생성

Apple은 Client Secret을 **JWT**로 직접 만들어야 합니다.

**필요 정보:**

| 항목 | 위치 |
|------|------|
| Key ID | Keys에서 생성한 Key의 Key ID |
| Team ID | Membership 상세의 Team ID |
| Client ID | Service ID 또는 App ID의 Identifier |
| Private Key | 다운로드한 .p8 파일 내용 |

**JWT 생성 예시 (백엔드에서):**

- `kid`: Key ID
- `iss`: Team ID
- `sub`: Client ID (Service ID)
- `aud`: `https://appleid.apple.com`
- 알고리즘: ES256 (ECDSA P-256, SHA-256)
- 유효기간: 최대 6개월

> JWT 생성 라이브러리 또는 스크립트를 사용해 백엔드에서 동적으로 생성하는 방식이 일반적입니다.

### 4.6 application.yml 매핑

| 발급/생성 값 | 환경변수 | application.yml |
|--------------|----------|-----------------|
| Service ID (웹) 또는 App ID (iOS) | APPLE_CLIENT_ID | client-id |
| JWT (동적 생성) | APPLE_CLIENT_SECRET | client-secret |

### 4.7 참고

- iOS 앱: App ID의 Bundle ID를 Client ID로 사용
- 웹/Android: Service ID를 Client ID로 사용
- Client Secret은 JWT로, 만료 전 갱신 필요

---

## 5. 환경변수 설정 요약

### 로컬 개발

```bash
export KAKAO_CLIENT_ID="발급받은_값"
export KAKAO_CLIENT_SECRET="발급받은_값"
export NAVER_CLIENT_ID="발급받은_값"
export NAVER_CLIENT_SECRET="발급받은_값"
export GOOGLE_CLIENT_ID="발급받은_값"
export GOOGLE_CLIENT_SECRET="발급받은_값"
export APPLE_CLIENT_ID="발급받은_값"
export APPLE_CLIENT_SECRET="생성한_JWT"  # Apple은 JWT
```

### application-local.yml

로컬에서는 placeholder로 앱이 기동되므로, OAuth 로그인 테스트 시에만 위 환경변수를 설정하면 됩니다.

### application-prod.yml (운영)

운영 배포 시 위 환경변수를 서버/컨테이너에 설정합니다. `APPLE_CLIENT_SECRET`은 JWT를 주기적으로 갱신하는 로직이 필요할 수 있습니다.

---

## 6. allowed-redirect-uris 설정

`application.yml`의 `app.oauth2.allowed-redirect-uris`에는 **실제로 사용할 모든 Redirect URI**를 등록합니다.

- 각 제공자 콘솔에 등록한 URI와 동일해야 합니다.
- 클라이언트(Flutter, React 등)가 OAuth 인증 시 사용하는 URI만 등록합니다.

```yaml
app:
  oauth2:
    allowed-redirect-uris:
      - com.mintonmatch.app://oauth/callback
      - http://localhost:3000/oauth/callback
      - http://127.0.0.1:3000/oauth/callback
```

---

## 7. 공식 문서 링크

| 제공자 | 문서 |
|--------|------|
| 카카오 | https://developers.kakao.com/docs/latest/ko/kakaologin/common |
| 네이버 | https://developers.naver.com/docs/login/api/api.md |
| 구글 | https://developers.google.com/identity/protocols/oauth2 |
| 애플 | https://developer.apple.com/documentation/sign_in_with_apple |
