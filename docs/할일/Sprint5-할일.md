# Sprint 5 할일 문서 (백엔드)

> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/ERD.md` §2.4, `docs/요구사항분석.md` (Phase 4·5 채팅·알림 흐름)
> **상태: 진행 예정**

## Sprint 5 목표

채팅방 생성·메시지 송수신, **인앱 알림**(저장·조회·읽음)이 동작한다. 대기열·신청·수락/거절·방장 취소 흐름과 연동되어 관련 사용자가 알림을 받을 수 있다.

### 완료 조건 (스프린트 플래닝·백로그 정합)

- [ ] **매칭 확정(ACCEPTED) 시** 해당 매칭과 **1:1**인 채팅방이 존재하며, **방장·확정 참여자**만 메시지 조회·발송 가능 (B6-1)
- [ ] 확정자가 채팅방에서 **메시지를 주고받을** 수 있다 (목록·발송·(선택) 수정·삭제) (B6-2)
- [ ] 채팅방(또는 매칭 연동) 상세에 **일시·장소·코트비 분담** 등 **공지용 요약**이 API로 내려간다 (B6-3, `docs/요구사항분석.md` Phase 4)
- [ ] **참여 신청 시 방장에게 알림**이 생성된다 (B6-4)
- [ ] **대기열에서 참여 기회**(순차 RESERVED, 긴급 모드 안내 등)가 생겼을 때 **해당 사용자(들)에게 알림**이 생성된다 (B6-5)
- [ ] 방장 **수락/거절** 시 신청자에게 `PARTICIPATION_ACCEPTED` / `PARTICIPATION_REJECTED` 인앱 알림 (B6-6)
- [ ] 방장 **모임 취소** 시 **ACCEPTED 참가자에게만** `MATCH_CANCELLED` 알림 (**방장 본인 제외**, B6-7)
- [ ] 사용자가 **알림 목록 조회·읽음 처리**를 할 수 있다 (B6-4 ~ B6-7, Phase 5 알림 아이콘)

---

## 비즈니스 규칙 요약

### 채팅방 (ChatRoom) — ERD `Matches (1) — (1) ChatRooms`

| 규칙 | 설명 |
|------|------|
| 관계 | 매칭(`Match`)당 채팅방 **최대 1개**. `ChatRoom.match` FK 유니크 |
| 생성 시점 | **제품 확정**: **첫 번째 참가자가 `ACCEPTED`가 되는 시점**에 채팅방을 **lazy 생성**한다. 방장만 있고 아직 아무도 확정되지 않았으면 `ChatRoom` 행이 없을 수 있다. 구현: `MatchParticipantService`의 ACCEPT 경로·`acceptOffer`에서 `ensureChatRoomForMatch` 호출(Step 2). *(매칭 생성 직후 방을 만드는 방식은 채택하지 않음.)* |
| 입장 권한 | `Match.host` 또는 해당 매칭에서 `ACCEPTED`인 사용자만 메시지·실시간 구독. `PENDING`/`WAITING`/`RESERVED`/`REJECTED`/`CANCELLED`는 **채팅 비허용**. “참여할 때마다 유입”은 **ACCEPTED 전환 시점**에 WebSocket 구독 허용 + (선택) `type=SYSTEM` 입장 알림으로 표현 가능. |
| 멤버 변동 | **추방(KICK)**·**확정 취소** 등으로 `ACCEPTED`가 아니게 되면 **해당 시점부터** 채팅 읽기/쓰기 차단(기존 메시지 열람 범위는 제품 정책: 전체 차단 vs 과거만 허용) |
| 매칭 종료 | 제품 방향: **종료 후 채팅방 닫힘**. 백엔드에서는 `FINISHED`/`CANCELLED` 시 **쓰기 금지**(REST·STOMP 공통) + (선택) **시스템 메시지·구독 세션 종료**. REST만 있을 때는 **읽기 허용 + 쓰기 금지**로 두고, 실시간 도입 시 **STOMP disconnect / CLOSE** 정책은 `docs/Sprint6-할일.md` Step 1에 맞춤. |

### 메시지 (Message) — ERD `Messages.type`: TEXT, IMAGE, SYSTEM

| 규칙 | 설명 |
|------|------|
| CRUD | **C**: 발송. **R**: 커서 또는 시퀀스 기반 페이징(최신 N건). **U/D**: MVP에서 선택 — 수정·삭제 시 **본인만**, 삭제는 **soft delete** 권장 |
| 이미지 | 본문에 **S3 URL**만 저장하거나 `type=IMAGE` + URL 필드 — Sprint 2 업로드 API와 정합 |
| 시스템 메시지 | (선택) 방장 변경·인원 변동 등 `type=SYSTEM` — Sprint 5 필수 아님 |
| 실시간성 | **Sprint 5 기본**: REST + **폴링**(또는 `afterId`/`createdAfter`). **후속(Sprint 6)**: Spring **WebSocket + STOMP**; 단일 인스턴스는 내장 브로커, **다중 인스턴스 시 Redis Pub/Sub** 브로커 권장 — `docs/Sprint6-할일.md`. 지속성은 **기존 MySQL `chat_messages` 유지** 권장(별도 MongoDB 불필요·이중 저장 복잡도 증가). |

### 채팅방 상단 공지 (B6-3)

| 규칙 | 설명 |
|------|------|
| 데이터 소스 | 원칙적으로 **`Match` 스냅샷**(제목, `matchDate`+`startTime`, 장소, 코트비 분담 방식 등)을 조회해 응답 — 채팅 전용 중복 컬럼은 **매칭 수정 시 자동 반영**이 되므로 최소화 |
| API | `GET /api/matches/{matchId}/chat` 또는 `GET /api/chat/rooms/by-match/{matchId}` 등 **한 번에** `roomId`, `matchSummary`(공지 필드), `lastMessage`(선택) 반환 가능하게 설계 |

### 알림 (Notification) — 인앱 저장소

| 규칙 | 설명 |
|------|------|
| 저장 | `user_id`, `type`(Enum), `title`, `body` 또는 JSON `payload`, `read_at`(nullable), `created_at`, (선택) `related_match_id`, `related_participant_id` |
| 유형(Enum) | `MATCH_APPLICATION` — 신청·재신청 시 방장 (B6-4). `PARTICIPATION_ACCEPTED` / `PARTICIPATION_REJECTED` — 방장 수락·거절 시 해당 신청자 (B6-6). `WAITLIST_SLOT_OFFER` / `WAITLIST_EMERGENCY_OPEN` — 대기열 (B6-5). `MATCH_CANCELLED` — 방장이 매칭 취소 시 **ACCEPTED 참가자만**, **방장 본인 제외** (B6-7) |
| 발송 시점 | 트랜잭션 **커밋 후** 전달이 안전하면 `@TransactionalEventListener(phase = AFTER_COMMIT)` 또는 동기 호출 후 **실패 로깅**(알림 누락이 비즈니스 실패는 아님) — 팀 합의 |
| 푸시(FCM/APNs) | Sprint 5 범위는 **DB 알림 + REST**까지. **외부 푸시**는 Sprint 6·인프라와 함께 연동해도 됨 |
| 읽음 | `PATCH /api/notifications/{id}/read`, 일괄 `POST /api/notifications/read-all` (선택) |

---

## 채팅 아키텍처 비교·본 프로젝트 권장안

> 제안해 주신 **STOMP + Redis Pub/Sub + DB 지속성** 방향은 **15명 내외 소규모 방·다수 방** 구조와 잘 맞습니다. 아래는 동일 목표를 두고 **이 코드베이스(Spring Boot·JPA·MySQL)** 에 맞게 다듬은 정리입니다.

### 규모·스택 적합성

- **동시 접속·메시지량**: 방당 최대 약 15명이면 **단일 JVM + 내장 STOMP 브로커**로도 충분한 경우가 많습니다.
- **STOMP vs Socket.io**: Spring Security·세션·핸들러 패턴과의 궁합 때문에 **백엔드가 Java/Spring이면 STOMP가 표준적**입니다. Socket.io는 주로 Node 생태계와 함께 쓸 때 이점이 큽니다.
- **지속성**: 실시간 전달과 별개로 **이력·재접속 시 조회**는 DB가 필요합니다. 이미 **`chat_messages`(MySQL)** 를 쓰는 전제이므로 **MongoDB를 추가하지 않는 것**을 권장합니다(이중 저장·일관성 비용만 증가).

### 제안하신 구성과의 정렬

| 항목 | 제안 | 본 프로젝트 권장 |
|------|------|------------------|
| WebSocket + STOMP | 유지 | `WebSocketMessageBrokerConfigurer`, `/ws-chat` 등 엔드포인트·SockJS는 **Sprint 6**(`docs/Sprint6-할일.md` Step 1)에서 구체화. |
| 브로커 | 내장 → Redis | **1인스턴스**: SimpleBroker로 시작. **수평 확장(2대+)**: **Redis Pub/Sub relay** 로 브로드캐스트 일치 — **Sprint 6 Step 2**. |
| 메시지 발송 | WS 위주 | **권장 흐름**: STOMP 수신 → **서비스에서 DB 저장(멱등·검증)** → 구독자에게 브로드캐스트. **이미지**는 Sprint 2 **REST 업로드 후 URL만** 페이로드. Sprint 5의 **POST 발송**은 폴링 MVP·클라이언트 단순화용으로 두고, Sprint 6에서 **POST 제거 또는 병행(이전 기간만)** 은 팀이 택일. |
| 자동 입장 | ACCEPTED 시 발행 | **권한의 단일 기준**은 기존과 동일: **host 또는 `ACCEPTED`**. “유입”은 **구독 허용 + (선택) `SYSTEM` 입장 메시지**로 표현. |
| 매칭 종료 | 세션 끊기·POST 차단 | **REST·STOMP 모두** `Match.status`가 `FINISHED`/`CANCELLED`면 **전송 거부**. (선택) 방 전체에 **종료 시스템 이벤트** 한 번 브로드캐스트 후 클라이언트가 연결 해제. |

### Sprint 배치

- **Sprint 5(본 문서 Step 1~6)**: REST·폴링 기반으로 **기능 완결(B6-1~B6-5 등)** — 실시간 인프라 없이도 배포 가능.
- **Sprint 6**: **실시간 채팅·Redis** — `docs/Sprint6-할일.md` 참조(기존 Step 7~8 이관).

---

## Step 1: ChatRoom·Message 엔티티 및 마이그레이션 (B6-1, B6-2)

> ERD `docs/ERD.md` §2.4 준수. 패키지: `org.app.mintonmatchapi.chat` (또는 도메인 컨벤션에 맞게).

### Step 1.1: `ChatRoom`
- [ ] `id` (PK), `match` (`@OneToOne` 또는 `@ManyToOne` + `match_id` UK)
- [ ] `createdAt` (감사)
- [ ] `ChatRoomRepository` — `findByMatchId`, `existsByMatchId`

### Step 1.2: `ChatMessage` (ERD: Messages)
- [ ] `id` (PK), `room` (FK), `sender` (FK User)
- [ ] `content` (TEXT), `messageType` (Enum: TEXT, IMAGE, SYSTEM)
- [ ] `createdAt`, (선택) `editedAt`, `deletedAt` (soft delete)
- [ ] `ChatMessageRepository` — `findByRoomIdAndIdGreaterThanOrderByIdAsc` / `Pageable` 정렬

### Step 1.3: Flyway/Liquibase
- [ ] `chat_rooms`, `chat_messages` 테이블, FK·인덱스 `(room_id, id)` 또는 `(room_id, created_at)`
- [ ] `chat_rooms.match_id` **UNIQUE**

---

## Step 2: 채팅방 자동 생성·멤버십 연동 (B6-1)

### Step 2.1: 서비스 `ChatRoomService`
- [x] `ensureChatRoomForMatch(Long matchId)` — 방이 없으면 생성, 있으면 반환
- [x] `assertCanAccessChat(Long userId, Long matchId)` — host 또는 `ACCEPTED`만 통과, 아니면 `CHAT_ACCESS_DENIED`(403)

### Step 2.2: 참여 도메인 훅
- [x] `MatchParticipantService.decideParticipant`에서 **ACCEPT** 저장 직후(같은 트랜잭션 내) `ensureChatRoomForMatch` 호출
- [x] `acceptOffer` 등 **예약 수락으로 ACCEPTED** 되는 경로에도 동일 적용
- **정책 정합**: 방장은 `MatchParticipant` 행 없이 **host**이므로, **첫 확정자(B) 이후** 생성된 방에도 **별도 member 테이블 없이** `assertCanAccessChat`만으로 방장 접근 가능

### Step 2.3: 추방·취소
- [x] `ACCEPTED` → `CANCELLED`/KICK 등으로 바뀐 사용자는 이후 `assertCanAccessChat` 실패하도록 유지 (별도 `ChatRoomMember` 테이블 없이 `MatchParticipant`가 소스 오브 트루스)

---

## Step 3: 채팅 API — 목록·발송·(선택) 수정·삭제 (B6-2)

### Step 3.1: 내 채팅방 목록
- [x] `GET /api/chat/rooms?page=&size=` (인증 필수)
  - 조건: 내가 **host**이거나 해당 매칭 **ACCEPTED**이고, `ChatRoom`이 존재하는 매칭만
  - 응답: `matchId`, `roomId`, 매칭 제목, 마지막 메시지 요약·시각(선택), 읽지 않음 수(선택·후속)
- [x] QueryDSL 목록 + JPQL 서브쿼리로 방별 최신 메시지 일괄 조회(`findLatestVisibleByRoomIdsWithSender`)

### Step 3.2: 채팅방 진입·공지 헤더 (B6-2, B6-3)
- [x] `GET /api/chat/rooms/{roomId}` 및 `GET /api/matches/{matchId}/chat` (인증 필수)
  - `assertCanAccessChat` 통과
  - **공지 블록**: `MatchChatNoticeResponse` — 일시·장소·`costPolicy`·상태 등

### Step 3.3: 메시지 조회
- [x] `GET /api/chat/rooms/{roomId}/messages?cursor=&afterId=&size=` (인증 필수)
  - **정책**: `afterId` 없을 때 `cursor` 없음 → 최신 `size`건, 응답은 **시간 오름차순**. `cursor`=messageId → 그보다 **오래된** 이전 페이지. `afterId` → 폴링용 **더 새로운** 메시지만(`nextCursor`는 null). `size` 기본 30, 최대 100.

### Step 3.4: 메시지 발송
- [x] `POST /api/chat/rooms/{roomId}/messages` — body: `content`, `messageType`(기본 TEXT)
  - 본문 최대 1000자(`@Size`, 모바일 UX). `FINISHED`/`CANCELLED` 매칭은 전송 불가.

### Step 3.5: (선택) 수정·삭제
- [x] `PATCH .../messages/{messageId}` — 발신자 본인, **생성 후 15분 이내**
- [x] `DELETE .../messages/{messageId}` — soft delete, 목록·과거 조회에서 제외

### Step 3.6: 예외·에러 코드
- [x] `CHAT_ROOM_NOT_FOUND`, `CHAT_ACCESS_DENIED`, `MESSAGE_NOT_FOUND`, `VALIDATION_ERROR`(전역 `@Valid` 실패 시 `GlobalExceptionHandler`)

### Step 3.7: 보안
- [x] `SecurityConfig`: `/api/chat/**`, `GET /api/matches/*/chat` → `authenticated()`

---

## Step 4: Notification 엔티티·Repository·마이그레이션 (B6-4, B6-5)

### Step 4.1: `Notification`
- [x] `id`, `user` (수신자), `type` (Enum), `title`, `body` 또는 `payload` (TEXT JSON 문자열)
- [x] `readAt` (nullable), `createdAt`/`updatedAt` (`BaseEntity`)
- [x] `relatedMatchId`, `relatedParticipantId` — 딥링크·화면 이동용

### Step 4.2: Repository
- [x] `findByUser_IdOrderByCreatedAtDesc(..., Pageable)`
- [x] `countByUser_IdAndReadAtIsNull`

### Step 4.3: 마이그레이션
- [x] `notifications` 테이블, 인덱스 `(user_id, created_at)` — Flyway `V5__notifications.sql`

---

## Step 5: NotificationService 및 알림 생성 연동 (B6-4 ~ B6-7)

### Step 5.1: 코어 API
- [x] `NotificationService.publishAfterCommit(NotificationDispatchCommand)` + `NotificationDispatchListener` — 커밋 후 `REQUIRES_NEW`로 INSERT
- [x] `GET /api/notifications?page=&size=`
- [x] `GET /api/notifications/unread-count`
- [x] `PATCH /api/notifications/{notificationId}/read` — `readAt` 설정
- [x] `POST /api/notifications/read-all` — 미읽음 일괄 읽음

### Step 5.2: B6-4 — 신청 시 방장 알림
- [x] `MatchParticipantService.applyParticipant` 성공 후 **매칭 방장**에게 `MATCH_APPLICATION` (`AFTER_COMMIT`)
  - 제목/본문: 매칭 제목(200자 절단)·신청자 닉네임(없으면 "회원")

### Step 5.2b: B6-6 — 수락/거절 시 신청자 알림
- [x] `decideParticipant` **ACCEPT** 후 신청자에게 `PARTICIPATION_ACCEPTED`
- [x] **REJECT** 후 신청자에게 `PARTICIPATION_REJECTED`
- [x] `acceptOffer` → `PARTICIPATION_ACCEPTED` (예약 수락 확정과 동일 UX)

### Step 5.2c: B6-7 — 방장 모임 취소 시 확정자 알림
- [x] `MatchService.cancelMatch` 성공 후 `ACCEPTED` 참가자 각각 `MATCH_CANCELLED`, **방장 제외**
- [x] (MVP) 비확정 인원 미발송

### Step 5.3: B6-5 — 대기열 기회 알림 (기존 TODO 대체)
- [x] `QueuePromotionService.promoteSequential` — `WAITLIST_SLOT_OFFER` + 설정된 분(offer-timeout) 본문
- [x] `promoteEmergency` — **WAITING 전체** `WAITLIST_EMERGENCY_OPEN`
- [x] `processExpiredReservations` → `promoteSequential` 연쇄 시에도 동일 경로로 알림 발송

### Step 5.4: 트랜잭션 경계
- [x] `@TransactionalEventListener(phase = AFTER_COMMIT)` + 알림 저장은 `REQUIRES_NEW` — 참여/매칭 커밋 후에만 INSERT, 실패 시 로깅만

### Step 5.5: (선택) Sprint 4 연동
- [ ] 매칭 자동 `FINISHED` 또는 수동 종료 시 "후기를 작성해 주세요" 알림 — Sprint 4 할일 문서의 확장 포인트

---

## Step 6: API 문서 (B1-3)

- [x] `common-docs/api/Sprint5-API.md` 신규 또는 기존 API 문서에 Sprint 5 섹션 추가
  - 채팅방 목록·상세(공지)·메시지 조회·발송·에러 코드
  - 알림 목록·읽음·타입 Enum(`MATCH_APPLICATION`, `PARTICIPATION_*`, `WAITLIST_*`, `MATCH_CANCELLED`)·payload 예시
  - 권한: host / ACCEPTED만 채팅

---

## 후속 스프린트 (Sprint 6)

**실시간 채팅(WebSocket + STOMP)·Redis 브로커** 는 `docs/Sprint6-할일.md` 로 이관했다. Sprint 5 범위는 **본 문서 Step 1~6** 까지다.

---

## Sprint 5 작업 요약

| Step | 내용 | 백로그 ID |
|------|------|-----------|
| 1 | `ChatRoom`·`ChatMessage` 엔티티·Repository·마이그레이션 | B6-1, B6-2 |
| 2 | 확정 시 채팅방 자동 생성·접근 검증 | B6-1 |
| 3 | 채팅 REST API·공지 헤더(B6-3)·폴링용 커서 | B6-2, B6-3 |
| 4 | `Notification` 엔티티·Repository·마이그레이션 | B6-4 ~ B6-7 |
| 5 | 알림 API + 신청·수락/거절·취소·대기열 연동 | B6-4 ~ B6-7 |
| 6 | API 문서 | B1-3 |

---

## 의존성 및 진행 순서

```
Step 1 (엔티티) → Step 2 (방 생성 훅) → Step 3 (채팅 API)
Step 4 (알림 엔티티) → Step 5 (알림 API + 연동)
Step 3·Step 5 일부 병렬 가능 (Step 1·4 선행 권장)
Step 6 마지막 (Sprint 5 범위)
실시간 채팅·Redis → docs/Sprint6-할일.md
```

- **에자일 워크플로우**: **Step 단위** 구현 후 사용자 리뷰(승인) 뒤 다음 Step 진행 (`docs/할일.md` 규칙과 동일).

---

## 예상 신규/수정 클래스

### 신규 (예: `org.app.mintonmatchapi.chat`, `org.app.mintonmatchapi.notification`)

- `chat/entity/ChatRoom.java`, `ChatMessage.java`, `ChatMessageType.java`
- `chat/repository/ChatRoomRepository.java`, `ChatMessageRepository.java`
- `chat/service/ChatRoomService.java`, `ChatService.java` (또는 통합)
- `chat/controller/ChatController.java`
- `chat/dto/ChatRoomResponse.java`, `ChatRoomListItemResponse.java`, `MatchChatSummaryResponse.java`, `ChatMessageRequest.java`, `ChatMessageResponse.java`
- `notification/entity/Notification.java`, `NotificationType.java`
- `notification/repository/NotificationRepository.java`
- `notification/service/NotificationService.java`
- `notification/controller/NotificationController.java`
- `notification/dto/NotificationResponse.java`
- (선택) `notification/event/NotificationEventListener.java` — AFTER_COMMIT

### 수정

- `match/service/MatchParticipantService.java` — ACCEPT 경로에서 `ChatRoomService`, 신청 후 `NotificationService`(B6-4), 수락/거절 시 신청자 알림(B6-6)
- `match/service/MatchService.java` — 방장 취소 시 ACCEPTED 참가자에게 `MATCH_CANCELLED`, 방장 제외(B6-7)
- `match/service/QueuePromotionService.java` — RESERVED·긴급 모드 알림 TODO 구현
- `config/SecurityConfig.java` — `/api/chat/**`, `/api/notifications/**` 인증
- `exception/ErrorCode.java` — 채팅·알림 관련 코드 추가
- (Sprint 6) `config/WebSocketConfig`, `chat/stomp/*` — 브로커·인터셉터·`@MessageMapping` 핸들러 — `docs/Sprint6-할일.md`

---

## Sprint 4·6와의 경계

- **Sprint 4**: 패널티·후기 API는 유지; 채팅 UI에서 방장 액션은 기존 `POST .../penalties` 호출.
- **Sprint 5**: REST 채팅·알림·문서(본 문서 Step 1~6).
- **Sprint 6**: 실시간 채팅(STOMP)·(선택) Redis — `docs/Sprint6-할일.md`. 그 외 전역 **API 인가 강화**(B7-2), **푸시 알림** 등은 백로그·플래닝에서 별도 작업으로 묶을 수 있음.

---

## 프론트엔드 참고 (별도 프로젝트)

`docs/스프린트플래닝.md` Sprint 5 프론트: 채팅 목록·채팅방(메시지·상단 공지)·알림 목록·읽음 처리.

**실시간(Sprint 6 / `docs/Sprint6-할일.md` 연계 시 권장)**

- **연결 생명주기**: 채팅 화면 진입 시 WebSocket 연결·STOMP 구독, 이탈 시 `unsubscribe` 및 연결 종료.
- **상태·캐시**: TanStack Query / SWR 등으로 **커서 기반 과거 메시지**와 **실시간 수신 메시지**를 하나의 리스트 모델로 병합.
- **Optimistic UI**: 전송 직후 로컬에 임시 행 표시 → 서버/브로커 확인 후 확정 또는 실패 롤백.
- **무한 스크롤**: 상단 스크롤 시 `cursor`로 이전 페이지 로드(Step 3.3과 동일 API).
- **종료 매칭**: 서버에서 전송 거부·종료 이벤트 수신 시 입력 비활성화 및 소켓 정리.
