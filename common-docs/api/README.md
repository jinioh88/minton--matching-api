# 배드민턴 매칭 API 문서

> 프론트엔드 개발자를 위한 API 명세서입니다. 스프린트별로 문서가 분리되어 있습니다.

## 문서 목록

| 스프린트 | 문서 | 설명 |
|----------|------|------|
| Sprint 1 | [Sprint1-API.md](./Sprint1-API.md) | OAuth2 소셜 로그인, 프로필 API |
| Sprint 2 | [Sprint2-API.md](./Sprint2-API.md) | S3 파일 업로드, 프로필 이미지, 매칭 API |
| Sprint 3 | [Sprint3-API.md](./Sprint3-API.md) | 참여 신청, 수락/거절, 대기열, 취소 API |
| Sprint 6 | [Sprint6-API.md](./Sprint6-API.md) | **신규 REST**(푸시 토큰)·STOMP·FCM·Redis·운영 env (채팅·알림 CRUD REST는 Sprint 5) |
| Sprint 7 | [Sprint7-API.md](./Sprint7-API.md) | 마이페이지 보강 — 내 프로필 집계 등 (`GET /api/users/me` 확장) |
| Sprint 8 | [Sprint8-API.md](./Sprint8-API.md) | 친구(팔로우) REST, 소셜 활동 알림 타입·`relatedMatchId` |

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
| POST | /api/files/upload | Sprint 2 | 이미지 파일 업로드 |
| POST | /api/users/me/profile-image | Sprint 2 | 프로필 이미지 업로드 |
| POST | /api/matches | Sprint 2 | 매칭 생성 |
| GET | /api/matches | Sprint 2 | 매칭 목록 조회 |
| GET | /api/matches/{matchId} | Sprint 2 | 매칭 상세 조회 |
| POST | /api/matches/{matchId}/participants | Sprint 3 | 참여/대기 신청 |
| PATCH | /api/matches/{matchId}/participants/{participationId} | Sprint 3 | 방장 수락/거절 |
| DELETE | /api/matches/{matchId}/participants/me | Sprint 3 | 참여 취소 |
| POST | /api/matches/{matchId}/participants/me/accept-offer | Sprint 3 | 예약 수락 |
| POST | /api/matches/{matchId}/participants/me/reject-offer | Sprint 3 | 예약 거절 |
| GET | /api/matches/{matchId}/participants/applications | Sprint 3 | 방장 신청 목록 조회 |
| POST | /api/users/me/friendships | Sprint 8 | 친구(팔로우) 추가 |
| GET | /api/users/me/friendships | Sprint 8 | 내 팔로잉 목록 |
| DELETE | /api/users/me/friendships/{followingUserId} | Sprint 8 | 언팔로우 |

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

**Sprint 1 API 상세**는 [Sprint1-API.md](./Sprint1-API.md), **Sprint 2 API 상세**는 [Sprint2-API.md](./Sprint2-API.md), **Sprint 3 API 상세**는 [Sprint3-API.md](./Sprint3-API.md)를 참조하세요. **Sprint 6 API·실시간 연동**은 [Sprint6-API.md](./Sprint6-API.md), **Sprint 7**은 [Sprint7-API.md](./Sprint7-API.md), **Sprint 8(친구·소셜 알림)**은 [Sprint8-API.md](./Sprint8-API.md)를 참조하세요.
