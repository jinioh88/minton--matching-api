# Sprint 7 API 문서 (마이페이지 보강)

> 상세 할일: `docs/Sprint7-할일.md`  
> 프로필 공통: [Sprint1-API.md](./Sprint1-API.md) § 내 프로필 조회

---

## Step 1: 내 프로필 집계 (`GET /api/users/me`)

### 집계 정책

| 필드 | 정책 |
|------|------|
| **hostedMatchCount** | `Match.host_id = 본인`인 행 개수. **모든 `MatchStatus` 포함** (`CANCELLED` 포함). |
| **participatedMatchCount** | `match_id` 기준 **중복 없이** 집계: **방장인 매칭** ∪ **`MatchParticipant`가 본인이고 `status = ACCEPTED`인 매칭**. |

### 응답 필드 (본인만)

`GET /api/users/me`의 `data` 객체에 아래가 **추가**된다. **`GET /api/users/{userId}`**(타인) 응답에는 **포함되지 않음** (`null` / 생략).

| 필드 | 타입 | 설명 |
|------|------|------|
| hostedMatchCount | Long | 내가 방장으로 연 매칭 수 |
| participatedMatchCount | Long | 참여한 매칭 수(위 정책) |

### 응답 예시 (발췌)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "배드민턴왕",
    "hostedMatchCount": 12,
    "participatedMatchCount": 30,
    "receivedReviewCount": 5
  }
}
```

(나머지 필드는 [Sprint1-API.md](./Sprint1-API.md)와 동일.)

---

## Step 2: 매칭 내역 (개설 / 참여)

마이페이지 **별도 화면**용. **`인증 필수`**.

응답 `data`는 Spring `Page` — [`Sprint2-API.md`](./Sprint2-API.md) 매칭 목록의 `MatchListResponse` 항목과 동일 구조·의미 (`content`, `totalElements`, `number`, `size` 등).

### GET `/api/users/me/matches/hosted`

| 항목 | 내용 |
|------|------|
| 설명 | 내가 **방장**인 매칭 목록. **모든 `MatchStatus` 포함** (취소 포함). |
| 정렬 | 경기일 `matchDate` **내림차순**, 같은 날이면 `startTime` 내림차순 |

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| status | MatchStatus | X | `RECRUITING`, `CLOSED`, `FINISHED`, `CANCELLED` 등으로 필터 |
| dateFrom | LocalDate | X | 경기일 하한 (yyyy-MM-dd) |
| dateTo | LocalDate | X | 경기일 상한 (yyyy-MM-dd) |
| page | int | X | 기본 0 |
| size | int | X | 기본 20 |

### GET `/api/users/me/matches/participated`

| 항목 | 내용 |
|------|------|
| 설명 | 내가 **`MatchParticipant.status = ACCEPTED`** 인 매칭만 (확정 참가). 방장만 있고 게스트 행이 없는 매칭은 **개설 API**에서만 조회. |
| 정렬 | hosted와 동일 (경기일 최신순) |

**Query**: `hosted`와 동일 (`status`, `dateFrom`, `dateTo`, `page`, `size`). 여기서 `status`는 **`Match.status`** 필터.

---

## Step 3: 후기 별도 화면 (내가 쓴 후기 / 작성 대기)

마이페이지 **별도 화면**용. **`인증 필수`**. 응답은 공통 `PageResponse` 래핑 (`items`, `page`, `pageSize`, `totalElements`, `totalPages`, `first`, `last`).

### GET `/api/users/me/reviews/written`

| 항목 | 내용 |
|------|------|
| 설명 | **내가 작성한** 후기 목록. 작성자 본인 조회이므로 **내용·피평가자 정보는 항상 공개** (받은 후기 목록의 마스킹 규칙 미적용). |
| 정렬 | `createdAt` **내림차순** |

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| page | int | X | 기본 0 |
| size | int | X | 기본 20 |

**`items[]` 항목 (`WrittenReviewListItemResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| reviewId | Long | 후기 ID |
| match | object | `MatchReviewSummaryResponse` — `matchId`, `title`, `matchDate`, `startTime` |
| reviewee | object | `ReviewerPublicSummaryResponse` — 피평가자 `userId`, `nickname`, `profileImg` |
| sentiment | enum | `ReviewSentiment` |
| score | int | 점수 |
| hashtags | string[] | 해시태그 코드명 |
| detail | string \| null | 상세 |
| createdAt | datetime | 작성 시각 |

### GET `/api/users/me/reviews/pending`

| 항목 | 내용 |
|------|------|
| 설명 | **아직 작성하지 않은** 후기 후보. `Match.status = FINISHED` 이고, 본인이 **방장이거나** `MatchParticipant.status = ACCEPTED`로 **확정 참여**한 매칭에서, 같은 매칭의 **다른 확정 참여자**(방장 ∪ ACCEPTED, 본인 제외) 중 **해당 `(match, reviewee)` 쌍에 대한 후기를 아직 쓰지 않은** 항목만 포함. |
| 정렬 | `match_id` **내림차순**, 동일 매칭 내 `reviewee_id` **오름차순** |
| 작성 API | 기존 `POST /api/matches/{matchId}/reviews` — 요청 본문 `revieweeId`에 응답의 `revieweeId` 사용 |

**Query**: `written`과 동일 (`page`, `size`).

**`items[]` 항목 (`PendingReviewItemResponse`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| matchId | Long | 매칭 ID |
| match | object | `MatchReviewSummaryResponse` |
| revieweeId | Long | 후기 대상 사용자 ID (`POST` 시 `revieweeId`) |
| reviewee | object | `ReviewerPublicSummaryResponse` |
