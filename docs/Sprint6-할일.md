# Sprint 6 할일 문서 (백엔드) — 실시간 채팅·Redis·알림 푸시(후속)

> **전제(Sprint 5 완료)**: `docs/Sprint5-할일.md` Step 1~6 — REST 채팅(`ChatService`, `ChatRoomService`), `chat_messages` 저장, 권한(방장 / `ACCEPTED`), 인앱 알림 DB·REST, `common-docs/api/Sprint5-API.md`.
>
> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/Sprint5-할일.md`(채팅 아키텍처 비교 절), `docs/인증-권한-패턴.md`(WebSocket 인증 시)

## Sprint 6 목표 (본 문서 범위)

- **WebSocket + STOMP** 로 채팅 메시지를 실시간 전달하되, **소스 오브 트루스는 `chat_messages` + `MatchParticipant`** 유지.
- (선택) **Redis Pub/Sub** 메시지 브로커로 다중 인스턴스 간 동일 topic 구독자에게 전달.
- (선택·Epic 6 후속) **알림 푸시(FCM/APNs) 및/또는 실시간 수신 채널** — Sprint 5는 인앱 DB + REST 폴링만 해당([`Sprint5-API.md` §1.3](../common-docs/api/Sprint5-API.md)).

### 완료 조건 (요약)

- [ ] STOMP 연결·구독 시 **REST와 동일한 채팅 접근 규칙**(`assertCanAccessChat` 계열) 적용
- [ ] STOMP로 수신한 메시지는 **DB 저장 후** 브로드캐스트
- [ ] `FINISHED` / `CANCELLED` 매칭은 STOMP 전송 거부(및 선택적 종료 이벤트)
- [ ] `common-docs/api`에 실시간 채팅 절 추가
- [ ] (선택) Redis relay — 프로파일 분기로 단일 인스턴스는 SimpleBroker 유지
- [ ] (선택) 알림 푸시·실시간 수신 — Step 3

---

## Step 1: 실시간 채팅 (WebSocket + STOMP)

> **전제**: Sprint 5 Step 3의 **DB 저장·권한 규칙**을 재사용. STOMP는 **전달 계층**으로 두고, **소스 오브 트루스는 여전히 `chat_messages` + `MatchParticipant`**.

### Step 1.1: 인프라·설정

- [ ] `WebSocketMessageBrokerConfigurer` 구현 — STOMP 엔드포인트(예: `/ws-chat`), **SockJS** 옵션(웹 호환 시)
- [ ] `SecurityConfig` / `StompEndpointRegistry` — WebSocket 핸드셰이크 **인증**(JWT·세션 정책은 `docs/인증-권한-패턴.md`와 정합)

### Step 1.2: 구독·발행 규칙

- [ ] 구독 destination 규칙 정의(예: `/topic/chat.{roomId}` 또는 user별 큐)
- [ ] **`ChannelInterceptor` / `@MessageMapping` 진입 시** `roomId`·`matchId` 기준 **`assertCanAccessChat` 동일 검증** — 비회원·비ACCEPTED 구독 차단

### Step 1.3: 메시지 핸들러

- [ ] `@MessageMapping`으로 수신 → **검증 후 DB 저장** → 브로커로 방 구독자에게 전파
- [ ] (선택) `MatchParticipantService` **ACCEPTED 커밋 후** `SYSTEM` 입장 알림 이벤트 발행 — 과도한 시스템 메시지는 UX 정책으로 제한

### Step 1.4: 매칭 종료와 연동

- [ ] `FINISHED`/`CANCELLED` 시 **STOMP 전송 거부**(핸들러·인터셉터에서 `Match` 상태 확인)
- [ ] (선택) 종료 브로드캐스트 한 번 전송 후 클라이언트가 **연결 종료**하도록 API 문서·프론트 가이드

### Step 1.5: API 문서

- [ ] STOMP destination, 페이로드 스키마, 인증 방식, 에러(권한 거부 시)를 `common-docs/api`에 **Sprint 6 실시간 채팅** 절로 추가

---

## Step 2: Redis 연동 (다중 인스턴스) — Step 1 후속·선택

- [ ] Spring **Redis 메시지 브로커**(Pub/Sub relay) 설정 — 인스턴스 간 동일 `topic` 구독자에게 메시지 전달
- [ ] 로컬/단일 Pod는 **SimpleBroker 유지**, `prod` 다중 replica에서만 Redis 켜는 **프로파일 분기** 검토
- [ ] (선택) 읽지 않음 수 등은 **DB 집계 또는 캐시**로 — Redis는 **필수 아님**, 브로커 목적 우선

---

## Step 3: 알림 푸시·실시간 수신 (Epic 6 후속)

> **현황(Sprint 5)**: `notifications` 테이블 저장 후 클라이언트가 `GET /api/notifications` · `unread-count` 등으로 **폴링**(또는 화면 진입 시 갱신). 서버→단말 **푸시·구독 채널 없음**([`Sprint5-API.md` §1.3](../common-docs/api/Sprint5-API.md)). `docs/요구사항분석.md` 의「실시간 푸시」와 **동등 UX는 미구현**.

### Step 3.1: 방향·인프라

- [ ] **FCM/APNs 등 모바일 푸시**와 **SSE·WebSocket(사용자별 알림 스트림)** 등 후보 중 우선순위·조합 합의(인프라·비용·앱 백그라운드 정책)

### Step 3.2: 백엔드·연동

- [ ] (푸시) 기기 토큰 등록·갱신·해지 API 및 사용자 연동
- [ ] `Notification` 저장(기존 `AFTER_COMMIT` 경로) 이후 **푸시 발송** 또는 **실시간 채널 브로드캐스트** 연동 — 실패 시 로깅·재시도 정책은 팀 합의

### Step 3.3: 제품 백로그·문서

- [ ] `docs/제품백로그.md`에 User Story ID 부여(예: B6-8) 및 Epic 6 표 반영
- [ ] `common-docs/api`에 푸시·실시간 알림 절 추가(인증, 페이로드, 권한)

---

## Sprint 6 작업 요약

| Step | 내용 | 비고 |
|------|------|------|
| 1 | 실시간 채팅 WebSocket + STOMP | Sprint 5 REST·폴링과 병행 또는 전환 정책은 팀 합의 |
| 2 | Redis Pub/Sub 브로커(다중 인스턴스) | 선택 |
| 3 | 알림 푸시(FCM/APNs)·실시간 수신 채널 | 선택, Step 1·2와 병렬 검토 가능 |

---

## 의존성 및 진행 순서

```
Sprint 5 (채팅 REST·인앱 알림 REST) 완료
  → Step 1 (STOMP)
  → Step 2 (Redis, 선택)
  → Step 3 (알림 푸시·실시간, 선택)
