# Sprint 1 API 문서

> OAuth2 소셜 로그인 및 프로필 API  
> 프론트엔드 개발자를 위한 API 명세서

---

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080/api` |
| Content-Type | `application/json` |
| 인증 방식 | JWT Bearer Token |

---

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "code": "ERROR_CODE"
}
```

---

## Enum 참조

### Provider (OAuth 제공자)

| 값 | 설명 |
|----|------|
| KAKAO | 카카오 로그인 |
| NAVER | 네이버 로그인 |
| GOOGLE | 구글 로그인 |
| APPLE | 애플 로그인 |

### Level (자칭 급수)

| 값 | 설명 |
|----|------|
| A | A급 |
| B | B급 |
| C | C급 |
| D | D급 |
| BEGINNER | 초급 |

### Role (역할, JWT 클레임용)

| 값 | 설명 |
|----|------|
| USER | 일반 사용자 |
| ADMIN | 관리자 |

---

## 인증

### JWT 사용 방법

인증이 필요한 API 호출 시 `Authorization` 헤더에 Bearer 토큰을 포함합니다.

```
Authorization: Bearer {accessToken}
```

### JWT 클레임

| 클레임 | 타입 | 설명 |
|--------|------|------|
| userId | Long | 사용자 ID |
| role | String | 역할 (USER, ADMIN) |
| nickname | String | 닉네임 |

---

## API 목록

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | /api/auth/oauth/login | 불필요 | OAuth2 소셜 로그인 |
| GET | /api/users/check-nickname | 불필요 | 닉네임 중복 체크 |
| GET | /api/users/me | **필요** | 내 프로필 조회 |
| PATCH | /api/users/me | **필요** | 내 프로필 수정 |
| GET | /api/users/{userId} | 불필요 | 타인 프로필 조회 |

---

## 상세 API

### 1. OAuth2 소셜 로그인

**POST** `/api/auth/oauth/login`

Flutter 앱에서 OAuth 인증 후 받은 authorization code를 전달하여 JWT를 발급받습니다.

**인증** 불필요

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| provider | Provider | O | OAuth 제공자 (enum) |
| authorizationCode | String | O | OAuth 인증 후 받은 code |
| redirectUri | String | O | OAuth redirect URI (허용 목록에 등록된 URI) |

**Request 예시**

```json
{
  "provider": "KAKAO",
  "authorizationCode": "xxx_authorization_code_from_oauth",
  "redirectUri": "com.mintonmatch.app://oauth/callback"
}
```

**Response 예시**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "닉네임",
      "profileImg": "https://example.com/profile.jpg",
      "profileComplete": false,
      "joinedAt": "2025-03-10T12:00:00"
    }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| user.joinedAt | String (ISO-8601) | 가입일 |

**user.profileComplete**

- `true`: 닉네임, 관심 지역 1 등 필수 프로필 입력 완료
- `false`: 신규 가입 또는 필수 프로필 미입력 → 추가 프로필 입력 유도

---

### 2. 닉네임 중복 체크

**GET** `/api/users/check-nickname?nickname={nickname}`

프로필 설정 전 닉네임 사용 가능 여부를 확인합니다.

**인증** 불필요

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| nickname | String | O | 확인할 닉네임 |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "available": true
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| available | boolean | `true`: 사용 가능, `false`: 이미 사용 중 |

---

### 3. 내 프로필 조회

**GET** `/api/users/me`

로그인한 사용자의 프로필을 조회합니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

마이페이지 집계 필드(`hostedMatchCount`, `participatedMatchCount`) 정책은 [Sprint7-API.md](./Sprint7-API.md) § Step 1 참고.

