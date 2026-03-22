# Sprint 4 할일 문서 (백엔드)

> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/ERD.md`, `docs/요구사항분석.md`
> **상태: 진행 예정**

## Sprint 4 목표

후기 작성·조회, 패널티 부여·조회가 동작한다.

### 완료 조건 (스프린트 플래닝·백로그 정합)

- [ ] **모임 종료 후** 해당 매칭의 확정 참여자끼리 후기 작성 가능 (B5-1)
- [ ] 후기에 **3단계 반응(좋아요 계열)**, **점수**, **해시태그**, 선택적 **상세 텍스트** 저장 (B5-2, `docs/요구사항분석.md` 후기 절)
- [ ] **유저별 후기 목록(이력) 조회** API 제공 (B5-3)
- [ ] **방장**이 해당 매칭 참여자에 대해 **노쇼/지각** 패널티를 기록하면, 이력이 남고 `Users.penalty_count` 등 집계가 반영됨 (B5-4)
- [ ] **유저별 패널티 이력 조회** API 제공 (B5-5)
- [ ] **단계별 제재**가 스트라이크(또는 points)에 따라 적용되고, `participation_banned_until` / `suspended_until`·참여 신청 검증이 동작함 (`docs/요구사항분석.md`)
- [ ] 프로필(또는 내 정보)에서 **제한·정지 만료 시각**(또는 남은 시간)을 확인할 수 있음
- [ ] 타인 프로필·매칭 상세 등에서 활용할 수 있도록 **평점 집계** 정책이 문서화·구현됨 (`User.rating_score`, **프라이어 반영·가중 갱신**)
- [ ] 후기 조회에 **상호 익명·공개 시점** 규칙이 반영됨 (`docs/요구사항분석.md`)

---

## 비즈니스 규칙 요약

### 후기 (Review)

| 규칙 | 설명 |
|------|------|
| 작성 시점 | 매칭이 **종료(FINISHED)** 상태이고, 모임 **일시가 지난 뒤** 작성 가능 여부는 팀 합의 후 `application.yml` 등으로 고정 |
| 작성자·대상 | **같은 매칭**에서 `ParticipantStatus.ACCEPTED`였던 서로 다른 두 유저만 (본인→본인 불가, 방장·게스트 구분 없이 확정자끼리) |
| 중복 | 동일 `(match_id, reviewer_id, reviewee_id)` **유니크** — 한 모임에서 A→B 후기는 1건 |
| 데이터 | **3단계 반응**(예: 부정/보통/긍정), **점수(1~5)**, **해시태그 목록**, **상세 설명(선택)** — ERD `Reviews` 확장 및 `docs/요구사항분석.md` 반영 |
| **상호 익명·공개** | **서로 후기를 모두 작성했거나**, FINISHED 기준 **작성 유예 기간(예: 3일) 종료 후**에만 상대가 쓴 후기의 **구체 내용**(점수·텍스트·태그) 공개. 그 전에는 **작성 완료 여부**만 노출하고 내용 마스킹. 조회자·역할별로 API에서 필터링 (`docs/요구사항분석.md`) |

### 평점 (`User.rating_score`)

| 규칙 | 설명 |
|------|------|
| **신규 초기값** | 가입 직후 `rating_score`는 **0**으로 둔다(아직 받은 후기 없음·**미평가**). UI에서는 0이면 "평가 없음" 등으로 표시하는 것을 권장한다. **만점(5.0)으로 두지 않는다** — 무후기인데 높은 점수로 보이는 왜곡을 막기 위함. |
| 단순 평균 금지 | 후기가 쌓인 뒤에도 **실제 점수만 산술 평균**하면 표본 수 차이로 비교가 왜곡될 수 있음 |
| **가중 갱신(필수)** | 가상 프라이어 개수 **M**, **프라이어 평균 priorMean**(예: 5.0 — 내부 가중용, DB 초기값과 별개), 직전 저장 평점 **R**, 실제 받은 후기 수(이번 반영 직전) **n**, 새 점수 **s**. **`n = 0`**(첫 후기): **`R' = (M × priorMean + s) / (M + 1)`**. **`n ≥ 1`**: **`R' = (R × (M + n) + s) / (M + n + 1)`**. M·priorMean은 `application.yml` (예: `review.rating-prior-count`, `review.rating-prior-mean`) |
| 대안 | 동일 취지의 베이지안 평균(전역 평균·강도) — 구현 시 주석에 수식 명시 |

### 패널티·단계별 제재 (Penalty & Graduated Sanction)

> 상세 표·운영 원칙: `docs/요구사항분석.md` **제재(Suspension) 정책** 절

| 규칙 | 설명 |
|------|------|
| 권한 | **해당 매칭 방장(host)** 만 부여 API 호출 가능 |
| 대상 | 해당 매칭에서 **ACCEPTED** 참여자(본인 제외 권장) |
| 유형 | **노쇼(NO_SHOW)**, **지각(LATE)** — `docs/제품백로그.md` B5-4 |
| 중복 | 동일 `(match_id, penalized_user_id, penalty_type)` **1회만** 부여 등, 정책을 명시하고 DB 유니크 또는 서비스 검증으로 보장 |
| 반영 | `Penalty` 행 INSERT + `Users.penalty_count` 증가(필수). **(선택)** 유형별 가중치만큼 `penalty_points` 합산 |
| **단계별 제재(권장·Sprint 4 포함)** | **스트라이크**(또는 points) 누적에 따라 계단 적용: **1~2 주의**(프로필 표시·알림), **3회 → 3일간 참여 신청 금지**(`participation_banned_until` 갱신), **5회 → 7일간 넓은 제한**(`suspended_until` 갱신), **10회 → 영구 정지/BANNED**(운영 검토). 임계값·일수는 `application.yml`. 방장(매칭 개설)은 제한 단계에서도 유지 가능 |
| **참여 신청 검증** | `POST .../participants` 상단에서 `participation_banned_until != null && isAfter(now)` → `USER_PARTICIPATION_BANNED` 등. 정지는 `suspended_until`·`account_status`로 다른 API에서 일괄 차단 |
| **프로필 API** | 본인·타인 정책에 맞게 **`participation_banned_until`**, **`suspended_until`**, 주의 배지 여부 등으로 **남은 제한 시간** 안내 |
| **복구·완화(선택)** | 장기 무발생 시 스트라이크 감소, 긍정 후기 상쇄 등 — 후속 스프린트 가능, 요구사항에만 명시해도 됨 |

### 매칭 종료(FINISHED) 선행 조건

후기·패널티(모임 이후 행위)의 전제로 **매칭을 FINISHED로 바꾸는 경로**가 필요하다.

- **수동**: `PATCH /api/matches/{matchId}/finish` — 방장만, 전제 상태·일시 검증은 정책에 맞게 정의
- **자동(필수)**: Spring **`@Scheduled`** 로 주기 실행 — 모임 **시작 시각(`matchDate` + `startTime`) 기준 N시간(기본 **6시간**) 경과한 매칭 중 `CLOSED`(또는 정책에 맞는 상태)를 **`FINISHED`로 일괄 전환**. 방장 누락 시에도 후기 작성 가능하도록 함 (`docs/요구사항분석.md`)

---

## Step 1: 매칭 종료(FINISHED) — 수동 API + 자동 스케줄러 (B5-1 전제)

> `MatchStatus`에 `FINISHED`가 이미 정의되어 있음. **자동 종료는 Sprint 4 필수**, 수동은 보조.

### Step 1.1: 수동 종료 API
- [ ] `PATCH /api/matches/{matchId}/finish` (인증 필요)
  - 호출자 == `Match.hostId`
  - `Match.status == CLOSED` (또는 팀이 정한 전제 상태)
  - (선택) `matchDate + startTime`이 현재 시각 이전인지 검증
- [ ] `MatchService.finishMatch(hostUserId, matchId)` — 상태 전이, 불가 시 예외

### Step 1.2: 자동 종료 스케줄러 (**필수**)
- [ ] `@Scheduled(cron = "0 0 * * * *")` + `@SchedulerLock` 적용 (매시 정각 실행)
- [ ] `application.yml`에 `match.auto-finish-hours: 10` 설정 반영
- [ ] `MatchRepository` 일괄 수정 쿼리 구현 (QueryDSL `update` clause)
  - 조건: `status = CLOSED` AND `(matchDate + startTime) <= now - 10h`
- [ ] `MatchService.autoFinishMatches()` 구현
  - 대상 조회 → 상태 변경(`FINISHED`) → 변경 건수 및 대상 ID 로깅
- [ ] (선택) 자동 종료된 매칭의 참여자들에게 "후기를 작성해주세요" 알림(Sprint 5 연동 대비 인터페이스만 설계)

### Step 1.3: 예외 (수동 API)
- [ ] `MATCH_NOT_FOUND`, `FORBIDDEN`, `INVALID_MATCH_STATUS` (이미 FINISHED/CANCELLED 등)

### Step 1.4: 설정
- [ ] `application.yml`: `match.auto-finish-after-start-hours` (기본 24), 스케줄 주기 설정값(선택)

---

## Step 2: Review 엔티티 및 Repository (B5-1, B5-2)

> ERD `2.5 Reviews`를 기준으로, 요구사항의 **3단계 좋아요·상세설명** 필드를 반영한다.

### Step 2.1: 엔티티 `Review` (패키지 예: `review.entity` 또는 `match.entity` 하위 — 프로젝트 컨벤션에 맞출 것)
- [ ] `id` (PK)
- [ ] `match` (@ManyToOne), `reviewer` (@ManyToOne User), `reviewee` (@ManyToOne User)
- [ ] `sentiment` — Enum: 3단계 (예: `ReviewSentiment`: NEGATIVE, NEUTRAL, POSITIVE)
- [ ] `score` — int, 1~5 (ERD와 동일)
- [ ] `detail` — String/Text, nullable (상세 설명)
- [ ] **해시태그 저장 (권장)** — 배드민턴 도메인에서 태그 종류가 **고정**에 가깝다면 **Enum Set** 또는 **`ReviewHashtag` 참조 테이블**(태그 코드 FK + `review_id`)로 관리해 검색·통계에 유리. 단순 JSON 컬럼도 가능하나, "태그별 유저 집계" 등 확장 시 마이그레이션 비용이 큼.
- [ ] `createdAt`, `updatedAt` (감사용)
- [ ] DB 유니크: `(match_id, reviewer_id, reviewee_id)`

### Step 2.2: Repository
- [ ] `ReviewRepository`
  - `existsByMatchIdAndReviewerIdAndRevieweeId(...)`
  - `findByRevieweeIdOrderByCreatedAtDesc(..., Pageable)` — 유저 후기 목록 (B5-3)
  - **상호 공개 판별용**: 특정 `matchId`에서 `(reviewer_id, reviewee_id)` 쌍 양방향 작성 여부, FINISHED 시각·유예 기간 계산에 필요한 조회 메서드
  - (선택) `findByMatchId` — 매칭별 후기 목록 (방장/분쟁 대응용)

### Step 2.3: Flyway/Liquibase 등 마이그레이션
- [ ] `reviews` 테이블 생성 및 **복합 인덱스 `(reviewee_id, created_at DESC)`** — 유저별 목록 페이징·정렬
- [ ] `match_id`, 유니크 인덱스 `(match_id, reviewer_id, reviewee_id)`

---

## Step 3: 후기 작성 API (B5-1, B5-2)

### Step 3.1: 요청 DTO `ReviewCreateRequest`
- [ ] `revieweeId` (Long, 필수)
- [ ] `sentiment` (Enum, 필수)
- [ ] `score` (int 1~5, 필수)
- [ ] `hashtags` (`List<String>`, 선택, 최대 개수·길이 제한)
- [ ] `detail` (String, 선택, 최대 길이 제한)

### Step 3.2: API
- [ ] `POST /api/matches/{matchId}/reviews` (인증 필요)
- [ ] `ReviewService.createReview(currentUserId, matchId, request)`
  - 매칭 `FINISHED` 확인
  - 작성자·피평가자 모두 해당 매칭 **ACCEPTED** 참여 이력 확인 (`MatchParticipant` 조회)
  - `reviewer_id != reviewee_id`
  - 중복 후기 방지
  - 저장 후 **피평가자 `User.rating_score` 가중 갱신 (필수)**  
    - 설정: `review.rating-prior-count` = M, `review.rating-prior-mean` = priorMean (내부 가중용, 예: 5.0)  
    - 피평가자의 **실제 받은 후기 건수** n은 **이번 INSERT 직전** `COUNT`(reviewee_id)로 조회(또는 `User.receivedReviewCount` 컬럼을 둘 경우 트랜잭션 내 일관되게 +1)  
    - **갱신 전** 저장 평점 R (신규·무후기는 **0**)  
    - **`n = 0`**: **`R' = (M × priorMean + s) / (M + 1)`**  
    - **`n ≥ 1`**: **`R' = (R × (M + n) + s) / (M + n + 1)`**  
    - 구현 클래스 주석에 위 수식과 `docs/요구사항분석.md` 참조 명시  
    - (대안) 베이지안 평균 사용 시 동일 문서에 수식 정리 후 코드 주석에 링크

