# Sprint 8 API 문서 — 친구(팔로우) · 소셜 활동 알림

> 할일: `docs/할일/Sprint8-할일.md`  
> 요구: `docs/요구사항분석.md` §2 친구 및 활동 알림  
> 알림 REST·STOMP 공통: [Sprint5-API.md](./Sprint5-API.md) §4, [Sprint6-API.md](./Sprint6-API.md) §12  
> 타인 프로필 ID와 동일한 **`userId`** 로 팔로우 대상을 지정한다 (`GET /api/users/{userId}`).

---

## 1. 인증 · 공통

- **헤더**: `Authorization: Bearer <access_token>` 필수(아래 친구 API 전부).
- **경로 보안**: `SecurityConfig`에서 `/api/users/me/**` 가 **authenticated** 이므로 `/api/users/me/friendships` 포함.
- **성공/실패 래퍼**: [README — 공통 응답](./README.md#공통-응답-형식) (`ApiResponse`, `ErrorResponse`). 실패 시 `code`는 `ErrorCode.name()` (예: `FRIENDSHIP_ALREADY_EXISTS`).

---

## 2. 친구(단방향 팔로우) API

**Base path**: `/api/users/me/friendships`

관계: **`follower_id`** = 로그인 사용자(나), **`following_id`** = 상대 `userId`.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/users/me/friendships` | 상대를 팔로우(친구 추가) |
| GET | `/api/users/me/friendships` | 내 팔로잉 목록(내가 팔로우한 사용자) |
| DELETE | `/api/users/me/friendships/{followingUserId}` | 언팔로우 |

### 2.1 `POST /api/users/me/friendships`

**Content-Type**: `application/json`

**Request body (`FriendshipFollowRequest`)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| followingUserId | Long | O | 팔로우할 사용자 ID (`GET /api/users/{userId}` 과 동일) |

**성공 `data` (`FollowingUserResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 상대 사용자 ID |
| nickname | String \| null | 닉네임 |
| profileImg | String \| null | 프로필 이미지 URL |
| level | enum \| null | `Level` |
| followedAt | datetime | 팔로우 관계 생성 시각 |

**실패 (예)**

| HTTP | code (예) | 설명 |
|------|-----------|------|
| 400 | `FRIENDSHIP_SELF_NOT_ALLOWED` | 자기 자신 팔로우 |
| 400 | `FRIENDSHIP_ALREADY_EXISTS` | 이미 팔로우 중 |
| 404 | `USER_NOT_FOUND` | `followingUserId` 에 해당 사용자 없음 |

### 2.2 `GET /api/users/me/friendships`

**성공 `data`**: `FollowingUserResponse[]` — 팔로우한 순서 **역순**(최근 팔로우가 앞). 빈 배열 가능.

### 2.3 `DELETE /api/users/me/friendships/{followingUserId}`

**Path**: `followingUserId` — 언팔로우할 상대 ID.

**성공**: `data` 는 `null` 일 수 있음 (`success: true`).

**실패**

| HTTP | code | 설명 |
|------|------|------|
| 404 | `FRIENDSHIP_NOT_FOUND` | 해당 팔로우 관계 없음 |

---

## 3. 소셜 활동 알림 (인앱·푸시)

발송 파이프라인은 기존과 동일: **`NotificationService.publishAfterCommit`** → 커밋 후 `notifications` 저장 → STOMP `/user/queue/notifications` · FCM([Sprint6 §12](./Sprint6-API.md#12-알림--fcm--stomp)).

### 3.1 수신 대상 (타깃팅)

- 활동 주체 **B**가 매칭을 만들거나 참여를 **ACCEPTED**로 확정하면, **`following_id = B`** 인 모든 **`follower_id`** 에게 알림.
- B 본인에게는 발송하지 않음(팔로워 목록에 B는 원칙적으로 없음).
- 팔로워가 없으면 **이벤트·INSERT 없음**.

### 3.2 신규 `NotificationType`

| 값 | 수신자 | 발생 시점 |
|----|--------|-----------|
| `FRIEND_CREATED_MATCH` | 활동 호스트의 팔로워 | 호스트가 매칭을 생성·저장한 직후 (`MatchStatus.RECRUITING` 최초 저장) |
| `FRIEND_CONFIRMED_PARTICIPATION` | 확정된 참여자의 팔로워 | 참여자 상태가 **ACCEPTED**로 바뀐 직후(방장 수락 **또는** 예약·긴급 선착순 수락) |

ENUM 전체 표는 [Sprint5 §4.3](./Sprint5-API.md#43-notificationtype-enum)에 통합되어 있다.

### 3.3 `relatedMatchId` · 딥링크 (B2-10)

- 두 타입 모두 **`relatedMatchId` 필수**(DB·`NotificationResponse`·STOMP `NotificationRealtimePayload` 동일).
- `FRIEND_CONFIRMED_PARTICIPATION` 은 추가로 **`relatedParticipantId`** 가 채워질 수 있다(참여 행 ID).
- 클라이언트: 알림 탭 시 요구사항 §2.4에 맞춰 **매칭 상세**로 이동 (예: 웹 `/matching/[id]`, 앱은 동일 `matchId` 라우트).

### 3.4 `title` / `body` 패턴 (실제와 동일)

닉네임이 없으면 **`회원`** 으로 치환.

**`FRIEND_CREATED_MATCH`**

- title: `새 매칭` (길면 서버에서 잘림 규칙 적용)
- body: `{닉네임}님이 새로운 매칭을 만들었습니다! 지금 확인해보세요.`

**`FRIEND_CONFIRMED_PARTICIPATION`**

- title: `참여 확정`
- body: `{닉네임}님이 매칭 참여를 확정했습니다! 함께 참여하시겠어요?`

---

## 4. 운영 DB

- **`friendships`** 테이블은 **Flyway** 로 반영한다(예: `V6__create_friendships.sql`). 로컬 기본 프로파일은 Hibernate `ddl-auto` 로도 엔티티와 맞출 수 있다([`application.yml`](../../src/main/resources/application.yml) 주석 참고).

---

## 5. 프론트 체크리스트 (요약)

| 항목 | 설명 |
|------|------|
| 프로필 | 타인 프로필 `userId` 로 `POST .../friendships` |
| 목록 | 마이페이지 등에서 `GET .../friendships` |
| 알림 | `type` 이 위 두 enum 일 때 `relatedMatchId` 로 상세 이동 |
| 실시간 | 기존처럼 `GET /api/notifications` 폴링 또는 STOMP `/user/queue/notifications` |
