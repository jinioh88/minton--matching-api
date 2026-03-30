# Sprint 5 API 문서 — 채팅(REST) · 인앱 알림

> 프론트엔드 연동용 명세. **채팅**은 Sprint 6에서 **WebSocket + STOMP** 실시간을 쓸 수 있다([Sprint6-API.md](./Sprint6-API.md)). **알림**은 DB·REST가 소스 오브 트루스이며, Sprint 6 Step 3 이후 **STOMP `/user/queue/notifications`** 및 **FCM 푸시**(설정 시)로 단말 전달이 추가된다(상세는 Sprint6 §12).  
> 참조: [Sprint1-API.md](./Sprint1-API.md) ~ [Sprint4-API.md](./Sprint4-API.md), `docs/요구사항분석.md`, `docs/Sprint5-할일.md`, `docs/Sprint6-할일.md`

---

## 1. 프론트를 위한 아키텍처 개요

### 1.1 역할 분리

| 영역 | 저장소 | 인증 | 비고 |
|------|--------|------|------|
| **채팅** | `chat_rooms`, `chat_messages` | JWT 필수 | 방장 또는 해당 매칭 **ACCEPTED** 만 접근·조회. **쓰기**는 매칭이 `FINISHED` / `CANCELLED` 이면 불가. **타인 메시지 수신은 폴링 등 REST 재조회**만 해당(실시간 아님). |
| **알림** | `notifications` (+ `device_push_tokens`) | JWT 필수 | 트랜잭션 **커밋 후** 비동기로 INSERT. 수신자 본인만 목록·읽음·푸시 토큰 등록. 실시간·푸시는 [Sprint6 §12](./Sprint6-API.md#12-알림--fcm--stomp). |

### 1.2 데이터 흐름 (개념도)

```mermaid
flowchart LR
  subgraph client[클라이언트]
    UI[앱/웹]
  end
  subgraph api[Spring API]
    ChatCtrl["/api/chat/*"]
    NotiCtrl["/api/notifications/*"]
    MatchCtrl["GET /api/matches/{id}/chat"]
  end
  subgraph domain[도메인 서비스]
    ChatSvc[ChatService / ChatRoomService]
    NotiSvc[NotificationService]
  end
  subgraph db[(MySQL)]
    CR[(chat_rooms)]
    CM[(chat_messages)]
    N[(notifications)]
  end
  UI -->|JWT| ChatCtrl
  UI -->|JWT| NotiCtrl
  UI -->|JWT| MatchCtrl
  ChatCtrl --> ChatSvc
  MatchCtrl --> ChatSvc
  NotiCtrl --> NotiSvc
  ChatSvc --> CR
  ChatSvc --> CM
  NotiSvc --> N
```

### 1.3 알림 생성 경로 (백엔드 내부)

참여·대기열·취소 등이 `NotificationService.publishAfterCommit` → `@TransactionalEventListener(AFTER_COMMIT)` → 별도 트랜잭션으로 `notifications` INSERT. INSERT 직후 서버가 **STOMP** `convertAndSendToUser`·(FCM 켜짐 시) **푸시**로 단말에도 보낸다([Sprint6-API.md](./Sprint6-API.md) §12). 과거 메시지·목록은 여전히 **`GET /api/notifications`** · `unread-count` 가 기준이며, 폴링은 STOMP 미사용 클라이언트용으로만 보면 된다.

### 1.4 채팅 — 메시지 확인 방식 (비실시간)

- **발신자 본인**만 **POST 응답 본문**으로 방금 전송한 메시지를 확인한다(HTTP 요청–응답 한 번의 결과이지, 상대방에게 실시간 전달되는 것은 아님).
- **다른 참가자** 화면에 새 메시지를 반영하려면 **`GET .../messages?afterId={마지막 messageId}` 폴링**(또는 주기적 재조회)이 필요하다(아래 **3.5절** `GET /api/chat/rooms/{roomId}/messages` 참고).

---

## 2. 인증 · 공통

- **헤더**: `Authorization: Bearer <access_token>` (기존 스프린트와 동일).
- **성공/실패 래퍼**: [README 공통 응답](./README.md#공통-응답-형식) — 성공 `ApiResponse`, 비즈니스/검증 실패 `ErrorResponse` (`success`, `message`, `code`).
- **페이지 응답**: 목록 API는 Spring `Page` 직렬화(`content`, `totalElements`, `totalPages`, `number`, `size` 등).

---

## 3. 채팅 API (`/api/chat`)

### 3.1 권한 규칙 (필수 이해)

| 동작 | 허용 주체 | 추가 조건 |
|------|-----------|-----------|
| 방 목록·상세·메시지 **조회** | 매칭 **방장** 또는 해당 매칭 참가 상태 **ACCEPTED** | 채팅방은 첫 확정 시 생성됨 — 없으면 `CHAT_ROOM_NOT_FOUND` |
| 메시지 **전송·수정·삭제** | 위와 동일 | 매칭 상태가 `FINISHED` 또는 `CANCELLED` 이면 `INVALID_MATCH_STATUS` |
| 메시지 **수정·삭제** | **본인이 보낸 메시지** | 수정은 발송 후 **15분 이내** (`BAD_REQUEST` 초과 시) |

### 3.2 API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/chat/rooms` | 내 채팅방 목록(방장 또는 ACCEPTED 매칭만, `roomId` 내림차순 페이지) |
| GET | `/api/chat/rooms/{roomId}` | 채팅방 상세: 공지 헤더 + 최근 메시지 1건 |
| GET | `/api/matches/{matchId}/chat` | 동일 본문을 **matchId** 로 조회 (진입 UX용) |
| GET | `/api/chat/rooms/{roomId}/messages` | 메시지 목록(커서 과거 조회 / `afterId` 폴링) |
| POST | `/api/chat/rooms/{roomId}/messages` | 메시지 전송 |
| PATCH | `/api/chat/rooms/{roomId}/messages/{messageId}` | 본인 메시지 수정 |
| DELETE | `/api/chat/rooms/{roomId}/messages/{messageId}` | 본인 메시지 소프트 삭제 |

### 3.3 `GET /api/chat/rooms`

**쿼리**: `page`(기본 0), `size`(기본 20)

**`data.content[]` 항목 (`ChatRoomListItemResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| matchId | number | 매칭 ID |
| roomId | number | 채팅방 ID |
| matchTitle | string | 매칭 제목 |
| lastMessagePreview | string \| null | 마지막 **노출** 메시지 본문 최대 120자(초과 시 `…`) |
| lastMessageAt | string \| null | ISO-8601 local date-time, 마지막 메시지 시각 |

### 3.4 `GET /api/chat/rooms/{roomId}` · `GET /api/matches/{matchId}/chat`

**`data` (`ChatRoomDetailResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | number | |
| matchId | number | |
| matchNotice | object | 채팅 상단 공지 ([`MatchChatNoticeResponse`](#matchchatnoticeresponse)) |
| lastMessage | object \| null | [`ChatMessageResponse`](#chatmessageresponse) |

#### `MatchChatNoticeResponse`

| 필드 | 타입 | 설명 |
|------|------|------|
| matchId | number | |
| title | string | |
| matchDate | string | `YYYY-MM-DD` |
| startTime | string | `HH:mm:ss` 등 (LocalTime 직렬화) |
| durationMin | number \| null | |
| locationName | string \| null | |
| costPolicy | string enum | `SPLIT_EQUAL`, `HOST_PAYS`, `GUEST_PAYS` |
| status | string enum | `RECRUITING`, `CLOSED`, `FINISHED`, `CANCELLED` |

### 3.5 `GET /api/chat/rooms/{roomId}/messages`

**쿼리**

| 파라미터 | 설명 |
|----------|------|
| `cursor` | 선택. 없으면 **최신** 쪽에서 `size` 개(시간 오름차순으로 반환). 있으면 해당 `messageId` **보다 오래된** 쪽 다음 페이지. |
| `afterId` | 선택. **폴링용**. 해당 id보다 **큰 id** 메시지만 오름차순, 최대 `size`개. 응답 `nextCursor` 는 항상 `null`. |
| `size` | 기본 30, 최소 1 · 최대 100으로 클램프 |

`cursor` 와 `afterId` 를 **동시에 쓰지 않는 것**을 권장(우선순위: `afterId` 가 있으면 폴링 분기).

**`data` (`ChatMessagePageResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| messages | array | [`ChatMessageResponse`](#chatmessageresponse) |
| nextCursor | number \| null | 더 과거가 있으면 다음 요청의 `cursor` 값 |

#### `ChatMessageResponse`

| 필드 | 타입 | 설명 |
|------|------|------|
| messageId | number | |
| roomId | number | |
| senderId | number | |
| senderNickname | string \| null | |
| content | string \| null | 삭제된 메시지는 목록에서 제외(소프트 삭제) |
| messageType | string enum | `TEXT`, `IMAGE`, `SYSTEM` |
| createdAt | string | date-time |
| editedAt | string \| null | 수정 시각 |

### 3.6 `POST /api/chat/rooms/{roomId}/messages`

**본문 (`ChatMessageSendRequest`)**

| 필드 | 제약 | 설명 |
|------|------|------|
| content | 필수, 공백 불가, 최대 1000자 | 앞뒤 trim 저장 |
| messageType | 선택 | 없으면 `TEXT` |

### 3.7 `PATCH /api/chat/rooms/{roomId}/messages/{messageId}`

**본문 (`ChatMessagePatchRequest`)**: `content` (동일 제약)

### 3.8 `DELETE /api/chat/rooms/{roomId}/messages/{messageId}`

**응답**: `data` 는 `null`, 성공 시 `success: true`

---

## 4. 알림 API (`/api/notifications`)

### 4.1 API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications` | 내 알림 페이지 (`createdAt` 내림차순) |
| GET | `/api/notifications/unread-count` | 미읽음 건수 (`data` = number) |
| PATCH | `/api/notifications/{notificationId}/read` | 단건 읽음 (`readAt` 설정) |
| POST | `/api/notifications/read-all` | 미읽음 일괄 읽음 (`data` = 갱신된 행 수 int) |
| POST | `/api/notifications/push-tokens` | FCM 토큰 등록·갱신(Upsert), JWT 필수 ([§4.6](#46-push-토큰-fcm)) |
| POST | `/api/notifications/push-tokens/revoke` | 토큰 해지(로그아웃 등), JWT 필수 ([§4.6](#46-push-토큰-fcm)) |

### 4.2 `GET /api/notifications`

**쿼리**: `page`(기본 0), `size`(기본 20)

**`data.content[]` (`NotificationResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| notificationId | number | |
| type | string | [`NotificationType`](#43-notificationtype-enum) |
| title | string | |
| body | string \| null | |
| payload | string \| null | **JSON 문자열** 확장용. 현재 MVP 에서는 대부분 `null` |
| relatedMatchId | number \| null | 매칭 상세·채팅 진입 등에 사용 |
| relatedParticipantId | number \| null | 참여 레코드 연결 |
| readAt | string \| null | 읽음 시각 |
| createdAt | string | 생성 시각 |

**미읽음 판별**: `readAt == null`

### 4.3 `NotificationType` enum

| 값 | 수신자 | 발생 맥락(요약) |
|----|--------|-----------------|
| `MATCH_APPLICATION` | 방장 | 누군가 매칭에 신청·재신청 |
| `PARTICIPATION_ACCEPTED` | 신청자 | 방장 수락 또는 예약 수락 확정 |
| `PARTICIPATION_REJECTED` | 신청자 | 방장 거절 |
| `WAITLIST_SLOT_OFFER` | 대기 1순위 | 순차 승격으로 RESERVED 부여, 본문에 수락 제한 **분** 안내 |
| `WAITLIST_EMERGENCY_OPEN` | WAITING 전원 | 긴급 모드 선착순 안내 |
| `MATCH_CANCELLED` | ACCEPTED 참가자(방장 제외) | 방장이 매칭 취소 |
| `FRIEND_CREATED_MATCH` | 내가 팔로우한 사용자(호스트)의 팔로워 각각 | 팔로우 대상이 새 매칭 생성 |
| `FRIEND_CONFIRMED_PARTICIPATION` | 내가 팔로우한 사용자(참여자)의 팔로워 각각 | 팔로우 대상이 매칭 참여 ACCEPTED 확정 |

상세(타깃팅·딥링크·문구): [Sprint8-API.md](./Sprint8-API.md) §3.

### 4.4 `payload` · 딥링크 (MVP)

- 스키마는 **고정 필드가 아님**: 향후 `{"deepLink":"/matches/123"}` 형태 등 JSON 문자열로 확장 가능.
- 현재 구현의 `NotificationDispatchCommand.of(...)` 는 `payload` 를 넣지 않으므로 **`null` 이 일반적**이다. 화면 이동은 `relatedMatchId` · `type` 조합으로 처리하면 된다.

### 4.5 알림 `title` / `body` 예시 (실제와 동일한 패턴)

아래는 백엔드에서 쓰는 문구 패턴이다(매칭 제목은 최대 200자 근처에서 잘림).

**`MATCH_APPLICATION`**

- title: `새 참여 신청`
- body: `'{매칭제목}' 매칭에 {닉네임}님이 신청했습니다.` (닉네임 없으면 `회원`)

**`PARTICIPATION_ACCEPTED`** (방장 수락)

- title: `참여가 확정되었습니다`
- body: `'{매칭제목}' 매칭 신청이 수락되었습니다.`

**`PARTICIPATION_ACCEPTED`** (예약 수락)

- body: `'{매칭제목}' 매칭에 확정되었습니다. 예약을 수락했습니다.`

**`PARTICIPATION_REJECTED`**

- title: `참여 신청 결과`
- body: `'{매칭제목}' 매칭 신청이 거절되었습니다.`

**`WAITLIST_SLOT_OFFER`**

- title: `참석 기회 안내`
- body: `'{매칭제목}' 매칭에 참석 기회가 생겼습니다. {N}분 내에 수락해 주세요.` (`N` = 설정 `offer-timeout` 분)

**`WAITLIST_EMERGENCY_OPEN`**

- title: `선착순 참가 안내`
- body: `'{매칭제목}' 매칭에 빈자리가 생겼습니다. 선착순으로 수락할 수 있습니다.`

**`MATCH_CANCELLED`**

- title: `모임이 취소되었습니다`
- body: `방장이 '{매칭제목}' 매칭을 취소했습니다.`

**`FRIEND_CREATED_MATCH`** · **`FRIEND_CONFIRMED_PARTICIPATION`** (Sprint 8, 수신자·`relatedMatchId`는 [Sprint8-API.md](./Sprint8-API.md) §3)

- `FRIEND_CREATED_MATCH` — title: `새 매칭`, body: `{닉네임}님이 새로운 매칭을 만들었습니다! 지금 확인해보세요.`
- `FRIEND_CONFIRMED_PARTICIPATION` — title: `참여 확정`, body: `{닉네임}님이 매칭 참여를 확정했습니다! 함께 참여하시겠어요?`

### 4.6 Push 토큰 (FCM)

| API | 본문 | 설명 |
|-----|------|------|
| `POST /api/notifications/push-tokens` | [`PushTokenRegisterRequest`](#461-pushtokenregisterrequest) | 동일 `fcmToken` 이 이미 있으면 **현재 사용자**로 소유 이전·플랫폼 갱신(멀티 기기는 행 여러 개). |
| `POST /api/notifications/push-tokens/revoke` | [`PushTokenRevokeRequest`](#462-pushtokenrevokerequest) | 본인에게 매핑된 토큰만 비활성화. 없으면 **멱등** 성공. |

#### 4.6.1 `PushTokenRegisterRequest`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| fcmToken | string | 예 | FCM 등록 토큰 (최대 512자) |
| platform | string enum | 예 | `ANDROID` \| `IOS` |

#### 4.6.2 `PushTokenRevokeRequest`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| token | string | 예 | 해지할 FCM 토큰 (최대 512자) |

---

## 5. 채팅·알림 관련 에러 코드

비즈니스 예외 시 HTTP 상태와 함께 `code` 로 아래 enum 이름이 온다.

| code | HTTP | 주로 쓰이는 API |
|------|------|-----------------|
| `CHAT_ACCESS_DENIED` | 403 | 채팅: 방장도 아니고 ACCEPTED 도 아님 |
| `CHAT_ROOM_NOT_FOUND` | 404 | roomId 불일치 또는 해당 match 에 방 없음 |
| `MESSAGE_NOT_FOUND` | 404 | messageId/roomId 불일치 또는 이미 삭제 |
| `INVALID_MATCH_STATUS` | 400 | 종료/취소 매칭에 메시지 전송 시도 등 |
| `FORBIDDEN` | 403 | 타인 메시지 수정·삭제 |
| `BAD_REQUEST` | 400 | 메시지 수정 15분 초과, 기타 |
| `VALIDATION_ERROR` | 400 | `@Valid` 실패(필드 메시지 합본) |
| `NOTIFICATION_NOT_FOUND` | 404 | 다른 사용자 알림 id 로 읽음 처리 |
| `UNAUTHORIZED` | 401 | 토큰 없음/무효 |

---

## 6. 보안 설정 요약

- `/api/chat/**`, `/api/notifications/**`, `GET /api/matches/*/chat` → **authenticated** (`SecurityConfig`).

---

## 7. Sprint 6 예고 (실시간 채팅)

Sprint 6(`docs/Sprint6-할일.md` Step 1)에서 WebSocket + STOMP 예정. 도입 후에도 **권한·저장 규칙은 동일**하게 `ChatRoomService` 검증을 재사용할 계획.