### Step 3.3: 예외
- [ ] `MATCH_NOT_FOUND`, `REVIEW_NOT_ALLOWED` (상태/참여 조건 불충족), `SELF_REVIEW_NOT_ALLOWED`, `DUPLICATE_REVIEW`, `USER_NOT_FOUND`

---

## Step 4: 후기 조회 API (B5-3)

### Step 4.1: 유저별 후기 목록
- [ ] `GET /api/users/{userId}/reviews?page=&size=` (인증 정책: 공개 프로필과 동일하게 **비로그인 허용** 또는 로그인만 — `B2-5`와 정합)
- [ ] 응답 DTO: 후기 id, 매칭 요약(제목·일시 또는 matchId만), 작성자 공개 정보(닉네임·프로필), sentiment, score, hashtags, detail, createdAt
- [ ] **상호 익명·공개 시점(필수)** — `docs/요구사항분석.md`  
  - 조회자가 **로그인**한 경우, 각 후기 행에 대해: 해당 `match`에서 조회자와 `reviewer` 간 **역방향 후기 작성 완료 여부**, FINISHED 시각 기준 **유예 기간(예: 3일, `review.reveal-after-finish-hours` 또는 days)** 경과 여부를 계산  
  - **공개 조건 불충족** 시: `contentRevealed=false`, 점수·해시태그·상세·sentiment 등 **구체 필드 null 또는 마스킹**, `reviewSubmitted=true` 등 **작성 완료 플래그만** 허용(정책에 맞게 필드 설계)  
  - **본인이 받은 후기**를 볼 때도 동일 규칙 적용(보복 방지)  
  - 비로그인 조회 시: 공개 정책을 **가장 엄격하게**(예: 항상 마스킹) 또는 공개된 후기만 — 제품 합의 후 API 스펙에 명시
