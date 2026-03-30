# Sprint 8 할일 문서 (백엔드) — 친구 시스템 및 소셜 알림

> **전제**: `docs/스프린트플래닝.md` Sprint 8, `docs/제품백로그.md` Epic 10 (B2-7 ~ B2-10), `docs/요구사항분석.md` 「친구 및 활동 알림」(§2.1 ~ 2.4).
>
> **프론트 범위**(프로필에서 친구 추가·목록 화면·알림 클릭 라우팅)는 본 문서에 포함하지 않는다. 백엔드만 단계별로 정리한다.
>
> **원칙**: 알림 저장·실시간 발송은 기존과 동일하게 `NotificationService#publishAfterCommit` → `NotificationDispatchCommand` → `@TransactionalEventListener(AFTER_COMMIT)` (`NotificationDispatchListener`) 경로를 재사용한다.

---

## 기존 코드 (확장·연동 대상)

| 영역 | 위치 | 비고 |
|------|------|------|
| 알림 발행 | `NotificationService#publishAfterCommit` | `ApplicationEventPublisher` |
| 알림 커밋 후 저장·STOMP·FCM | `NotificationDispatchListener`, `NotificationOutboundService` | `relatedMatchId` 이미 지원 |
| 알림 타입 | `NotificationType` | Sprint 8에서 **소셜용 enum 값 추가** |
| 매칭 생성 | `MatchService` (매칭 persist 완료 시점) | 요구사항의 «CREATED»는 코드상 **`MatchStatus.RECRUITING`** 로 첫 저장되는 시점에 대응 |
| 참여 확정 | `MatchParticipantService` 등 | 요구사항의 «CONFIRMED»는 코드상 **`ParticipantStatus.ACCEPTED`** 전환 직후 |
| 타인 프로필 | `GET /api/users/{userId}` | 친구 추가 API는 **대상 userId** 기준으로 설계 (닉네임 검색 불필요) |

---

## Sprint 8 목표 (백엔드)

1. **단방향 팔로우(친구 추가)** 를 DB·도메인으로 표현하고, **팔로우/목록(및 필요 시 언팔로우)** REST API를 제공한다.
2. 활동 주체 사용자 B에 대해 **`following_id = B` 인 모든 `follower_id`** 를 조회하는 리포지토리·쿼리를 둔다 (`docs/요구사항분석.md` §2.3).
3. **(가) 새 매칭 생성** 직후·**(나) 참여가 ACCEPTED로 확정**된 직후, 위 팔로워들에게 인앱 알림을 발행한다. 본문·제목은 §2.2 예시에 맞추고, **`relatedMatchId`를 반드시 채운다** (§2.4, B2-10).

### 완료 조건 (백엔드)

- [ ] **B2-7**: 로그인 사용자가 **상대 `userId`** 로 팔로우(친구 추가)할 수 있다. 자기 자신·중복 추가는 거절(명확한 에러/메시지).
- [ ] **B2-8**: 본인 기준 **팔로잉 목록**(내가 추가한 친구) 조회 API. «관리»에 따라 **언팔로우(DELETE)** 가 제품 합의에 포함되면 동 구현.
- [ ] **B2-9**: (가) 호스트가 매칭을 생성·저장한 뒤, 호스트를 팔로우하는 사용자에게 알림. (나) 참여자가 **ACCEPTED**가 된 뒤, 해당 참여자를 팔로우하는 사용자에게 알림. 실시간은 기존 STOMP/FCM 파이프라인 활용.
- [ ] **B2-10**: 위 알림 레코드에 **`relatedMatchId`** 가 저장되며, 기존 `NotificationResponse`(또는 동등 DTO)로 클라이언트가 `matchId` 를 알 수 있다.
- [ ] 운영 DB: **Flyway** 마이그레이션으로 `friendship`(또는 합의한 테이블명) 스키마 반영 (`application-prod.yml` 등에서 Flyway 사용 전제).

---

## Step 0: 정책·용어·스키마 합의

### Step 0 진행 상태

- [ ] 요구사항 문서의 **CREATED / CONFIRMED** 와 코드 **이름·상태** 불일치를 정리해 두었는가 → 본 문서 **「기존 코드」** 표 준수
- [ ] 테이블명·컬럼: `follower_id`(나), `following_id`(상대), **UK(`follower_id`, `following_id`)**, FK → `users`
- [ ] 알림 **타입명**·**메시지 템플릿**(닉네임 치환) 합의
- [ ] ACCEPTED로 바뀌는 **모든 코드 경로**를 나열해, **중복 알림**이 없는지(또는 허용인지) 합의 — 예: 방장 수락, 예약(RESERVED) 수락 등

### 산출

- (선택) `common-docs/api/Sprint8-API.md` — 엔드포인트·요청/응답·에러 코드

---

## Step 1: Friendship 엔티티·Repository·마이그레이션

### 1.1 엔티티

- [ ] 패키지: `org.app.mintonmatchapi` 하위 도메인 패키지 컨벤션에 맞게 — 예: `friendship` 또는 `social`
- [ ] `Friendship`: `id`, `follower`(User), `following`(User), 생성 시각 등
- [ ] 제약: **동일 (follower, following) 유니크**, **follower ≠ following** 은 서비스/DB 체크

