# Sprint 1 할일 문서 (백엔드)

> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/ERD.md`

**클라이언트**: Flutter 앱

## Sprint 1 목표

프로젝트 기초를 완성하고, **OAuth2 소셜 로그인·프로필 조회**가 동작하는 API를 만든다.

### 완료 조건
- [ ] OAuth2 로그인(카카오/네이버/구글/애플) 후 JWT 발급
- [ ] 로그인 후 내 프로필 조회/수정 가능
- [ ] 다른 사용자 프로필 조회 가능

---

## Step 1: 인프라 및 공통 구조 (B1-1, B1-2, B1-3)

> Phase 1에서 완료된 항목. 확인만 진행.

- [x] Spring Boot Web, JPA, MySQL 의존성
- [x] DB 연결 설정 (application.yml)
- [x] 패키지 구조 (controller, service, repository, entity)
- [x] ApiResponse, 전역 예외 처리
- [x] API 문서 (docs/api/README.md)

---

## Step 2: User 엔티티 및 Repository

### Step 2.1: User 엔티티
- [ ] User 엔티티 생성 (`docs/ERD.md` Users 테이블 참조)
  - id, email (소셜에서 제공), nickname
  - **provider** (KAKAO, NAVER, GOOGLE, APPLE)
  - **providerId** (소셜 제공자 고유 ID)
  - profileImg, level(급수: A/B/C/D/BEGINNER)
  - **interestLoc1, interestLoc2** (행정구역 코드/ID - 주변 매칭 검색용)
  - racketInfo, playStyle (선호 플레이 스타일)
  - ratingScore, penaltyCount
  - createdAt, updatedAt
- [ ] (provider, providerId) Unique 제약 - 동일 소셜 계정 중복 가입 방지
- [ ] BaseEntity (createdAt, updatedAt) - 선택

### Step 2.2: User Repository
- [ ] UserRepository 인터페이스 (JpaRepository 상속)
- [ ] findByProviderAndProviderId(provider, providerId) 메서드
- [ ] existsByNickname(nickname) 메서드 (닉네임 중복 체크용)

---

## Step 3: OAuth2 설정 및 의존성

### Step 3.1: 인증 방식 (Flutter 앱)
- [ ] **Authorization Code 방식 통일**: Flutter 앱이 OAuth 인증 후 **authorization code**를 백엔드에 전달
- [ ] 백엔드가 code를 받아 토큰 교환 → 사용자 정보 조회 → User 생성/조회 → JWT 발급

### Step 3.2: 의존성
- [ ] OAuth2 클라이언트 (각 제공자 REST API 호출용)
- [ ] JWT 의존성 (jjwt 또는 spring-boot-starter-security)

### Step 3.3: OAuth2 설정
- [ ] application.yml에 OAuth2 설정 (카카오, 네이버, 구글, 애플)
  - client-id, client-secret (각 플랫폼 개발자 콘솔 발급)
  - token-uri, user-info-uri (code → token, token → userInfo 교환용)
- [ ] **Redirect URI 허용 목록**: application.yml에 허용된 redirect-uri 목록 관리
  - Flutter 앱, React 웹 등 클라이언트별 redirect URI가 다를 수 있음
  - Request의 redirectUri가 허용 목록에 있는지 검증 후 토큰 교환 진행

---

## Step 4: OAuth2 로그인 API (B2-1, B2-2)

### Step 4.1: Authorization Code 처리
- [ ] POST /api/auth/oauth/login Request: `{ provider, authorizationCode, redirectUri }`
- [ ] **redirectUri 검증**: application.yml에 등록된 허용 목록에 있는지 확인 후 토큰 교환 진행
- [ ] 백엔드: authorizationCode로 액세스 토큰 교환 → 사용자 정보 API 호출
- [ ] 소셜 제공자에서 받은 사용자 정보로 User 조회/생성
- [ ] 신규 사용자: User 생성 후 추가 프로필 입력 유도 (닉네임, 관심 지역 등)
- [ ] 기존 사용자: User 조회

### Step 4.2: JWT 발급
- [ ] JWT 토큰 생성/검증 유틸 또는 Service
- [ ] **JWT 클레임**: `userId`, `role`, `nickname` 포함 (프론트에서 API 호출 없이 기본 정보 표시 가능)
- [ ] application.yml에 JWT 설정 (secret, expiration)

### Step 4.3: 토큰 발급 응답
- [ ] AuthController: JWT 발급 응답 (accessToken, user 정보)

---

## Step 5: 프로필 API (B2-4, B2-5, B2-6)

### Step 5.1: 닉네임 중복 체크
- [ ] GET /api/users/check-nickname?nickname={nickname} (인증 불필요)
- [ ] 프로필 설정 전에 중복 확인 가능
- [ ] 응답: `{ available: true | false }`

### Step 5.2: 내 프로필
- [ ] 내 프로필 조회 API GET /api/users/me (인증 필요)
- [ ] 내 프로필 수정 API PATCH /api/users/me (인증 필요)
- [ ] UserService.getMyProfile(), updateMyProfile()

### Step 5.3: 타인 프로필
- [ ] 타인 프로필 조회 API GET /api/users/{userId}
- [ ] UserService.getUserProfile(userId)
- [ ] 응답 DTO (공개 가능한 필드만 노출)

### Step 5.4: 프로필 수정 DTO
- [ ] nickname, profileImg, level, **interestLoc1, interestLoc2 (행정구역 코드/ID)**
- [ ] racketInfo, playStyle
- [ ] 관심 지역 최대 2곳 검증 (행정구역 코드 형식)
- [ ] 신규 가입 시 필수 프로필(nickname, interestLoc1) 입력

### Step 5.5: 프로필 이미지 (profileImg)
- [ ] **Sprint 1 범위**: profileImg는 **URL 문자열**만 업데이트 (JSON body에 URL 전달)
- [ ] Multipart/form-data 파일 업로드(S3 등)는 **이번 스프린트 제외**
- [ ] 소셜 로그인 시 제공자에서 받은 profile_img URL을 기본값으로 저장
- [ ] 이미지 업로드 서버가 없다면: 클라이언트가 외부 URL을 전달하거나, 기본 이미지 URL 사용 → 실제 파일 업로드는 다음 스프린트로 이관

---

## Step 6: 인증 필터 및 API 보호 (B7-2 일부)

### Step 6.1: 인증 필터
- [ ] JWT 검증 필터 또는 Interceptor
- [ ] /api/auth/*, /api/health, /api/users/check-nickname은 인증 제외
- [ ] /api/users/me, PATCH /api/users/me는 인증 필요

### Step 6.2: 현재 사용자 주입
- [ ] @AuthenticationPrincipal 또는 ThreadLocal로 현재 User 조회
- [ ] Controller에서 로그인 사용자 식별

---

## Step 7: API 문서 업데이트

- [ ] docs/api/README.md에 Sprint 1 API 추가
  - POST /api/auth/oauth/login (provider, authorizationCode, redirectUri)
  - JWT 클레임: userId, role, nickname
  - GET /api/users/check-nickname?nickname={nickname}
  - GET /api/users/me
  - PATCH /api/users/me
  - GET /api/users/{userId}

---

## Sprint 1 백엔드 작업 요약

| Step | 내용 | 백로그 ID |
|------|------|-----------|
| 1 | 인프라 확인 | B1-1, B1-2, B1-3 |
| 2 | User 엔티티(provider, providerId, 행정구역 코드), Repository | B2-4, B2-6 |
| 3 | OAuth2 설정 (Authorization Code 방식) | B2-1, B2-2 |
| 4 | OAuth2 로그인(code→JWT), JWT 클레임(id, role, nickname) | B2-1, B2-2 |
| 5 | 닉네임 중복 체크, 프로필 조회/수정 API | B2-4, B2-5, B2-6 |
| 6 | 인증 필터 | B7-2 |
| 7 | API 문서 | B1-3 |