- [ ] **성능**: `Review` → `reviewer`(User) → `match` 연관이 이어지므로 **fetch join**(또는 배치 사이즈) 필수, **복합 인덱스 `(reviewee_id, created_at)`** 활용

### Step 4.2: (선택) 매칭 단위 후기 목록
- [ ] `GET /api/matches/{matchId}/reviews` — 방장 또는 참여자만 (정책 합의)

---

## Step 5: Penalty 엔티티 및 Repository (B5-4, B5-5)

### Step 5.1: 엔티티 `Penalty`
- [ ] `id` (PK)
- [ ] `match` (@ManyToOne)
- [ ] `host` (@ManyToOne User) — 부여한 방장
- [ ] `penalizedUser` (@ManyToOne User) — 대상자
- [ ] `type` — Enum: `NO_SHOW`, `LATE`
- [ ] `createdAt`
- [ ] 유니크: `(match_id, penalized_user_id, type)` 권장

### Step 5.2: Repository
- [ ] `PenaltyRepository`
  - `findByPenalizedUserIdOrderByCreatedAtDesc(..., Pageable)` — 패널티 이력 (B5-5)
  - `existsByMatchIdAndPenalizedUserIdAndType(...)`

### Step 5.3: 마이그레이션
- [ ] `penalties` 테이블, 인덱스 `penalized_user_id`

