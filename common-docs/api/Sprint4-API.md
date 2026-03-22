# Sprint 4 API 문서

> 후기(작성·목록·마스킹), 매칭 종료(FINISHED), 패널티·단계별 제재, 프로필 제약 필드  
> 참조: [Sprint1-API.md](./Sprint1-API.md), [Sprint2-API.md](./Sprint2-API.md), [Sprint3-API.md](./Sprint3-API.md)  
> 요구·정책: `docs/요구사항분석.md`, `docs/Sprint4-할일.md`

---

## API 목록 (Sprint 4)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| PATCH | /api/matches/{matchId}/finish | **필요** | 매칭 수동 종료 (방장, CLOSED → FINISHED) |
| POST | /api/matches/{matchId}/reviews | **필요** | 후기 작성 |
| GET | /api/users/{userId}/reviews | 선택 (`IfLogin`) | 받은 후기 목록 (비로그인 시 내용 마스킹) |
| POST | /api/matches/{matchId}/penalties | **필요** | 패널티 부여 (방장) |
| GET | /api/users/{userId}/penalties | 불필요 | 받은 패널티 이력 |
| GET | /api/users/me | **필요** | 내 프로필 (Step 8: 제재·후기 건수 필드 확장) |
| GET | /api/users/{userId} | 불필요 | 타인 프로필 (제약 시각 미포함 등) |