### 1.2 Repository

- [ ] `existsByFollower_IdAndFollowing_Id`
- [ ] `findAllByFollower_Id` — 내 팔로잉 목록 (B2-8)
- [ ] `findFollowerIdsByFollowing_Id(Long followingUserId)` 또는 동일 의미의 JPQL/QueryDSL — §2.3 타깃팅

### 1.3 스키마

- [ ] **Flyway** `V…__create_friendship.sql` (또는 팀 버전 규칙) — **prod** 배포 경로
- [ ] 로컬: 기존처럼 Hibernate `ddl-auto` 와 스키마 일치 확인

---

## Step 2: 친구 추가·목록(·언팔로우) API

### 2.1 서비스

- [ ] `FriendshipService`(가칭): 팔로우 생성, 목록 조회, (선택) 언팔로우
- [ ] 비즈니스 규칙: 자기 자신 팔로우 불가, 이미 존재 시 «이미 친구입니다» 등 (`docs/요구사항분석.md` §2.1)

### 2.2 API (예시 — Step 0에서 경로 확정)

- [ ] `POST /api/...` — body 또는 path에 **`followingUserId`** (타인 프로필과 동일 ID)
- [ ] `GET /api/users/me/friendships` 또는 `GET /api/friendships/me/following` 등 — 본인 **팔로잉** 목록 (User 요약 DTO 재사용 검토: `ProfileResponse`/간략 DTO)
- [ ] (선택) `DELETE /api/.../{followingUserId}` — 언팔로우

### 2.3 보안

- [ ] `SecurityConfig`: 위 경로 인증 필요 여부 반영 (일반적으로 **인증 사용자만**)
- [ ] Controller는 Repository 직접 호출 금지 — **Service만** 호출

---

## Step 3: 알림 수신 대상 필터링

- [ ] 활동 사용자 `activeUserId`(매칭 만든 호스트 또는 ACCEPTED가 된 참여자)에 대해 `following_id = activeUserId` 인 모든 `follower_id` 조회
- [ ] **활동 주체 본인**에게는 발송하지 않음 (팔로워 목록에 자기 자신은 원칙적으로 없음)
- [ ] 팔로워가 없으면 **알림 발행 호출 생략** (불필요한 이벤트 방지)

---

## Step 4: MatchService / MatchParticipantService 연동 (이벤트·알림)

### 4.1 `NotificationType` 확장

- [ ] 예: `FRIEND_CREATED_MATCH`(가칭), `FRIEND_CONFIRMED_PARTICIPATION`(가칭) — **제품/코드 네이밍 확정** 후 enum 반영
- [ ] `NotificationDispatchCommand.of(...)` 로 `title`, `body`, `relatedMatchId` 설정 — §2.2 문구·닉네임 치환

### 4.2 매칭 생성 직후 (B2-9 가지 1)

- [ ] `MatchService`에서 매칭이 **처음 저장·커밋되는 트랜잭션** 안에서, 호스트 ID 기준 팔로워 ID 목록 조회 후 **팔로워별** `publishAfterCommit(...)` 호출
- [ ] 메시지: «[친구닉네임]님이 새로운 매칭을 만들었습니다!…» (호스트 닉네임)

### 4.3 참여 ACCEPTED 직후 (B2-9 가지 2)

- [ ] `ParticipantStatus`가 **ACCEPTED로 바뀐 직후**(커밋 전 트랜잭션 내에서 이벤트만 발행) 해당 **참여자 userId** 기준 팔로워에게 동일 패턴으로 발행
- [ ] 메시지: «[친구닉네임]님이 매칭 참여를 확정했습니다!…»
- [ ] **주의**: PENDING → ACCEPTED, WAITING → … → ACCEPTED, RESERVED → ACCEPTED 등 경로별로 **한 번만** 발행되도록 구현 위치 통일(헬퍼 메서드 추출 권장)

### 4.4 실패·성능

- [ ] 알림 실패는 비즈니스 트랜잭션을 깨지 않음 — 기존 `NotificationDispatchListener` try/catch 패턴 유지
- [ ] 팔로워 수가 많을 때를 대비해 **배치 크기·비동기**는 필요 시 후속; MVP는 단순 루프로도 무방하다면 문서에 «후속 최적화»로 남김

---

## Step 5: API 문서·회귀

- [ ] `Sprint8-API.md`(선택) 또는 기존 API 문서에 신규 엔드포인트·알림 타입·`relatedMatchId` 필드 명시
- [ ] (선택) 통합 테스트: 팔로우 → 매칭 생성 → 팔로워에게 `Notification` 행 존재 여부

---

## Step 완료 후 리뷰

- Step마다 사용자 리뷰 후 다음 Step 진행 (`.cursor/rules/agile-workflow.mdc`).

---

## 참고 코드 위치

- `notification`: `NotificationService`, `NotificationDispatchListener`, `NotificationDispatchCommand`, `NotificationType`, `NotificationOutboundService`
- `match`: `MatchService`, `MatchParticipantService`, `Match`, `MatchStatus`, `MatchParticipant`, `ParticipantStatus`
- `user`: `UserController`, `UserService`, `UserRepository`
