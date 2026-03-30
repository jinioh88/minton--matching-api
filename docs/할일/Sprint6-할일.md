# Sprint 6 할일 문서 (백엔드) — 실시간 채팅·Redis·알림 푸시(후속)

> **전제(Sprint 5 완료)**: `docs/Sprint5-할일.md` Step 1~6 — REST 채팅(`ChatService`, `ChatRoomService`), `chat_messages` 저장, 권한(방장 / `ACCEPTED`), 인앱 알림 DB·REST, `common-docs/api/Sprint5-API.md`.
>
> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/Sprint5-할일.md`(채팅 아키텍처 비교 절), `docs/인증-권한-패턴.md`(WebSocket 인증 시)

## Sprint 6 목표 (본 문서 범위)

- **WebSocket + STOMP** 로 채팅 메시지를 실시간 전달하되, **소스 오브 트루스는 `chat_messages` + `MatchParticipant`** 유지.
- **Redis Pub/Sub** 메시지 브로커(relay)로 다중 인스턴스 간 동일 topic·user 큐 구독자에게 전달 — **Step 2 포함**(아래 **§F**). 로컬·단일 인스턴스는 **SimpleBroker** 유지(**프로파일 분기**).
- **알림 푸시(FCM/APNs) 및/또는 실시간 수신 채널** — **Sprint 6 Step 3로 포함**(아래 **§E**). Sprint 5는 인앱 DB + REST 폴링만 해당([`Sprint5-API.md` §1.3](../common-docs/api/Sprint5-API.md)).

### 완료 조건 (요약)

- [x] STOMP 연결·구독 시 **REST와 동일한 채팅 접근 규칙**(`assertCanAccessChat` 계열) 적용
- [x] STOMP로 수신한 메시지는 **DB 저장 후** 브로드캐스트
- [x] `FINISHED` / `CANCELLED` 매칭은 STOMP 전송 거부
- [x] (선택) 종료·취소 시 `SYSTEM` 등 **1회** topic 브로드캐스트
- [x] `common-docs/api`에 실시간 채팅·프론트 연동 반영 — 초안: [`Sprint6-API.md`](../common-docs/api/Sprint6-API.md) (구현 확정 시 STOMP 헤더·기기 토큰 API 등 갱신)
- [x] **Redis relay** — 프로파일 분기로 단일 인스턴스는 SimpleBroker 유지, 다중 replica에서는 Redis Pub/Sub 릴레이(**§F**)
- [x] 알림 푸시·실시간 수신 — Step 3 (**§E**)

### Sprint 6 프론트·백엔드 합의 초안

#### A. 연결·인프라

| ID | 항목 | 결정 |
|----|------|------|
| **A1** | URL | REST와 **동일 호스트**, 경로 **`/ws-chat`** (로컬 예: `ws://localhost:8080/ws-chat`, 운영 클라이언트는 `wss://{도메인}/ws-chat` 등 배포 URL에 맞춤) |
| **A2** | SockJS | **웹: SockJS + STOMP**, **앱: 순수 WebSocket + STOMP**; 서버는 SockJS 옵션 포함. 동일 STOMP 계약. |
| **A3** | JWT | **쿼리스트링 미사용**. **STOMP `CONNECT` 프레임 헤더**로 전달 (예: `Authorization: Bearer <access_token>` — 최종 헤더명은 구현·문서와 정합). |
| **A4** | 만료 | **만료 전 갱신** 목표. **리프레시 토큰·갱신 API가 구현된 뒤** 완성; 미구현 구간에는 WS 끊김 시 재연결·UX는 임시 정책으로 처리. |
| **A5** | 환경 | **로컬·운영만**(스테이징 없음). 로컬 포트 **8080 고정**. 운영은 앱 **8080** 기동 전제; 대외는 리버스 프록시 시 **443 → 8080**일 수 있음 — 클라이언트 실제 origin은 배포에 맞게 문서화. |

#### B. STOMP 계약

##### B1 — 구독·식별·권한

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 구독 경로 (Topic) | `/topic/chat.{roomId}` | 예: `/topic/chat.105` |
| ID 타입 | Number (Long) | DB PK와 동일하게 유지 |
| 권한 검증 | 구독 시점에 해당 `roomId` 접근 권한 체크 | `ChannelInterceptor` 등에서 수행, REST `assertCanAccessChat` 계열과 동일 규칙 |