### Step 5.4: `Users` 제재 관련 컬럼 (`docs/ERD.md`)
- [ ] `participation_banned_until` (DATETIME, nullable) — 참여 신청(Apply)만 막는 제한 단계
- [ ] `suspended_until` (DATETIME, nullable) — 정지 단계(넓은 기능 제한)
- [ ] `account_status` (ENUM/VARCHAR: ACTIVE, SUSPENDED, BANNED 등) — 퇴출·영구 정지
- [ ] **(선택)** `penalty_points` — 유형별 가중치 합산 시 단계 판정에 사용
- [ ] `User` 엔티티 필드 매핑 및 기존 사용자 마이그레이션(NULL 허용)

---

## Step 6: 패널티 부여 API (B5-4)

### Step 6.1: 요청 DTO `PenaltyGrantRequest`
- [ ] `userId` (Long, 필수) — 패널티 대상
- [ ] `type` (NO_SHOW | LATE, 필수)

### Step 6.2: API 및 단계별 제재 갱신
- [ ] `POST /api/matches/{matchId}/penalties` (인증 필요)
- [ ] `PenaltyService.grantPenalty(currentUserId, matchId, request)`
  - `currentUserId == match.hostId`
  - 대상자는 해당 매칭 **ACCEPTED** (방장 본인에게 부여 불가 등 검증)
  - 중복 부여 방지
  - `Penalty` 저장 + `penalizedUser.penaltyCount` +1
  - **(선택)** 설정에 따라 `penalty_points += weight(type)` (예: NO_SHOW=2, LATE=1)
  - 부여 직후 **누적 스트라이크(또는 points)** 로 현재 단계 판정 → 요구사항 표에 맞게 **`participation_banned_until` / `suspended_until` / `account_status` 갱신**
    - **3스트라이크(또는 동일 단계 임계)**: `participation_banned_until = max(기존값, now + restrictionDays)` (기본 3일)
    - **5스트라이크**: `suspended_until = max(기존값, now + suspensionDays)` (기본 7일), 필요 시 `account_status`
    - **10스트라이크**: `account_status = BANNED`(또는 영구 정지 플래그), 운영 알림·로그
    - 이미 더 강한 제재 시각이 남아 있으면 **짧은 기간으로 덮어쓰지 않도록** `max`로 병합
  - **주의(1~2회)**: 별도 DATETIME 없이 `penalty_count`·프로필 **주의 배지**만으로도 구현 가능(플래그 `caution_level` 등 선택)