**매칭 상세 (`GET /api/matches/{matchId}`) Sprint 4 확장**  
- `canFinishMatch`, `reviewPendingUserIds` — 아래 [매칭 상세 확장 필드](#매칭-상세-확장-필드-get-apimatchesmatchid) 참조.

**배치(HTTP 아님)**  
- 매칭 **자동 FINISHED** 스케줄러: `MatchAutoFinishScheduler` — 아래 [자동 종료 스케줄러](#자동-종료-스케줄러) 참조.

**보류 (Step 9)**  
- `GET /api/reviews/hashtag-suggestions` — 추후 개발 시 문서화.

---

## 매칭 상태 전이 (요약)

| 전이 | 조건 |
|------|------|
| RECRUITING → CLOSED | 기존 Sprint 2·3 흐름 |
| CLOSED → **FINISHED** | (1) 방장 `PATCH .../finish` (2) 자동 배치: **경기 시작 시각** 기준 N시간 경과 ([자동 종료 스케줄러](#자동-종료-스케줄러)) |
| * → FINISHED | 후기·패널티 부여 전제는 **FINISHED** |

---

## 자동 종료 스케줄러

| 항목 | 내용 |
|------|------|
| 클래스 | `MatchAutoFinishScheduler` |
| 실행 | Spring `@Scheduled`, 기본 cron `match.auto-finish-cron` (기본값 `0 0 * * * *` — 매시 정각) |
| 락 | ShedLock `autoFinishMatches` (다중 인스턴스 시 단일 실행) |
| 조건 | `status = CLOSED` 이고 **경기 시작 시각** `matchDate.atTime(startTime)` ≤ `now - match.auto-finish-after-start-hours` (`MatchService.autoFinishMatches`의 `cutoff`). `durationMin`은 자동 종료 조건에 **포함되지 않음**. 구현: `MatchRepositoryImpl.findClosedMatchIdsStartedOnOrBefore` + 벌크 `UPDATE` |
| 설정 키 | `match.auto-finish-after-start-hours`, `match.auto-finish-cron`, `match.require-past-start-for-manual-finish` (`application.yml`) |
| 비고 | 벌크 갱신 후 `EntityManager.flush` + `clear` 로 1차 캐시 불일치 방지 |

---

## 설정 (`application.yml` 요약)

### `match.*` (수동·자동 종료)

```yaml
match:
  auto-finish-after-start-hours: 10   # 예시
  auto-finish-cron: "0 0 * * * *"
  require-past-start-for-manual-finish: true
```

### `review.*` (평점·후기 목록 공개 유예)

```yaml
review:
  rating-prior-count: 10          # M (가상 표본 수)
  rating-prior-mean: 5.0          # priorMean (내부 가중용)
  reveal-after-finish-hours: 72   # 모임 일정 종료 기준 시각 이후 경과 시 내용 공개 후보
```

### `sanction.*` (패널티·단계별 제재)

```yaml
sanction:
  strike-thresholds:
    participation-ban: 3      # 누적 penaltyCount ≥ 이면 participation_banned_until 갱신
    full-suspension: 5
    permanent-ban: 10
  durations:
    participation-ban-days: 3
    suspension-days: 7
  penalty-weights:
    NO_SHOW: 2
    LATE: 1
```

- 스트라이크 판정은 **`penaltyCount`**(패널티 부여 1회당 +1) 기준. `penalty_points`는 가중치 합산용.
- `participation_banned_until` / `suspended_until`은 **기존보다 짧은 기간으로 덮어쓰지 않음** (`max` 병합).

---

## 평점 갱신 (`User.rating_score`)

후기 저장 직전 피평가자의 받은 후기 건수를 `n`, 저장 평점을 `R`(무후기 구간에서는 수식에 `0`), 새 점수 `s`, 프라이어 `M`·`priorMean`은 `review.rating-prior-count` / `review.rating-prior-mean`.

| 조건 | 수식 |
|------|------|
| `n = 0` | `R' = (M × priorMean + s) / (M + 1)` |
| `n ≥ 1` | `R' = (R × (M + n) + s) / (M + n + 1)` |

구현: `ReviewRatingCalculator`, `docs/요구사항분석.md` 평점 절 참고.

---

## 페이징 공통 (`PageResponse`)

후기·패널티 목록 API는 Spring Data `Page` 대신 **`PageResponse<T>`** 로 감싸 직렬화한다 (`org.app.mintonmatchapi.common.dto.PageResponse`).

| 필드 | 타입 | 설명 |
|------|------|------|
| items | T[] | 기존 Spring `Page.content` |
| page | int | 0부터 페이지 번호 (기존 `number`) |
| pageSize | int | 페이지 크기 (기존 `size`) |
| totalElements | long | 전체 행 수 |
| totalPages | int | 전체 페이지 수 |
| first | boolean | 첫 페이지 여부 |
| last | boolean | 마지막 페이지 여부 |

쿼리 파라미터는 기존과 동일: `?page=0&size=20`.

---

## 매칭 상세 확장 필드 (`GET /api/matches/{matchId}`)

Sprint 2·3 필드에 더해 **항상** 아래가 포함된다 (비로그인 시에도).

| 필드 | 타입 | 설명 |
|------|------|------|
| canFinishMatch | boolean | 로그인 사용자가 **해당 매칭 방장**이고 `status === CLOSED`이며, `match.require-past-start-for-manual-finish`가 true면 경기 시작 시각이 현재 이전(또는 동시)일 때만 `true`. 그 외(비로그인·타인·다른 상태)는 `false`. `PATCH .../finish`와 동일 판정. |
| reviewPendingUserIds | Long[] | `FINISHED`이고 로그인 사용자가 해당 매칭 **확정 참여자**(방장 또는 ACCEPTED)일 때, **본인을 제외**한 확정 참여자 중 **아직 본인이 후기를 작성하지 않은** `userId` 목록. 순서: 방장 → ACCEPTED 참가자(대기열 순). 그 외에는 빈 배열 `[]`. |

프론트는 [모임 종료] 버튼에 `canFinishMatch`, 후기 작성 대상 선택에 `reviewPendingUserIds`(또는 `confirmedParticipants`와 조합)를 사용할 수 있다.

---

## 상세 API

### 1. 매칭 수동 종료

**PATCH** `/api/matches/{matchId}/finish`

**인증** 필요 (방장만)

**전제**

- `Match.status == CLOSED`
- `match.require-past-start-for-manual-finish: true`이면 경기 시작 시각(`matchDate` + `startTime`)이 현재 이전(또는 동시)이어야 함

**Response**  
`ApiResponse<MatchResponse>` — Sprint2 `MatchResponse`와 동일 형태 (`status`: `FINISHED`).

**에러**

| 코드 | 설명 |
|------|------|
| MATCH_NOT_FOUND | 매칭 없음 |
| FORBIDDEN | 방장 아님 |
| INVALID_MATCH_STATUS | CLOSED 아님, 또는 수동 종료 시각 조건 불충족 |

---

### 2. 후기 작성

**POST** `/api/matches/{matchId}/reviews`

**인증** 필요

**작성 가능 조건**

- 매칭 **`FINISHED`**
- 작성자·피평가자 모두 해당 매칭 **확정 참여**: 방장은 `Match.host`로 간주, 게스트는 `MatchParticipant` **ACCEPTED**
- 작성자 ≠ 피평가자
- 동일 `(matchId, reviewerId, revieweeId)` 중복 불가
- 작성자 계정: **BANNED / 정지 기간(`suspendedUntil`)** 이면 불가 (`USER_BANNED`, `USER_SUSPENDED`)
- 참여 제한(`participationBannedUntil`)만 있는 경우에는 **후기 작성은 가능** (참여 신청과 정책 분리)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| revieweeId | Long | O | 피평가자 사용자 ID |
| sentiment | String | O | `NEGATIVE`, `NEUTRAL`, `POSITIVE` |
| score | int | O | 1 ~ 5 |
| hashtags | String[] | X | 최대 10, 코드명: `PASSION`, `MANNER`, `KINDNESS`, `EXPERT`, `PUNCTUAL`, `TEAM_PLAY` |
| detail | String | X | 최대 2000자 |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "reviewId": 12,
    "matchId": 3,
    "revieweeId": 8,
    "sentiment": "POSITIVE",
    "score": 5,
    "hashtags": ["MANNER", "PUNCTUAL"],
    "detail": "시간 약속 잘 지켰어요.",
    "createdAt": "2025-03-22T10:00:00"
  }
}
```

**에러**

| 코드 | 설명 |
|------|------|
| MATCH_NOT_FOUND | 매칭 없음 |
| REVIEW_NOT_ALLOWED | FINISHED 아님, 확정 참여 아님 등 |
| SELF_REVIEW_NOT_ALLOWED | 본인에게 후기 불가 |
| DUPLICATE_REVIEW | 중복 후기 |
| USER_NOT_FOUND | 피평가자 없음 |
| USER_SUSPENDED / USER_BANNED | 작성자 제재 |
| BAD_REQUEST | 해시태그 형식 오류 등 |

---

### 3. 받은 후기 목록

**GET** `/api/users/{userId}/reviews?page=0&size=20`

**인증** 선택 — 비로그인 허용. 로그인 시 `Authorization` 있으면 조회자 ID로 공개 여부 판단.

**페이징**  
`data`는 [페이징 공통 (`PageResponse`)](#페이징-공통-pageresponse) 형식이며, 후기 행은 `data.items`이다.

**에러**  
`userId`에 해당 사용자가 없으면 `NOT_FOUND`(404).

**공개 규칙 (`contentRevealed`)**

- **비로그인**: 항상 `contentRevealed: false` (내용·작성자 식별 마스킹).
- **로그인**  
  - 조회자가 **작성자** 본인 → 항상 공개  
  - **유예 경과**: 기준 시각 = `matchDate` + `startTime` + `durationMin` 이후 `review.reveal-after-finish-hours` 경과 시 공개  
  - 조회자가 **피평가자** → 해당 매칭에서 작성자↔피평가자 **양방향 후기** 존재 시 공개  
  - **제3자** → 해당 매칭에서 조회자↔작성자 **양방향 후기** 존재 시 공개  

**마스킹 시** (`contentRevealed: false`)  
`reviewer`, `sentiment`, `score`, `hashtags`, `detail`은 응답에서 생략(null). `reviewId`, `match`, `createdAt`, `reviewSubmitted: true` 등 유지.

**Response 예시 (마스킹 행, `PageResponse`)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "reviewId": 12,
        "match": {
          "matchId": 3,
          "title": "주말 게임",
          "matchDate": "2025-03-20",
          "startTime": "14:00:00"
        },
        "createdAt": "2025-03-22T10:00:00",
        "contentRevealed": false,
        "reviewSubmitted": true
      }
    ],
    "page": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 4. 패널티 부여

**POST** `/api/matches/{matchId}/penalties`

**인증** 필요 (해당 매칭 **방장**만)

**전제**

- 매칭 **`FINISHED`**
- 대상은 해당 매칭 **ACCEPTED** 참여자, **방장 본인 불가**
- 동일 `(matchId, penalizedUserId, type)` 중복 불가

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 패널티 대상 |
| type | String | O | `NO_SHOW`, `LATE` |

**부여 후 서버 동작 (요약)**

- `Penalty` 저장, 대상 `penaltyCount += 1`, `penalty_points += sanction.penalty-weights[type]`
- 누적 `penaltyCount`로 `participation_banned_until` / `suspended_until` / `accountStatus` 갱신 (`sanction.*` 임계·일수)

**에러**

| 코드 | 설명 |
|------|------|
| MATCH_NOT_FOUND | 매칭 없음 |
| FORBIDDEN | 방장 아님 |
| INVALID_MATCH_STATUS | FINISHED 아님 |
| INVALID_PENALTY_TARGET | 대상 불가(비참여, 방장 본인 등) |
| DUPLICATE_PENALTY | 동일 유형 이미 부여 |
| USER_NOT_FOUND | 대상 사용자 없음 |

---

### 5. 받은 패널티 이력

**GET** `/api/users/{userId}/penalties?page=0&size=20`

**인증** 불필요

**페이징**  
`data`는 [페이징 공통 (`PageResponse`)](#페이징-공통-pageresponse) 형식이며, 행 목록은 `data.items`이다.

**Response 행 필드 (요약)**

| 필드 | 설명 |
|------|------|
| penaltyId | PK |
| type | `NO_SHOW` / `LATE` |
| match | `matchId`, `title`, `matchDate`, `startTime` |
| createdAt | 부여 시각 |
| hostNickname | 부여 방장 닉네임 |

`ProfileResponse.penaltyCount`와 목록 `PageResponse.totalElements`는 동일 사용자 기준으로 정합.

---

### 6. 프로필 (`GET /api/users/me`, `GET /api/users/{userId}`)

Step 8 확장 필드:

| 필드 | 본인 (`/me`) | 타인 |
|------|----------------|------|
| participationBannedUntil | O (ISO, null 가능) | **미포함** |
| suspendedUntil | O | **미포함** |
| accountStatus | O (`ACTIVE`, `SUSPENDED`, `BANNED`) | **미포함** |
| showCautionBadge | O | O (`penaltyCount`가 1 ~ 참여금지임계-1) |
| receivedReviewCount | O | O (`count(reviewee_id)`) |

null 필드는 `@JsonInclude(NON_NULL)`로 생략될 수 있음.

**타인 프로필**에 “최근 후기 요약·태그 Top-N”은 Sprint 4에서 `ratingScore` + `receivedReviewCount`로 최소 요약. 확장 시 별도 필드/집계 검토.

---

## 참여 신청 연동 (`POST .../participants`)

`MatchParticipantService.applyParticipant` 상단에서 검사:

| 순서 | 조건 | 코드 |
|------|------|------|
| 1 | `accountStatus == BANNED` | `USER_BANNED` |
| 2 | `suspendedUntil != null && isAfter(now)` | `USER_SUSPENDED` |
| 3 | `participationBannedUntil != null && isAfter(now)` | `USER_PARTICIPATION_BANNED` |

메시지 예: 패널티 누적으로 참여 제한 등.

---

## Sprint 4 관련 에러 코드 (추가·활용)

| 코드 | HTTP | 용도 |
|------|------|------|
| INVALID_MATCH_STATUS | 400 | 종료/상태 전이 불가 등 |
| REVIEW_NOT_ALLOWED | 400 | 후기 작성 조건 불충족 |
| SELF_REVIEW_NOT_ALLOWED | 400 | 본인 후기 |
| DUPLICATE_REVIEW | 400 | 후기 중복 |
| USER_NOT_FOUND | 404 | 사용자 없음 |
| INVALID_PENALTY_TARGET | 400 | 패널티 대상 불가 |
| DUPLICATE_PENALTY | 400 | 패널티 중복 |
| USER_PARTICIPATION_BANNED | 400 | 참여 신청 제한 기간 |
| USER_SUSPENDED | 403 | 정지 기간 중 쓰기 |
| USER_BANNED | 403 | 영구 제재 |

공통 응답 형식: `ErrorResponse` — `success: false`, `code`, `message` (`GlobalExceptionHandler`).

---

## 호스트 요약 (`HostSummary`)

매칭 상세 등에서 방장 요약의 `ratingScore`는 **`User.ratingScore`** 이며, 후기 작성 시 `ReviewService`에서 가중 갱신된다 (`HostSummary.from(User)`).

---

## DB 마이그레이션 (운영 Flyway)

- `V2__add_reviews_tables.sql` — `reviews`, `review_hashtags`
- `V3__penalties_and_user_sanctions.sql` — `penalties`, `users` 제재 컬럼

로컬 기본 프로파일은 Flyway 비활성 + Hibernate `ddl-auto: update` 사용 가능.

---

*문서 버전: Sprint 4 구현 기준. Step 9 해시태그 제안 API는 추후 반영.*