##### B2 — 전송(SEND)·본문

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| Application Prefix | `/app` | 서버 `@MessageMapping` 처리용 접두어 |
| SEND Destination | `/app/chat/messages` | 메시지 발송 공통 경로 (`@MessageMapping("/chat/messages")` 와 정합) |
| Payload (Body) | JSON: `roomId` (Long, 필수), `content` (String, 필수), `messageType` (선택) 등 | [`Sprint5-API.md` §3.6 `ChatMessageSendRequest`](../common-docs/api/Sprint5-API.md)와 동일 제약·의미; REST는 `roomId`가 경로에 있으나 STOMP는 **본문에 포함** |

##### B3 — 수신(브로드캐스트)·타입

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| Payload 스키마 | [`ChatMessageResponse`](../common-docs/api/Sprint5-API.md)와 동일 | 의미상 `id`, `content`, 발신자(`senderId`·`senderNickname` 등), `messageType`, `createdAt` 등 — 비고의 `sender(id, nick, img)`는 UI 관점 예시이며, **실제 JSON 키는 Sprint 5 스키마를 따르고** 프로필 이미지 등 추가 필드가 필요하면 Sprint 6 API 절에서 `ChatMessageResponse` 확장과 함께 정의 |
| Message Type | `TEXT`, `SYSTEM` (필요 시 `ENTER`, `LEAVE` 추가) | 문자열 enum으로 전송; 기존 REST의 `IMAGE` 등과 공존 시 Sprint 6 API 표에 전체 값 나열 |
| 시스템 메시지 | 동일 Topic `/topic/chat.{roomId}` 사용 | 클라이언트는 `messageType`에 따라 UI 분기 |

##### B4 — 에러·권한 거부