### Step 6.3: `application.yml` 설정 예시 (대기열 `queue`와 분리 권장)

```yaml
sanction:
  strike-thresholds:
    participation-ban: 3      # 참여 신청 금지 단계
    full-suspension: 5       # 넓은 제한
    permanent-ban: 10
  durations:
    participation-ban-days: 3
    suspension-days: 7
  penalty-weights:
    NO_SHOW: 2
    LATE: 1
  # weight 미사용 시 penalty_count만으로 strike 판정
```

### Step 6.4: 참여 신청 연동 (**필수**)
- [ ] `MatchParticipantService.applyParticipant` (또는 동일 진입점) 최상단:  
  `if (user.getParticipationBannedUntil() != null && user.getParticipationBannedUntil().isAfter(Instant/LocalDateTime.now()))` → `BusinessException(USER_PARTICIPATION_BANNED, "패널티 누적으로 참여가 제한되었습니다.")`
- [ ] 정지 단계: `suspended_until`·`account_status`에 따라 다른 쓰기 API 차단(필요 시 필터/서비스 공통 검증)

### Step 6.5: 예외
- [ ] `MATCH_NOT_FOUND`, `FORBIDDEN`, `INVALID_PENALTY_TARGET`, `DUPLICATE_PENALTY`

---

## Step 7: 패널티 이력 조회 API (B5-5)