**Response 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "배드민턴왕",
    "profileImg": "https://example.com/profile.jpg",
    "level": "B",
    "interestLoc1": "1159010500",
    "interestLoc2": "1168010100",
    "racketInfo": "요넥스 아크세이버",
    "playStyle": "공격형",
    "ratingScore": 5.0,
    "penaltyCount": 0,
    "hostedMatchCount": 12,
    "participatedMatchCount": 30
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 사용자 ID |
| nickname | String | 닉네임 |
| profileImg | String | 프로필 이미지 URL |
| level | Level | 자칭 급수 (enum) |
| interestLoc1 | String | 관심 지역 1 (행정구역 코드) |
| interestLoc2 | String | 관심 지역 2 (행정구역 코드) |
| racketInfo | String | 라켓 정보 |
| playStyle | String | 선호 플레이 스타일 |
| ratingScore | Float | 매너/실력 평균 점수 (기본 5.0) |
| penaltyCount | Integer | 노쇼/지각 누적 횟수 (기본 0) |
| hostedMatchCount | Long | (본인만) 내가 방장으로 연 매칭 수 — [Sprint7-API.md](./Sprint7-API.md) |
| participatedMatchCount | Long | (본인만) 참여 매칭 수 — [Sprint7-API.md](./Sprint7-API.md) |

---

### 4. 내 프로필 수정

**PATCH** `/api/users/me`

로그인한 사용자의 프로필을 수정합니다. 전달한 필드만 부분 수정됩니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

**Request Body** (모든 필드 선택)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | String | △ | 닉네임 (최대 30자). 신규 가입 시 필수 |
| profileImg | String | X | 프로필 이미지 URL (최대 2000자) |
| level | Level | X | 자칭 급수 (enum) |
| interestLoc1 | String | △ | 관심 지역 1 (7~10자리 숫자). 신규 가입 시 필수 |
| interestLoc2 | String | X | 관심 지역 2 (7~10자리 숫자) |
| racketInfo | String | X | 라켓 정보 (최대 100자) |
| playStyle | String | X | 플레이 스타일 (최대 50자) |

△ 신규 가입(닉네임/관심지역 미입력) 시 첫 프로필 수정에서 필수

**Request 예시**

```json
{
  "nickname": "배드민턴왕",
  "profileImg": "https://example.com/new-profile.jpg",
  "level": "B",
  "interestLoc1": "1159010500",
  "interestLoc2": "1168010100",
  "racketInfo": "요넥스 아크세이버",
  "playStyle": "공격형"
}
```

**Response 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "배드민턴왕",
    "profileImg": "https://example.com/new-profile.jpg",
    "level": "B",
    "interestLoc1": "1159010500",
    "interestLoc2": "1168010100",
    "racketInfo": "요넥스 아크세이버",
    "playStyle": "공격형",
    "ratingScore": 5.0,
    "penaltyCount": 0
  }
}
```

**유효성 검증**

- `interestLoc1`, `interestLoc2`: 7~10자리 숫자 형식 (행정구역 코드)
- `profileImg`: URL 문자열 전달 시 그대로 저장. 프로필 이미지 업로드는 [Sprint2-API.md](./Sprint2-API.md) 참조

---

### 5. 타인 프로필 조회

**GET** `/api/users/{userId}`

다른 사용자의 프로필을 조회합니다. (내 프로필 조회와 동일한 응답 구조)

**인증** 불필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| userId | Long | 조회할 사용자 ID |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "id": 2,
    "nickname": "다른유저",
    "profileImg": "https://example.com/profile2.jpg",
    "level": "A",
    "interestLoc1": "1159010500",
    "interestLoc2": null,
    "racketInfo": "빅터 제트스피드",
    "playStyle": "수비형",
    "ratingScore": 4.8,
    "penaltyCount": 0
  }
}
```

응답 구조는 내 프로필 조회와 동일합니다.

---

## 에러 코드

| code | HTTP 상태 | 설명 |
|------|-----------|------|
| BAD_REQUEST | 400 | 잘못된 요청 (유효성 검증 실패 등) |
| UNAUTHORIZED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 접근 권한 없음 |
| NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| OAUTH_INVALID | 400 | OAuth 인증 실패 |
| INTERNAL_SERVER_ERROR | 500 | 서버 오류 |

---

## 기타 API (Sprint 1 이전)

### 헬스 체크

**GET** `/api/health`

서버 상태를 확인합니다.

**Response 예시**

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "minton-match-api"
  }
}
```
