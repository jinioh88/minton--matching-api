# Sprint 3 할일 문서

> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/ERD.md`, `docs/요구사항분석.md`
> **상태: 진행 예정**

## Sprint 3 목표

참여 신청, 수락/거절, 대기열, 취소가 end-to-end로 동작한다.

### 완료 조건
- [ ] 참여 신청 → 방장 수락 → 확정 흐름 동작
- [ ] 정원 초과 시 대기 신청 가능
- [ ] 취소 시 대기열 하이브리드 시스템 동작 (순차 기회 → 타임아웃 → 긴급 선착순)
- [ ] 방장이 신청자 목록을 보고 수락/거절 가능

---

## 대기열 시스템: 하이브리드 타임아웃 (Hybrid Timeout System)

> 사용자 편의와 매칭 성공률을 모두 잡기 위한 "시간 제한이 있는 순차 알림" 방식

### 1단계: 순차 기회 부여 (기본 로직)
- 자리가 생기면 **대기 1번에게만** "참석 기회가 생겼습니다! 15분 내에 수락해 주세요." 알림 발송
- 해당 자리는 **예약 상태(RESERVED)**가 되어 다른 사람이 챌 수 없음

### 2단계: 타임아웃(Timeout) 처리
- **15분**(설정 가능) 내에 응답 없음 또는 '거절' → 기회가 대기 2번에게 자동 이전
- 응답 안 한 1번은 **대기 상태 해제** (CANCELLED)

### 3단계: 긴급 상황 전환 (임박 매칭)
- 경기 시작 **2시간 미만**으로 남은 시점에서 자리가 생기면 → **전체 알림(선착순)**으로 전환
- 매칭이 깨지는 것을 막기 위해 대기열 전체에게 동시 알림, 먼저 수락한 사람이 확정

---

## 하이브리드 대기열 구현 방향 (아키텍처)

> 순차 기회·타임아웃은 실시간성이 중요. **Spring Boot + Redis + (선택) Message Queue** 조합 권장.

### 1. 상태 전이 및 이벤트 흐름

| 구분 | 설명 |
|------|------|
| **Event Trigger** | `DELETE /api/matches/{id}/participants/me` 취소 시 → `ParticipantCancelledEvent` 발행 |
| **Scheduler/Worker** | 취소 이벤트 수신 후, 경기 시작까지 2시간 이상/미만 체크 |
| **Normal** | 대기 1번을 RESERVED로 변경, 15분 타이머 작동 |
| **Emergency** | WAITING 전체에게 알림, 선착순 모드 API 활성화 |

### 2. 타임아웃 구현 방안

| 방안 | 기술 | 원리 | 장점 | 단점 |
|------|------|------|------|------|
| **A (권장)** | Redis Key Space Notifications | `match:reservation:{participationId}` 키에 15분 TTL 설정 → 만료 시 Spring에 알림 | 실시간, 스케줄러 쿼리 불필요 | Redis 의존 |
| **B** | Spring @Scheduled + QueryDSL | 1분마다 `status=RESERVED AND offerExpiresAt < now` 조회 후 일괄 처리 | 구현 용이, DB 중심, 서버 재시작에도 안정 | 최대 1분 오차 |

- [ ] **방안 A**: Redis 의존성 추가, Key Space Notifications 구독, 만료 시 다음 대기자 승격
- [ ] **방안 B**: `@Scheduled(fixedRate = 60000)` + `(status, offerExpiresAt)` 복합 인덱스

### 3. 아키텍처 구성 요소

| 구성 요소 | 역할 |
|-----------|------|
| **Redis (Redisson)** | 분산 락 - 동시 취소/신청 시 정합성, **중복 수락 방지** |
| **ApplicationEventPublisher** | `ParticipantCancelledEvent` 발행 - 취소와 승격 로직 결합도 낮게 유지 |
| **@Async Task Executor** | 알림 발송, 다음 대기자 검색을 메인 트랜잭션과 분리 → 응답 속도 개선 |

### 4. 백엔드 개발자 피드백 (구현 시 필수)

| 항목 | 내용 |
|------|------|
| **동시성 제어** | 대기 1번에게 기회 부여 시 **DB 비관적 락(Pessimistic Lock)** 또는 **Redis 분산 락** 필수. 찰나의 순간 2명 승격 버그 방지 |
| **임박 기준값** | 2시간 기준은 `application.yml`에서 관리. `matchDate + startTime` 연산 시 **인덱스** 활용 (예: match_date, start_time) |
| **타임아웃 핸들러** | Redis Expire Event 사용 시 가장 깔끔. DB 스케줄러 사용 시 `(status, offer_expires_at)` **복합 인덱스** 설정 |

---

## Step 1: MatchParticipant 확장 및 Repository 메서드 (B4-1, B4-2)

> Sprint 2에서 MatchParticipant 엔티티가 이미 존재함. 참여 신청 API에 필요한 Repository 메서드 추가.

### Step 1.1: MatchParticipantRepository 메서드 추가
- [ ] `findByMatchIdAndUserId(Long matchId, Long userId)` - 중복 신청 방지, 기존 참여 여부 확인
- [ ] `findByMatchIdAndStatusOrderByQueueOrderAsc(Long matchId, ParticipantStatus status)` - 대기열 1번 조회용
- [ ] `findMaxQueueOrderByMatchId(Long matchId)` - 대기열 순번 계산 (WAITING 상태 최대 queueOrder + 1)
- [ ] `findByMatchIdAndStatusIn(Long matchId, List<ParticipantStatus> statuses)` - 방장의 PENDING/WAITING 신청 목록 조회
- [ ] `findByMatchIdAndStatus(Long matchId, ParticipantStatus status)` - RESERVED(예약 중) 조회용

### Step 1.2: MatchParticipant 상태 전이 검증
- [ ] PENDING: 참여 신청 (정원 여유 시)
- [ ] WAITING: 대기 신청 (정원 초과 시), queueOrder 부여
- [ ] **RESERVED**: 참여 기회 부여됨 (예약 상태, 15분 내 수락 대기) - 대기열에서 승격 시
- [ ] ACCEPTED: 방장 수락 또는 예약 수락 완료
- [ ] REJECTED: 방장 거절 또는 본인 거절/타임아웃

---

## Step 2: 참여 신청 API (B4-1, B4-2)

### Step 2.1: 참여 신청 요청 DTO
- [ ] `ParticipantApplyRequest`
  - applyMessage (String, 선택, 최대 200자) - 참여 멘트

### Step 2.2: 참여 신청 API
- [ ] POST /api/matches/{matchId}/participants (인증 필요)
  - Request Body: `ParticipantApplyRequest` (applyMessage)
  - 현재 사용자가 해당 매칭에 참여/대기 신청
- [ ] MatchParticipantService.applyParticipant(currentUserId, matchId, request)
  - 매칭 존재 여부, RECRUITING 상태 확인
  - 중복 신청 방지 (이미 PENDING/ACCEPTED/WAITING이면 예외)
  - 방장 본인 신청 방지
  - **정원 여유**: status=PENDING, queueOrder=0
  - **정원 초과**: status=WAITING, queueOrder=다음 순번
- [ ] Response: 참여 신청 결과 (participationId, status, queueOrder 등)

### Step 2.3: 예외 처리
- [ ] MATCH_NOT_FOUND
- [ ] MATCH_NOT_RECRUITING (CLOSED, CANCELLED 등)
- [ ] ALREADY_APPLIED (이미 신청한 상태)
- [ ] HOST_CANNOT_APPLY (방장은 참여 신청 불가)

---

## Step 3: 수락/거절 API (B4-3)

### Step 3.1: 수락/거절 요청 DTO
- [ ] `ParticipantDecisionRequest`
  - action (Enum: ACCEPT, REJECT)

### Step 3.2: 수락/거절 API
- [ ] PATCH /api/matches/{matchId}/participants/{participationId} (인증 필요)
  - Request Body: `ParticipantDecisionRequest` (action: ACCEPT | REJECT)
  - **방장만** 호출 가능
- [ ] MatchParticipantService.decideParticipant(currentUserId, matchId, participationId, request)
  - 매칭 hostId == currentUserId 확인
  - participationId의 status가 PENDING 또는 WAITING인지 확인
  - ACCEPT: status=ACCEPTED, queueOrder=0 (또는 유지)
  - REJECT: status=REJECTED
  - **수락 시 정원 초과 여부 확인** (currentPeople >= maxPeople이면 수락 불가)

### Step 3.3: 예외 처리
- [ ] FORBIDDEN (방장이 아님)
- [ ] PARTICIPANT_NOT_FOUND
- [ ] INVALID_STATUS (이미 수락/거절된 신청)
- [ ] MATCH_FULL (수락 시 정원 초과)

---

## Step 4: 취소 API 및 대기열 하이브리드 로직 (B4-4, B4-5)

### Step 4.1: 참여 취소 API 및 이벤트 발행
- [ ] DELETE /api/matches/{matchId}/participants/me (인증 필요)
  - 현재 사용자의 해당 매칭 참여/대기 신청 취소
- [ ] MatchParticipantService.cancelParticipant(currentUserId, matchId)
  - 본인 참여(PENDING/ACCEPTED/WAITING/RESERVED)만 취소 가능
  - **권장**: status=CANCELLED enum 추가 (이력 보존)
- [ ] **이벤트 발행**: ACCEPTED 취소 시 `ApplicationEventPublisher.publishEvent(ParticipantCancelledEvent)` 호출
  - 취소 로직과 대기열 승격 로직 결합도 분리

### Step 4.2: 대기열 하이브리드 로직 (B4-5)

#### 4.2.1 기본: 순차 기회 부여 (경기 2시간 이상 남음)
- [ ] `ParticipantCancelledEvent` 리스너에서 처리 (또는 Service 내부)
- [ ] **동시성**: DB 비관적 락(`@Lock(PESSIMISTIC_WRITE)`) 또는 Redisson 분산 락으로 대기 1번 승격 시 락 획득
- [ ] ACCEPTED 취소 시:
  1. 해당 매칭에 RESERVED 상태 없음 확인 (이미 예약 중이면 대기)
  2. WAITING 중 queueOrder 최소인 1명 조회 (락 유지)
  3. 있으면 → status=**RESERVED**, **offerExpiresAt=now+15분** 설정
  4. **비동기(@Async)**: 알림 발송 "참석 기회가 생겼습니다! 15분 내에 수락해 주세요."
  5. **타임아웃 등록**: Redis Key(`match:reservation:{participationId}`) 15분 TTL 또는 DB에 offerExpiresAt 저장

#### 4.2.2 예약 수락/거절 API (대기열 사용자용)
- [ ] POST /api/matches/{matchId}/participants/me/accept-offer (인증 필요)
  - RESERVED 상태인 본인이 수락 → status=ACCEPTED
  - **분산 락**으로 중복 수락 방지
- [ ] POST /api/matches/{matchId}/participants/me/reject-offer (인증 필요)
  - RESERVED 상태인 본인이 거절 → 대기 상태 해제(CANCELLED), 다음 대기자에게 기회 부여

#### 4.2.3 타임아웃 처리
- [ ] **방안 A (Redis)**: Key Space Notifications 구독 → `match:reservation:*` 만료 시 다음 대기 1번에게 RESERVED 부여
- [ ] **방안 B (DB)**: `@Scheduled(fixedRate = 60000)` + `status=RESERVED AND offer_expires_at < now` 조회
  - `(status, offer_expires_at)` 복합 인덱스 필수
  - 타임아웃 시: 해당 유저 → **대기 상태 해제**(CANCELLED), 다음 대기 1번에게 RESERVED 부여

#### 4.2.4 긴급 모드: 선착순 (경기 2시간 미만)
- [ ] `matchDate + startTime`이 현재로부터 **2시간 미만**이면 (application.yml `queue.emergency-threshold-hours`):
  - 순차 부여 대신 **WAITING 전체**에게 동시 알림 (@Async)
  - **먼저 수락한 사람**이 ACCEPTED (선착순) - accept-offer 호출 시 분산 락으로 선착순 보장
  - RESERVED 없이, 정원 여유 확인 후 즉시 ACCEPTED

### Step 4.3: MatchParticipant 엔티티 확장
- [ ] `offerExpiresAt` (LocalDateTime, nullable) - 예약 만료 시각
- [ ] ParticipantStatus에 **RESERVED**, **CANCELLED** 추가

### Step 4.4: 설정값 및 인덱스
- [ ] `queue.offer-timeout-minutes` (기본 15) - 예약 유효 시간
- [ ] `queue.emergency-threshold-hours` (기본 2) - 긴급 선착순 전환 기준
- [ ] Match 조회: `matchDate`, `startTime` 인덱스로 2시간 미만 판단 최적화

---

## Step 5: 방장 신청 관리 API (B4-3)

### Step 5.1: 신청 목록 조회
- [ ] GET /api/matches/{matchId}/participants/applications (인증 필요)
  - **방장만** 호출 가능
  - PENDING(참여 대기), WAITING(대기열), RESERVED(예약 중) 목록 조회
  - 정렬: PENDING 먼저, RESERVED, 그 다음 WAITING (queueOrder ASC)
- [ ] MatchParticipantService.getApplications(matchId, currentUserId)
  - hostId 검증
  - PENDING, WAITING 상태의 MatchParticipant 목록 + User 프로필 요약

### Step 5.2: 응답 DTO
- [ ] `ParticipantApplicationResponse`
  - participationId, userId, nickname, profileImg, ratingScore
  - status, queueOrder, applyMessage
  - appliedAt (createdAt)
  - offerExpiresAt (RESERVED일 때만, 수락 마감 시각)

---

## Step 6: 매칭 상세 응답 확장 (B4-1~5)

### Step 6.1: 로그인 사용자용 추가 정보
- [ ] GET /api/matches/{matchId} 호출 시 **인증된 사용자**에게 추가 필드:
  - `myParticipation`: 현재 사용자의 참여 상태 (없으면 null)
    - participationId, status (PENDING/ACCEPTED/REJECTED/WAITING/**RESERVED**), queueOrder, applyMessage
    - **offerExpiresAt** (RESERVED일 때 - 수락 마감 시각)
  - 참여 신청 가능 여부, 취소 가능 여부, **예약 수락 대기 여부** 등 UI 힌트

### Step 6.2: MatchDetailResponse 수정
- [ ] `myParticipation` (ParticipantSummary 또는 MyParticipationSummary) 필드 추가
- [ ] @IfLogin으로 principal 전달 시에만 myParticipation 채움

---

## Step 7: API 문서 업데이트

- [ ] common-docs/api/Sprint3-API.md 또는 기존 문서에 Sprint 3 API 추가
  - POST /api/matches/{matchId}/participants (참여/대기 신청)
  - PATCH /api/matches/{matchId}/participants/{participationId} (방장 수락/거절)
  - DELETE /api/matches/{matchId}/participants/me (참여 취소)
  - POST /api/matches/{matchId}/participants/me/accept-offer (예약 수락 - 대기열 사용자)
  - POST /api/matches/{matchId}/participants/me/reject-offer (예약 거절 - 대기열 사용자)
  - GET /api/matches/{matchId}/participants/applications (방장용 신청 목록)
- [ ] Request/Response 예시, ParticipantStatus 확장(RESERVED, CANCELLED) 반영
- [ ] 대기열 하이브리드 시스템(순차/타임아웃/긴급 선착순) 설명

---

## Sprint 3 작업 요약

| Step | 내용 | 백로그 ID |
|------|------|-----------|
| 1 | MatchParticipant Repository 메서드 확장 | B4-1, B4-2 |
| 2 | 참여 신청 API (참여/대기) | B4-1, B4-2 |
| 3 | 수락/거절 API | B4-3 |
| 4 | 취소 API, 대기열 하이브리드(순차/타임아웃/긴급 선착순) | B4-4, B4-5 |
| 5 | 방장 신청 관리 API | B4-3 |
| 6 | 매칭 상세 myParticipation 확장 | B4-1~5 |
| 7 | API 문서 | B1-3 |

---

## 의존성 및 진행 순서

```
Step 1 (Repository) → Step 2 (참여 신청) → Step 3 (수락/거절)
                                    ↘