- [ ] `GET /api/users/{userId}/penalties?page=&size=`
- [ ] 응답: 패널티 id, type, 매칭 요약, 부여 시각, (선택) 부여 방장 닉네임 — 과도한 개인정보 노출 주의
- [ ] 프로필 화면(`ProfileResponse`)의 `penaltyCount`와 목록 API의 총건수 정합성 유지

---

## Step 8: 프로필·매칭 응답과의 연동 (선택·권장)

- [ ] `GET /api/users/{userId}` · `GET /api/users/me` 등: **`participation_banned_until`**, **`suspended_until`**, (선택) **주의(caution) 표시**, **남은 제한 시간** 또는 ISO 시각 — 본인은 전부, 타인은 노출 범위를 정책으로 제한(개인정보·스토킹 방지)
- [ ] `GET /api/users/{userId}` (타인 프로필) 응답에 **최근 후기 요약** 또는 **평균 점수·태그 상위 N개** 필드 추가 여부 검토 (B3-6, Phase 5 UI 스펙과 정합)
- [ ] `MatchDetailResponse` / `HostSummary` 등 기존 `ratingScore`는 `User.rating_score`를 계속 사용 — 후기 저장 시 갱신되도록 Step 3과 연결

---

## Step 9: 해시태그 (선택)

- [ ] `GET /api/reviews/hashtag-suggestions` — 서버 고정 목록(열정, 매너, 친절, 고수 등) 반환, 프론트 선택 UI용 (B5-2)

---

## Step 10: API 문서 (B1-3)

- [x] `common-docs/api/Sprint4-API.md` 작성 (또는 기존 API 문서에 Sprint 4 섹션 추가)
  - `PATCH /api/matches/{matchId}/finish` (수동 종료)
  - **자동 종료 스케줄러** 동작 요약(배치 주기·조건·설정 키)
  - `POST /api/matches/{matchId}/reviews`
  - `GET /api/users/{userId}/reviews` — **마스킹 필드·`contentRevealed` 등** 응답 예시
  - `POST /api/matches/{matchId}/penalties`
  - `GET /api/users/{userId}/penalties`
  - **단계별 제재·`sanction.*` 설정**, 참여 신청 시 `USER_PARTICIPATION_BANNED` / 정지 관련 에러
  - 프로필 응답의 **`participation_banned_until` / `suspended_until`**
  - 에러 코드, 상태 전이, 작성 가능 조건(FINISHED + ACCEPTED), **평점 갱신 수식·설정 키** 요약

---

## Sprint 4 작업 요약

| Step | 내용 | 백로그 ID |
|------|------|-----------|
| 1 | 매칭 FINISHED: **수동 API + 자동 스케줄러(필수)** | B5-1 전제 |
| 2 | Review 엔티티·Repository·마이그레이션(복합 인덱스, 태그 저장 방식) | B5-1, B5-2 |
| 3 | 후기 작성 API + **프라이어 가중 `rating_score` 갱신** | B5-1, B5-2 |
| 4 | 유저별 후기 목록 API + **익명·공개 시점 마스킹** + fetch join | B5-3 |
| 5 | Penalty 엔티티·Repository·**Users 제재 컬럼**·마이그레이션 | B5-4, B5-5 |
| 6 | 패널티 부여 API + **단계별 제재·참여 신청 검증** | B5-4 |
| 7 | 유저별 패널티 이력 API | B5-5 |
| 8 | 프로필 **제재 시각·주의 표시**·후기/평점 연동 (선택) | B3-6, B5-3, B5-5 |
| 9 | 해시태그 제안 API (선택) | B5-2 |
| 10 | API 문서 | B1-3 |