```

- **에자일 워크플로우**: Step 단위 구현 후 사용자 리뷰(승인) 뒤 다음 Step 진행 (`docs/할일.md` 규칙과 동일).

---

## 예상 신규/수정 클래스

- `config/WebSocketConfig`(또는 `WebSocketMessageBrokerConfigurer` 구현체)
- `chat/stomp/*` — 브로커·인터셉터·`@MessageMapping` 핸들러
- `config/SecurityConfig` — STOMP/WebSocket 보안
- (Step 2) Redis 브로커 설정·프로파일
- (Step 3) 기기 토큰 엔티티·API, FCM/APNs 클라이언트(또는 SSE/WS 알림 스트림 핸들러), `NotificationDispatchListener` 연동 지점

---

## Sprint 5·6와의 경계

- **Sprint 5**: REST 채팅·알림·`Sprint5-API.md` — 완료 후 본 스프린트 착수.
- **Sprint 6(본 문서)**: 실시간 채팅(STOMP)·(선택) Redis·(선택) 알림 푸시·실시간(Step 3).
- **그 외 후보**(`docs/Sprint5-할일.md` § Sprint 4·6와의 경계): 전역 API 인가 강화(B7-2) 등은 백로그·플래닝에서 별도 작업으로 쪼개도 됨.

---

## 프론트엔드 참고 (별도 프로젝트)

**실시간(Step 1 연계 시 권장)**

- **연결 생명주기**: 채팅 화면 진입 시 WebSocket 연결·STOMP 구독, 이탈 시 `unsubscribe` 및 연결 종료.
- **상태·캐시**: TanStack Query / SWR 등으로 **커서 기반 과거 메시지**(기존 REST)와 **실시간 수신 메시지**를 하나의 리스트 모델로 병합.
- **Optimistic UI**: 전송 직후 로컬에 임시 행 표시 → 서버/브로커 확인 후 확정 또는 실패 롤백.
- **무한 스크롤**: 상단 스크롤 시 `cursor`로 이전 페이지 로드(Sprint 5 REST API와 동일).
- **종료 매칭**: 서버에서 전송 거부·종료 이벤트 수신 시 입력 비활성화 및 소켓 정리.