| 에러 유형 | 전달 방식 | 클라이언트 동작 |
|-----------|-----------|-----------------|
| 인증·프로토콜 오류 | STOMP `ERROR` 프레임 | 연결 종료 및 재로그인 유도 |
| 비즈니스 로직 오류 | `/user/queue/errors` (유니캐스트) | `code`, `message` 표시 후 **연결 유지** |

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 데이터 포맷 (`/user/queue/errors`) | REST [`ErrorResponse`](../common-docs/api/README.md#공통-응답-형식)와 동일 규칙 | JSON 예: `{ "success": false, "message": "...", "code": "..." }` — Sprint 6 API 절에서 코드 목록을 REST와 교차 참조 |

##### B5 — 매칭 종료(`FINISHED` / `CANCELLED`)

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 브로드캐스트 여부 | **포함** (상태 전환 시 **1회** 전송) | 매칭이 `FINISHED` 또는 `CANCELLED`로 바뀔 때 |
| 전달 경로 | `/topic/chat.{roomId}` | 기존 채팅 topic 재사용 |
| 페이로드 형태 | [`ChatMessageResponse`](../common-docs/api/Sprint5-API.md) 스키마 준수 | `messageType`: `SYSTEM`으로 구분; `chat_messages`에 동일 행으로 남길지·가상 이벤트만 보낼지는 구현 시 정책 확정 |
| 클라이언트 대응 | 입력 비활성화 및 안내 UI | **소켓 연결 유지 여부는 프론트 재량** (일반적으로 유지 권장) |

#### C. REST·화면 연동

##### C1 — 히스토리·진입

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 히스토리 데이터 소스 | REST API (MySQL) | STOMP는 **실시간 수신**만 담당 |
| 페이징 방식 | Cursor-based Pagination | [`Sprint5-API.md` §3.5 `GET .../messages`](../common-docs/api/Sprint5-API.md) `cursor`·`size`; 중복 방지·대용량 대응 |
| 최초 진입 로직 | 방 상세 API + 메시지 목록 API 호출 | [`§3.4`](../common-docs/api/Sprint5-API.md) `GET .../rooms/{roomId}`(또는 `GET .../matches/{matchId}/chat`) + [`§3.5`](../common-docs/api/Sprint5-API.md) 목록; 프론트에서 병합 렌더링 |

##### C2 — 연결·구독 타이밍(화면별)

| 화면 위치 | WebSocket 연결 | 전역 알림 구독 | 상세 채팅 구독 | 비고 |
|-----------|----------------|----------------|----------------|------|
| 채팅 목록 | Connected | Subscribed | — | 목록 숫자 실시간 업데이트 |
| 채팅 상세 | Connected | Subscribed | Subscribed | 다른 방 알림은 백그라운드 수신 |
| 뒤로가기 시 | 유지 | 유지 | Unsubscribed | 즉시 최신 목록 노출 가능 |

- **전역 알림 구독**: B절의 `/topic/chat.{roomId}`와 별개 — **destination·페이로드**(예: 미읽음·마지막 메시지 요약)는 Sprint 6 백엔드·`common-docs`에 **추가 정의** 필요.

##### C3 — 폴링·동기화

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 상시 폴링 여부 | **중단** (Disable) | WebSocket 연결 유지 시 불필요 — [`Sprint5-API.md` §3.5 `afterId`](../common-docs/api/Sprint5-API.md) 폴링은 STOMP 도입 후 **상시 반복하지 않음** |
| 동기화 전략 | 재연결 시 **1회** 하이브리드 폴링 | `afterId`로 Gap 보정 후 다시 STOMP만 사용 |
| 동기화 기준 | 클라이언트가 보유한 `lastMessageId` | 유실 구간만 [`afterId`](../common-docs/api/Sprint5-API.md)로 정밀 조회 |
| 백그라운드 대응 | 앱 **포그라운드 진입** 시 소켓 재연결 및 동기화 | 모바일 OS의 소켓 절전·차단 대응 |

##### C4 — Optimistic UI

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| Optimistic UI 여부 | **적용** (Enable) | 전송 즉시 화면 렌더링 |
| 임시 ID 규칙 | `temp-{UUID}` (String) | 서버 `messageId` (Long)와 타입으로 구분 |
| 확정(Sync) 시점 | STOMP Topic 수신 시 | 본인 메시지가 `/topic/chat.{roomId}` 브로드캐스트로 돌아올 때 임시 행을 실제 행으로 치환 |
| 실패 대응 | `/user/queue/errors`(B4) 수신 시 해당 임시 행에 **재전송 / 삭제** UI | 롤백 및 사용자 알림 — B4 `ErrorResponse` 형식과 정합 |

##### C5 — 수정·삭제·실시간 반영

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 수정·삭제 요청 | REST API (`PATCH` / `DELETE`) | [`Sprint5-API.md` §3.7 · §3.8](../common-docs/api/Sprint5-API.md) 유지 |
| 실시간 전파 | STOMP Topic 브로드캐스트 | `/topic/chat.{roomId}` 재사용 — 저장·권한 검증 후 서버가 발행 |
| 메시지 타입 | `UPDATE`, `DELETE` **추가** | `ChatMessageResponse`의 `messageType` 활용; Sprint 5 enum(`TEXT`, `IMAGE`, `SYSTEM` 등)과 병합해 Sprint 6 API 절·백엔드 enum에 반영 |
| 클라이언트 대응 | 수신 `messageId`로 해당 행 UI 갱신 | 삭제 시 「삭제된 메시지입니다」 표시 또는 목록에서 제거 등 UX 정책 |

#### D. 화면·UX·복원

> A~C 절과 맞춘 **프론트 구현 참고안**. 세부 수치(백오프 상한 등)는 제품·플랫폼에 맞게 조정.

##### D1 — 이탈·구독 정리

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 상세 채팅 이탈(뒤로가기 등) | `/topic/chat.{roomId}` **Unsubscribe** | **C2절**: WebSocket·전역 알림 구독은 **유지** |
| 채팅 목록 이탈·로그아웃·앱 종료 | 전역 알림 구독 해제, **연결 종료** | 플랫폼 생명주기(`beforeunload`, 앱 `onDestroy` 등)에 맞춤 |

##### D2 — 백그라운드·탭

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 모바일 앱 | 포그라운드 진입 시 **소켓 재연결 + 동기화** | **C3절**과 동일 |
| 웹 브라우저 | **Page Visibility API**로 탭 전환 감지 | 기본 권장: 비활성 탭에서도 연결 **유지**(실시간 품질); 절전·배터리 정책으로 끊는 경우 **재포커스 시 C3 동기화** |

##### D3 — 재연결·중복 제거

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 재시도 간격 | **지수 백오프** + 최대 간격 상한 | 연속 핸드셰이크 폭주 방지 |
| 재연결 직후 | STOMP `CONNECT`에 **유효 액세스 토큰**(A3·A4), 구독 복원 | **C2절**: 목록이면 전역만, 상세면 전역+해당 방 topic |
| Gap 보정 | **C3절** `afterId` **1회** 폴링 | `lastMessageId` 기준 |
| 목록 중복 방지 | 수신 브로드캐스트의 `messageId` 기준 merge / 이미 있으면 스킵 | **C4절** 임시 행 치환과 동일 키 활용 |

##### D4 — 연결 상태·에러 표시

| 항목 | 결정 사항 | 비고 |
|------|-----------|------|
| 연결 중 | 로딩·스피너 등 비차단 UI | |
| 연결 실패 | 사용자 안내 + **재시도** 진입점 | 네트워크 오류 문구 |
| 인증·프로토콜 오류 | **B4절** STOMP `ERROR` → 연결 종료·**재로그인 유도** | |
| 비즈니스·전송 실패 | **B4절** `/user/queue/errors` + **C4절** 임시 행 액션 | 연결은 유지 |

#### E. 알림 (Step 3 — Sprint 6 포함)

| ID | 항목 | 결정 |
|----|------|------|
| **E0** | Sprint 6 범위 | **Step 3(푸시·실시간 알림)** 를 본 스프린트에 **포함**한다. |

##### E1 — 채널·페이로드

| 항목 | 선택 사항 | 기술적 근거 |
|------|-----------|-------------|
| 백그라운드 알림 | **FCM** (우선 도입) | 외부 푸시 연동·크로스 플랫폼; iOS는 **FCM → APNs** 연동(앱·Firebase 설정) 또는 이후 **APNs 직연동** 검토 |
| 포그라운드 알림 | **STOMP 통합** `/user/queue/notifications` | 채팅과 **동일 WebSocket·CONNECT**(A절) 재사용, 인증·인터셉터 단일화 |
| 알림 데이터 | **경량 JSON** (예: `notificationId`, `title`, `summary` 등) | 페이로드 최소화·실시간 목록 갱신 트리거; 상세는 기존 REST [`GET /api/notifications`](../common-docs/api/Sprint5-API.md) 등으로 보강 가능 |

- **C2절**「전역 알림 구독」과 제품 알림 스트림을 맞출 경우, destination을 **`/user/queue/notifications`** 로 통일해 구현·문서화한다.

##### E2 — 기기 토큰·멀티 기기·해지

| 항목 | 선택 사항 | 기술적 근거 |
|------|-----------|-------------|
| 토큰 등록 | **REST `POST`** (JWT 필수), 본문에 `fcmToken`(또는 `token`), `platform` (`ANDROID` \| `IOS` 등) | 현재 사용자와 바인딩; **정확한 URL·필드명**은 `common-docs` Sprint 6 알림 절에 명시 |
| 토큰 갱신 | FCM이 새 토큰을 줄 때 **동일 등록 API 재호출(Upsert)** | 기존 행 갱신 또는 구 토큰 행 비활성화 — 구현에서 중복·충돌 방지 |
| 멀티 기기 | **허용** — 사용자당 **여러 토큰** 행 | 폰·태블릿 등; 푸시 발송 시 활성 토큰 전부(또는 정책에 따라 최근 N개)에 전달 |
| 로그아웃·해지 | **`DELETE`**(경로에 token 또는 본문) 또는 **`POST .../revoke`** 로 **명시적 해지** | 로그아웃 플로우에서 호출해 **다른 계정·오기기 오발송** 방지 |
| 앱 삭제·재설치 | 재로그인 시 새 토큰 등록으로 자연 갱신; 구 토큰은 FCM 무효 응답 시 정리 | — |
| 무효 토큰 정리 | FCM **unregistered** 등 응답 시 해당 행 **비활성/삭제** | 불필요한 재시도·에러 로그 감소 |

##### E3 — 저장 후 발송·재시도

| 항목 | 선택 사항 | 기술적 근거 |
|------|-----------|-------------|
| 트리거 | 기존 `Notification` 저장 **AFTER_COMMIT** 이벤트 이후 비동기 발송 훅 연동 | DB 커밋 보장 후 발송([`Sprint5-API.md` §1.3](../common-docs/api/Sprint5-API.md) 배경) |
| 채널 | **FCM**(활성 기기 토큰) + **STOMP** `convertAndSendToUser` → `/user/queue/notifications` | E1; 포그라운드는 STOMP·백그라운드는 FCM(앱 설정에 따름) |
| 페이로드 | E1 경량 JSON + FCM `data`/`notification` 필드 매핑은 플랫폼 가이드 준수 | — |
| 실패·재시도 | **로깅** + **제한적 재시도**(백오프, 최대 횟수) | 횟수·간격은 운영 합의; 영구 실패 시 드롭·알림 |

- **E4 이하**(선택): Web 푸시(Web Push)·토큰 암호화 저장 등 — 필요 시 본 절에 추가.

#### F. Redis (Step 2 — Sprint 6 포함)

| ID | 항목 | 결정 |
|----|------|------|
| **F0** | Sprint 6 범위 | **Step 2(Redis Pub/Sub relay)** 를 본 스프린트에 **포함**한다. |
| **F1** | 브로커 전환 | **로컬·단일 Pod/replica**: **SimpleBroker** · **운영 다중 인스턴스**: **Redis** 메시지 브로커 — `application-*.yml`(또는 Spring 프로파일)로 **분기** |
| **F2** | 적용 대상 | 채팅 `/topic/*`·`/user/queue/*`(알림 등) **STOMP 브로드캐스트 중계**에 relay 적용 |
| **F3** | 비브로커 용도 | 미읽음 수 **캐시** 등은 **선택** — Step 2의 Redis는 **메시지 relay 우선** |

---

## Step 1: 실시간 채팅 (WebSocket + STOMP)

> **전제**: Sprint 5 Step 3의 **DB 저장·권한 규칙**을 재사용. STOMP는 **전달 계층**으로 두고, **소스 오브 트루스는 여전히 `chat_messages` + `MatchParticipant`**.

### Step 1.1: 인프라·설정

- [x] `WebSocketMessageBrokerConfigurer` 구현 — STOMP 엔드포인트(예: `/ws-chat`), **SockJS** 옵션(웹 호환 시)
- [x] `SecurityConfig` / `StompEndpointRegistry` — WebSocket 핸드셰이크 **인증**(JWT·세션 정책은 `docs/인증-권한-패턴.md`와 정합)

### Step 1.2: 구독·발행 규칙

- [x] 구독 destination 규칙 정의(예: `/topic/chat.{roomId}` 또는 user별 큐)
- [x] **`ChannelInterceptor` / `@MessageMapping` 진입 시** `roomId`·`matchId` 기준 **`assertCanAccessChat` 동일 검증** — 비회원·비ACCEPTED 구독 차단

### Step 1.3: 메시지 핸들러

- [x] `@MessageMapping`으로 수신 → **검증 후 DB 저장** → 브로커로 방 구독자에게 전파
- [ ] (선택) `MatchParticipantService` **ACCEPTED 커밋 후** `SYSTEM` 입장 알림 이벤트 발행 — 과도한 시스템 메시지는 UX 정책으로 제한

### Step 1.4: 매칭 종료와 연동

- [x] `FINISHED`/`CANCELLED` 시 **STOMP 전송 거부**(핸들러·인터셉터에서 `Match` 상태 확인)
- [x] (선택) 종료 브로드캐스트 한 번 전송 후 클라이언트가 **연결 종료**하도록 API 문서·프론트 가이드 — **SYSTEM 메시지 1회** `chat_messages` 저장 + `/topic/chat.{roomId}` 발행([`Sprint6-API.md` §11](../common-docs/api/Sprint6-API.md#11-매칭-종료-finished--cancelled))

### Step 1.5: API 문서

- [x] STOMP destination, 페이로드 스키마, 인증 방식, 에러(권한 거부 시)를 `common-docs/api`에 **Sprint 6 실시간 채팅** 절로 추가

---

## Step 2: Redis 연동 — Step 1 후속

> **Sprint 6 범위**: 본 Step을 **이번 스프린트에 포함**한다(상단 합의 초안 **§F**).

- [x] Spring **Redis 메시지 브로커**(Pub/Sub relay) 설정 — 인스턴스 간 동일 `topic`·`user` destination 구독자에게 메시지 전달
- [x] 로컬/단일 Pod는 **SimpleBroker 유지**, 운영 다중 replica에서 **Redis** 켜는 **프로파일·환경변수 분기** 적용
- [ ] (선택) 읽지 않음 수 등 **캐시** — **§F3**; relay와 목적 분리

---

## Step 3: 알림 푸시·실시간 수신

> **Sprint 6 범위**: 본 Step을 **이번 스프린트에 포함**한다(상단 합의 초안 **§E**).
>
> **현황(Sprint 5)**: `notifications` 테이블 저장 후 클라이언트가 `GET /api/notifications` · `unread-count` 등으로 **폴링**(또는 화면 진입 시 갱신). 서버→단말 **푸시·구독 채널 없음**([`Sprint5-API.md` §1.3](../common-docs/api/Sprint5-API.md)). Step 3 완료 시 요구사항의「실시간 푸시」에 **더 가까운 UX**를 목표로 한다.

### Step 3.1: 방향·인프라

- [x] **FCM/APNs 등 모바일 푸시**와 **SSE·WebSocket(사용자별 알림 스트림)** 등 후보 중 우선순위·조합 합의(인프라·비용·앱 백그라운드 정책) — **합의: FCM + 기존 STOMP `/user/queue/notifications`** (본 문서 상단 **§E**)

### Step 3.2: 백엔드·연동

- [x] (푸시) 기기 토큰 등록·갱신·해지 API 및 사용자 연동
- [x] `Notification` 저장(기존 `AFTER_COMMIT` 경로) 이후 **푸시 발송** 또는 **실시간 채널 브로드캐스트** 연동 — 실패 시 로깅·재시도 정책은 팀 합의

### Step 3.3: 제품 백로그·문서

- [x] `docs/제품백로그.md`에 User Story ID 부여(예: B6-8) 및 Epic 6 표 반영
- [x] `common-docs/api`에 푸시·실시간 알림 절 추가(인증, 페이로드, 권한)

---

## Sprint 6 작업 요약

| Step | 내용 | 비고 |
|------|------|------|
| 1 | 실시간 채팅 WebSocket + STOMP | Sprint 5 REST·폴링과 병행 또는 전환 정책은 팀 합의 |
| 2 | Redis Pub/Sub 브로커(relay) | **Sprint 6 포함**(**§F**); 프로파일로 SimpleBroker 병행 |
| 3 | 알림 푸시(FCM/APNs)·실시간 수신 채널 | **Sprint 6 포함**(**§E**); Step 1·2와 병렬 검토 가능 |

---

## 의존성 및 진행 순서

```
Sprint 5 (채팅 REST·인앱 알림 REST) 완료
  → Step 1 (STOMP)
  → Step 2 (Redis, **§F**)
  → Step 3 (알림 푸시·실시간, **§E** 포함)
```

- **에자일 워크플로우**: Step 단위 구현 후 사용자 리뷰(승인) 뒤 다음 Step 진행 (`docs/할일.md` 규칙과 동일).

---

## 예상 신규/수정 클래스

- `config/WebSocketConfig`(또는 `WebSocketMessageBrokerConfigurer` 구현체)
- `chat/stomp/*` — 브로커·인터셉터·`@MessageMapping` 핸들러
- `chat/event/MatchChatClosedEventListener`, `match/event/MatchChatClosedEvent`, `ChatService.publishMatchTerminalSystemMessageAndBroadcast` — 매칭 터미널 SYSTEM 안내
- `config/SecurityConfig` — STOMP/WebSocket 보안
- (Step 2) `RedisStompRelayConfiguration`·`RedisStompRelayBrokerInterceptor`·`application-prod` Redis·`minton.chat.stomp-redis-relay`
- (Step 3) `DevicePushToken`·`PushTokenService`·`POST .../push-tokens`·`FcmPushClient`·`NotificationOutboundService`·`NotificationDispatchListener` 연동

---

## Sprint 5·6와의 경계

- **Sprint 5**: REST 채팅·알림·`Sprint5-API.md` — 완료 후 본 스프린트 착수.
- **Sprint 6(본 문서)**: 실시간 채팅(STOMP)·Redis relay(**Step 2**, **§F**)·알림 푸시·실시간(**Step 3**, **§E**).
- **그 외 후보**(`docs/Sprint5-할일.md` § Sprint 4·6와의 경계): 전역 API 인가 강화(B7-2) 등은 백로그·플래닝에서 별도 작업으로 쪼개도 됨.

---

## 프론트엔드 참고 (별도 프로젝트)

**실시간(Step 1 연계 시 권장)**

- **연결 생명주기**: 채팅 화면 진입 시 WebSocket 연결·STOMP 구독, 이탈 시 `unsubscribe` 및 연결 종료.
- **상태·캐시**: TanStack Query / SWR 등으로 **커서 기반 과거 메시지**(기존 REST)와 **실시간 수신 메시지**를 하나의 리스트 모델로 병합.
- **Optimistic UI**: 전송 직후 로컬에 임시 행 표시 → 서버/브로커 확인 후 확정 또는 실패 롤백.
- **무한 스크롤**: 상단 스크롤 시 `cursor`로 이전 페이지 로드(Sprint 5 REST API와 동일).
- **종료 매칭**: 서버에서 전송 거부·종료 이벤트 수신 시 입력 비활성화 및 소켓 정리.