---

## 의존성 및 진행 순서

```
Step 1 (매칭 종료) → Step 2 (Review 엔티티) → Step 3 (후기 작성)
                              ↓
Step 4 (후기 조회)     Step 5 (Penalty 엔티티) → Step 6 → Step 7
Step 8~9 병렬 가능 (Step 3 이후 권장)
Step 10 마지막
```

- 패널티(Step 5~7)는 후기와 **독립** 병렬 가능하나, 동일 매칭 라이프사이클을 다루므로 **Step 1**과 참여자 검증 로직을 공유하면 된다.
- **에자일 워크플로우**: 할일 문서의 **Step 단위**로 구현 후, 사용자 리뷰(승인) 후 다음 Step 진행.

---

## 예상 신규/수정 클래스

### 신규 (예시 패키지: `org.app.mintonmatchapi.review`, `org.app.mintonmatchapi.penalty` 또는 도메인 통합)

- `review/entity/Review.java`, `ReviewSentiment.java`
- `review/repository/ReviewRepository.java`
- `review/service/ReviewService.java`
- `review/controller/ReviewController.java` (또는 `MatchController`에 매핑)
- `review/dto/ReviewCreateRequest.java`, `ReviewResponse.java`, `ReviewListResponse.java`
- `penalty/entity/Penalty.java`, `PenaltyType.java`
- `penalty/repository/PenaltyRepository.java`
- `penalty/service/PenaltyService.java`
- `penalty/controller/PenaltyController.java`
- `penalty/dto/PenaltyGrantRequest.java`, `PenaltyResponse.java`
- **`penalty/policy/SanctionPolicy.java`** 또는 설정 바인딩 클래스 — 스트라이크→`until` 갱신 규칙

### 수정

- `match/service/MatchService.java` — `finishMatch` (Step 1.1)
- `match/controller/MatchController.java` — `PATCH .../finish` (Step 1.1)
- **`match/schedule/MatchAutoFinishScheduler.java`** (또는 `config` 하위) — Step 1.2, `@EnableScheduling` 활성화
- `user/service/UserService.java` 또는 `ReviewService` 내 — **`ratingScore` 가중 갱신** (Step 3)
- **`match/service/MatchParticipantService.java`** — Step 6.4: `participation_banned_until`·정지 검증
- `user/entity/User.java` — `participationBannedUntil`, `suspendedUntil`, `accountStatus`, **(선택)** `penaltyPoints`
- `user/dto/ProfileResponse.java` — Step 8: 제재 시각·주의 표시
- `config/SecurityConfig.java` — 신규 경로 인증·인가
- `application.yml` — `match.*`, `review.*`, **`sanction.*`** (Step 6.3)
- `user/entity/User.java` — 신규 `ratingScore` **기본값 0** (기존이 5.0이면 Sprint 4에서 변경·마이그레이션 검토)

---

## 프론트엔드 참고 (별도 프로젝트)

`docs/스프린트플래닝.md` Sprint 4 프론트 범위 (본 저장소 비목표):

- 후기 작성: 3단계 좋아요, 점수, 해시태그, 상세 입력
- 유저 프로필: 후기 목록·패널티 이력
- 방장: 노쇼/지각 체크 UI → `POST .../penalties`

---

## Sprint 5와의 경계

- 채팅방 내 **지각/노쇼 버튼** UI는 Sprint 5 흐름과 맞닿지만, **패널티 API**는 Sprint 4에서 완성해 두고 프론트가 동일 엔드포인트를 호출하면 된다.
- 후기 작성 알림 등은 Sprint 5(알림)에서 확장 가능.
