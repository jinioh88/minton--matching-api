# 배드민턴 매칭 API 문서

> 프론트엔드 개발자를 위한 API 명세서입니다. 스프린트별로 문서가 분리되어 있습니다.

## 문서 목록

| 스프린트 | 문서 | 설명 |
|----------|------|------|
| Sprint 1 | [Sprint1-API.md](./Sprint1-API.md) | OAuth2 소셜 로그인, 프로필 API |

---

## 기본 정보

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`

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

## API 목록 (전체)

| 메서드 | 경로 | 스프린트 | 설명 |
|--------|------|----------|------|
| GET | /api/health | - | 서버 상태 확인 |
| POST | /api/auth/oauth/login | Sprint 1 | OAuth2 소셜 로그인 |
| GET | /api/users/check-nickname | Sprint 1 | 닉네임 중복 체크 |
| GET | /api/users/me | Sprint 1 | 내 프로필 조회 |
| PATCH | /api/users/me | Sprint 1 | 내 프로필 수정 |
| GET | /api/users/{userId} | Sprint 1 | 타인 프로필 조회 |

---

## 상세 API

### 헬스 체크

**GET** `/api/health`

서버 상태를 확인합니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "minton-match-api"
  }
}
```

---

**Sprint 1 API 상세**는 [Sprint1-API.md](./Sprint1-API.md)를 참조하세요.