Step 4 (취소, 대기열 승인) ← Step 3 완료 후
Step 5 (방장 신청 관리) ← Step 2, 3과 병렬 가능
Step 6 (상세 확장) ← Step 2 완료 후
Step 7 (API 문서) ← 모든 API 완료 후
```

- Step 1 → Step 2 → Step 3 → Step 4 순차 진행
- Step 5는 Step 2, 3과 독립적으로 병렬 가능
- Step 6은 Step 2 완료 후 진행

---

## 예상 신규/수정 클래스

### 신규
- match/dto/ParticipantApplyRequest.java
- match/dto/ParticipantDecisionRequest.java
- match/dto/ParticipantApplicationResponse.java
- match/dto/MyParticipationSummary.java (또는 기존 ParticipantSummary 확장)
- match/service/MatchParticipantService.java
- match/controller/MatchParticipantController.java (또는 MatchController 하위 매핑)
- **match/event/ParticipantCancelledEvent.java** - 취소 이벤트
- **match/event/QueuePromotionListener.java** - 이벤트 리스너 (대기열 승격, @Async)
- **config/AsyncConfig.java** - @Async Executor 설정 (선택)

### 수정
- match/entity/ParticipantStatus.java (RESERVED, CANCELLED 추가)
- match/entity/MatchParticipant.java (offerExpiresAt 컬럼 추가)
- match/repository/MatchParticipantRepository.java (메서드 추가, 비관적 락 쿼리)
- match/service/MatchService.java (상세 조회 시 myParticipation 추가)
- match/dto/MatchDetailResponse.java (myParticipation 필드)
- config/SecurityConfig.java (새 API 경로 인증 설정)

### 선택 (방안 A: Redis)
- Redis 의존성 (spring-boot-starter-data-redis, redisson)
- RedisKeySpaceNotificationConfig - Key Space Notifications 활성화
- QueueReservationExpiryHandler - 키 만료 시 다음 대기자 승격

---

## 프론트엔드 참고 (별도 프로젝트)

Sprint 3 프론트엔드 작업 (본 API 프로젝트 범위 아님):
- 참여 신청 UI: 참여/대기 버튼, 멘트 입력
- 방장 신청 관리: 신청 목록, 수락/거절 버튼
- 참여 취소: 내 참여 취소 버튼
- **예약 수락/거절 UI**: "참석 기회가 생겼습니다! 15분 내 수락" 알림 → 수락/거절 버튼
- 상태 표시: 수락 대기 중, 확정, 대기열, **예약 대기(15분)**

---

## 알림 연동 (Sprint 5 참조)

대기열 하이브리드 시스템은 **알림(B6-5)**과 연동됩니다. Sprint 3에서는 API·비즈니스 로직을 구현하고, 실제 푸시 알림 발송은 Sprint 5(채팅 및 알림)에서 구현합니다.
