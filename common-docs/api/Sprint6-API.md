# Sprint 6 API · 실시간 연동 가이드

> **대상**: 프론트엔드(웹·앱). Sprint 6에서 **추가·확정된 HTTP API**, **WebSocket(STOMP)**, **FCM·Redis(서버)**, **운영 환경**을 정리한다.  
> 채팅·알림 **기존 REST** 전체 목록·필드는 [Sprint5-API.md](./Sprint5-API.md)를 따른다.  
> **합의 배경**: [`docs/Sprint6-할일.md`](../../docs/Sprint6-할일.md) §A ~ §F.

**목차**: [1. 문서 역할](#1-문서-역할) · [2. Sprint 6 신규 HTTP API](#2-sprint-6-신규-http-api) · [3. 환경·URL](#3-환경url) · [4. 클라이언트 스택](#4-클라이언트-스택) · [5. STOMP 연결·인증](#5-stomp-연결인증) · [6. STOMP Destination·메시지 규격](#6-stomp-destination메시지-규격) · [7~11. 화면·채팅 정책](#7-화면별-연결구독-생명주기) · [12. 알림](#12-알림--fcm--stomp) · [13. Redis](#13-redis-서버-인프라) · [14. 운영 환경변수](#14-운영-환경변수-요약) · [15. 팀 확인 사항](#15-백엔드-구현-직후-팀이-맞출-것)

공통 응답·에러: [README — 공통 응답 형식](./README.md#공통-응답-형식).

---

## 1. 문서 역할

| 구분 | 문서 |
|------|------|
| 채팅·알림 **REST**(목록, 발송, 수정, 삭제, 알림 CRUD 등) | [Sprint5-API.md](./Sprint5-API.md) |
| **Sprint 6 신규 REST**(푸시 토큰) | **본 문서 §2** (+ Sprint5 §4.6과 동일 내용 요약) |
| **WebSocket + STOMP**(채팅 SEND/SUBSCRIBE, 에러 큐, 알림 큐) | **본 문서 §5~6** |
| **FCM data**, **Redis 릴레이**, **CORS·운영 env** | **본 문서 §12~14** |

---

## 2. Sprint 6 신규 HTTP API

Sprint 6 백엔드에서 **새로 추가된** 알림 관련 REST다. 모두 **`Authorization: Bearer <access_token>`** 필수, `SecurityConfig` 상 `/api/notifications/**` → `authenticated`.

### 2.1 API 요약

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/notifications/push-tokens` | FCM 등록 토큰 **등록·갱신(Upsert)** |
| POST | `/api/notifications/push-tokens/revoke` | 해당 토큰 **비활성화**(로그아웃 등), 멱등 |

### 2.2 `POST /api/notifications/push-tokens`

**Request body (`application/json`)**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| fcmToken | string | 예 | 공백 불가, 최대 512자 | FCM `getToken()` 등으로 받은 문자열 |
| platform | string enum | 예 | `ANDROID` \| `IOS` | |

**동작**

- 동일 `fcmToken`이 없으면 **신규 행** 저장(현재 사용자·멀티 기기 허용).
- 이미 DB에 있는 토큰이면 **현재 로그인 사용자로 소유 이전** 및 `platform`·활성 상태 갱신(다른 계정에 묶였던 기기 재로그인 시 대비).

**Response** — 성공 시 [ApiResponse](./README.md#공통-응답-형식) `success: true`, `data`: `null`.

**오류(예)**

| HTTP | code (예시) | 상황 |
|------|-------------|------|
| 400 | `VALIDATION_ERROR` | `@Valid` 실패(토큰 누락·길이, platform 누락 등) |
| 401 | `UNAUTHORIZED` | 토큰 없음·무효 |
| 404 | `USER_NOT_FOUND` | principal 사용자 ID가 DB에 없을 때(드묾) |

### 2.3 `POST /api/notifications/push-tokens/revoke`

**Request body (`application/json`)**

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| token | string | 예 | 공백 불가, 최대 512자 |

**동작**

- **본인**에게 매핑된 활성 토큰만 `disabled_at` 설정.
- 행이 없거나 다른 사용자 토큰이면 **아무 것도 하지 않고 성공**(멱등).

**Response** — `success: true`, `data`: `null`.

---

## 3. 환경·URL

| 체크 | 항목 | 규칙 |
|------|------|------|
| ☐ | REST Base URL | 기존과 동일 (예: `https://api.example.com`, 로컬 `http://localhost:8080`) |
| ☐ | WebSocket URL | **REST와 동일 호스트**, 경로 **`/ws-chat`** |
| ☐ | 로컬 WS | `ws://localhost:8080/ws-chat` (+ SockJS 사용 시 동일 호스트·경로, 내부 전송 경로는 라이브러리 규칙 따름) |
| ☐ | 운영 WS | `wss://{API 호스트}/ws-chat` (리버스 프록시 TLS 종료 시 **443**) |
| ☐ | CORS·WS Origin | 운영은 환경변수 **`CORS_ALLOWED_ORIGIN_PATTERNS`**(쉼표 구분)로 백엔드 `minton.web.cors-allowed-origin-patterns`에 주입. **프론트 실제 origin과 일치**해야 브라우저 REST·SockJS가 동작한다. |

스테이징 없음 — **로컬·운영**만 ([`Sprint6-할일.md`](../../docs/Sprint6-할일.md) §A5).

---

## 4. 클라이언트 스택·패키지

| 플랫폼 | 권장 | 비고 |
|--------|------|------|
| **웹** | **SockJS + STOMP** | 방화벽/프록시 대비 |
| **네이티브 앱** | **순수 WebSocket + STOMP** | SockJS 불필요 |
| 공통 | STOMP 1.2 클라이언트 | 예: `@stomp/stompjs` |

---

## 5. STOMP 연결·인증

| 체크 | 항목 | 규칙 |
|------|------|------|
| ☐ | JWT | **쿼리스트링 금지** |
| ☑ | `CONNECT` 헤더 | **`Authorization: Bearer <access_token>`** |
| ☐ | 만료 | 만료 전 갱신 목표; 미구현 시 끊김 → 재연결·재로그인 UX |
| ☐ | 재연결 | 새 액세스 토큰으로 `CONNECT` 후 구독 복원 |

### 5.1 백엔드 구현 확정

| 항목 | 값 |
|------|-----|
| 엔드포인트 | **`/ws-chat`** — 순수 WebSocket + **동일 경로 SockJS** |
| 핸드셰이크 | Spring Security: `/ws-chat`, `/ws-chat/**` → `permitAll` (JWT는 STOMP `CONNECT`에서 검증) |
| 토큰 오류 | STOMP **`ERROR`** 프레임 → 연결 실패 |
| 허용 `SUBSCRIBE` | `/topic/chat.{roomId}`(권한: REST와 동일 `assertCanAccessChat`), `/user/queue/errors`, `/user/queue/notifications` |
| 허용 `SEND` | **`/app/chat/messages` 만** |
| 비즈니스 오류 | `@MessageMapping` 예외 → **`/user/queue/errors`** 에 [`ErrorResponse`](./README.md#공통-응답-형식) (`success`, `message`, `code`) |

---

## 6. STOMP Destination·메시지 규격

| 용도 | Destination | 방향 |
|------|-------------|------|
| 방 메시지·시스템 안내 수신 | `/topic/chat.{roomId}` | SUBSCRIBE |
| 채팅 발송 | `/app/chat/messages` | SEND, JSON body |
| 비즈니스 오류 | `/user/queue/errors` | SUBSCRIBE |
| 알림 실시간(포그라운드) | `/user/queue/notifications` | SUBSCRIBE |

**Application prefix**: `/app` → SEND는 **`/app/chat/messages`**.

### 6.1 SEND 본문 (채팅)

[`Sprint5-API.md` §3.6](./Sprint5-API.md) `ChatMessageSendRequest`와 동일 의미.

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | number | 예 | STOMP는 경로가 아니라 **본문** |
| content | string | 예 | 최대 1000자(서버 검증) |
| messageType | string enum | 선택 | 사용자 발송은 **`TEXT`** 또는 **`IMAGE`** 만 허용(`SYSTEM` 등은 거부) |

### 6.2 수신 본문 (채팅 topic)

[`ChatMessageResponse`](./Sprint5-API.md) 스키마 (`messageId`, `roomId`, `senderId`, `senderNickname`, `content`, `messageType`, `createdAt`, `editedAt` 등).

**저장·브로드캐스트되는 `messageType` 값(현재 백엔드 enum)**

| 값 | 설명 |
|----|------|
| `TEXT` | 일반 텍스트 |
| `IMAGE` | 이미지(REST·STOMP 동일 정책) |
| `SYSTEM` | 서버 생성(예: 매칭 **종료·취소** 시 1회 안내 문구) |

> REST `PATCH`/`DELETE`로 수정·삭제한 뒤 **타 클라이언트에 STOMP로 `UPDATE`/`DELETE`를 자동 브로드캐스트하는 로직은 현재 미구현**이다. 상대 화면 갱신은 재조회·`afterId` 보정 등으로 처리하거나, 추후 백엔드 확장 시 [Sprint6-할일 §C5](../../docs/Sprint6-할일.md)에 맞춰 추가할 수 있다.

### 6.3 `/user/queue/errors`

[README `ErrorResponse`](./README.md#공통-응답-형식)와 동일.

### 6.4 프로토콜 오류

STOMP **`ERROR`** → 연결 종료·재로그인 유도.

---

## 7. 화면별 연결·구독 생명주기

[`Sprint6-할일.md`](../../docs/Sprint6-할일.md) §C2·§D 참고.

| 화면 | WebSocket | `/user/queue/notifications` | `/topic/chat.{roomId}` |
|------|-----------|----------------------------|-------------------------|
| 채팅 목록 | 연결 | 구독 | — |
| 채팅 상세 | 연결 | 구독 | 해당 `roomId` 구독 |
| 상세 → 뒤로 | 유지 | 유지 | 해당 방만 Unsubscribe |
| 목록 이탈·로그아웃·앱 종료 | 종료 | 해제 | — |

권한 없는 `roomId` 구독은 서버가 거절할 수 있다.

---

## 8. REST 연동 정책 (채팅)

| 체크 | 항목 | 규칙 |
|------|------|------|
| ☐ | 과거 메시지 | **REST** `GET .../messages` + `cursor` / `size` ([§3.5](./Sprint5-API.md)) |
| ☐ | 최초 진입 | 방 상세 + 메시지 목록 REST 후 STOMP 수신 병합 |
| ☐ | `afterId` 상시 폴링 | WS 유지 중 **중단**, 재연결 직후 **1회** Gap 보정 |
| ☐ | 동기화 | 클라이언트 `lastMessageId` 기준 |

---

## 9. Optimistic UI

| 체크 | 항목 | 규칙 |
|------|------|------|
| ☐ | 임시 ID | `temp-{UUID}` |
| ☐ | 확정 | `/topic/chat.{roomId}` 수신 시 `messageId`로 치환 |
| ☐ | 실패 | `/user/queue/errors` → 재전송·삭제 UX |

---

## 10. 수정·삭제 (REST)

- 요청: [Sprint5 §3.7·§3.8](./Sprint5-API.md) `PATCH` / `DELETE`.
- **현재**: 상대 단말 실시간 반영용 STOMP 브로드캐스트는 **없음**(§6.2 참고).

---

## 11. 매칭 종료 (`FINISHED` / `CANCELLED`)

- 일반 채팅 SEND: 서버가 `assertCanWriteChat`으로 **거부**.
- **1회 `SYSTEM`**: `chat_messages`에 저장 후 [`ChatMessageResponse`](./Sprint5-API.md) 형태로 **`/topic/chat.{roomId}`** 브로드캐스트 (`messageType`: `SYSTEM`, 발신자는 **방장** User).
- 발생 시점: 방장 **수동 종료**·**취소**, 스케줄 **자동 종료**. 채팅방이 없으면(확정 전 등) **스킵**.
- 문구 예: 종료 — `모임이 종료되어 채팅에서 새 메시지를 보낼 수 없습니다.` / 취소 — `모임이 취소되어 채팅에서 새 메시지를 보낼 수 없습니다.`

---

## 12. 알림 — FCM + STOMP

### 12.1 포그라운드

| 체크 | 항목 |
|------|------|
| ☑ | 채팅과 **동일** STOMP 세션에서 **`SUBSCRIBE /user/queue/notifications`** |
| ☑ | 본문: **`NotificationRealtimePayload`** (§12.3). 상세 필드는 `GET /api/notifications` |

### 12.2 백그라운드 (모바일)

| 체크 | 항목 |
|------|------|
| ☐ | Firebase 프로젝트·`google-services.json` / `GoogleService-Info.plist` |
| ☐ | FCM 토큰 수신·갱신 |
| ☑ | 로그인 후 **§2 `POST .../push-tokens`**, 로그아웃 시 **`POST .../push-tokens/revoke`** |
| ☐ | iOS: FCM ↔ APNs |

### 12.3 `NotificationRealtimePayload` (STOMP JSON)

| 필드 | 타입 | 설명 |
|------|------|------|
| notificationId | number | PK |
| recipientUserId | number | 수신자 |
| type | string | [`NotificationType`](./Sprint5-API.md#43-notificationtype-enum) (Sprint 8: `FRIEND_CREATED_MATCH`, `FRIEND_CONFIRMED_PARTICIPATION` 포함) |
| title | string | 제목 |
| summary | string | 본문 요약(알림 `body`와 동일) |
| relatedMatchId | number \| null | |
| relatedParticipantId | number \| null | |

### 12.4 FCM (네이티브 `data` 맵)

서버가 **`FCM_ENABLED=true`** 이고 Firebase 초기화가 된 경우에만 발송. 알림 표시용 `notification` 제목/본문 + 아래 **data** 문자열 키:

| 키 | 값 |
|----|-----|
| notificationId | 문자열 숫자 |
| type | enum 이름 |
| relatedMatchId | 있을 때만 문자열 숫자 |
| relatedParticipantId | 있을 때만 문자열 숫자 |

무효 토큰 응답 시 서버가 DB 토큰 **비활성화**. 전송 실패는 로그 및 최대 **3회** 지수 백오프 재시도.

### 12.5 알림 생성 → 단말 경로 (백엔드)

1. 비즈니스 로직 `NotificationService.publishAfterCommit` → 트랜잭션 커밋 후 `notifications` INSERT.  
2. INSERT 직후 `@Async`: `convertAndSendToUser(userId, "/queue/notifications", payload)` + (FCM 준비 시) 기기별 `send`.

---

## 13. Redis (서버 인프라)

프론트는 **Redis에 직접 연결하지 않는다**. WS URL은 항상 **`/ws-chat`**.

| 항목 | 설명 |
|------|------|
| 릴레이 off | 인메모리 SimpleBroker (`minton.chat.stomp-redis-relay.enabled=false`) |
| 릴레이 on | `spring.data.redis` + `STOMP_REDIS_RELAY_ENABLED=true` 등. 브로커 outbound `/topic/*`, `/queue/*`, `/user/*` 를 Redis Pub/Sub 단일 채널(기본 `minton:chat:stomp-relay`)로 중계해 **다중 인스턴스** 간 구독자에게 전달 |

자세한 env는 **§14** 참고.

---

## 14. 운영 환경변수 요약

`application-prod.yml`과 맞춘 **대표 변수**다. 실제 값은 시크릿·Parameter Store 등에 두고 주입한다.

| 변수(예) | 용도 |
|----------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL |
| `JWT_SECRET` | HS256, **필수**, 충분한 길이 |
| `JWT_EXPIRATION_MS` | 선택(기본 86400000 등) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `https://app.example.com` — **복수는 쉼표** |
| `OAUTH_REDIRECT_URI` | OAuth 콜백 |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` | STOMP 릴레이·(기타) Redis |
| `STOMP_REDIS_RELAY_ENABLED` | 다중 replica면 `true` |
| `STOMP_REDIS_RELAY_CHANNEL` | 선택(기본 채널명 유지 시 생략) |
| `FCM_ENABLED` | 푸시 사용 시 `true` |
| `FCM_CREDENTIALS_PATH` 또는 `GOOGLE_APPLICATION_CREDENTIALS` | Firebase 서비스 계정 JSON |
| `AWS_*` | S3 업로드 등(기존 앱 설정) |

---

## 15. 백엔드 구현 직후 팀이 맞출 것

- STOMP `CONNECT` 헤더 → §5.1 확정.
- 푸시 토큰 REST → **§2** · [Sprint5 §4.6](./Sprint5-API.md#46-push-토큰-fcm).
- `ErrorResponse` / STOMP `ERROR` **코드 목록**을 프론트와 공유할지(채팅·알림 공통 여부).
- 운영 **OAuth redirect 허용 목록**(`app.oauth2.allowed-redirect-uris`)에 웹 프로덕션 URL 추가 여부.

---

## 16. 참고 링크

| 문서 | 내용 |
|------|------|
| [Sprint5-API.md](./Sprint5-API.md) | 채팅·알림 REST 전체 |
| [README.md](./README.md) | 공통 응답 |
| [`docs/Sprint6-할일.md`](../../docs/Sprint6-할일.md) | Step·합의 §A~§F |
