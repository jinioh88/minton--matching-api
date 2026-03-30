# Sprint 7 할일 문서 (백엔드) — 마이페이지·운영·계정

> **전제**: `docs/스프린트플래닝.md` Sprint 7, `docs/제품백로그.md` Epic 9 (B9-1 ~ B9-7), `docs/요구사항분석.md` 「마이페이지·계정」.
>
> **원칙**: 마이페이지 상단 요약(`GET /api/users/me`)에는 **집계(숫자) 위주**를 두고, **매칭 내역·후기 목록은 전부 별도 화면**에서 조회한다 (Step 0 합의). **B9-5·B9-6·B9-7(공지·고객센터·계정 탈퇴 등)은 본 스프린트에서 제외**한다.

## 기존 API (확장·연동 대상)

| 메서드 | 경로 | DTO | 비고 |
|--------|------|-----|------|
| GET | `/api/users/me` | `ProfileResponse` | **집계 필드** (`hostedMatchCount`, `participatedMatchCount` 등) |
| GET | `/api/users/me/matches/hosted` | `Page<MatchListResponse>` | 개설 매칭 목록 (Step 2) |
| GET | `/api/users/me/matches/participated` | `Page<MatchListResponse>` | 확정 참가 매칭 목록 (Step 2) |
| GET | `/api/users/me/reviews/written`, `/api/users/me/reviews/pending` | `WrittenReviewListItemResponse`, `PendingReviewItemResponse` | **후기 별도 화면** (Step 3) |
| PATCH | `/api/users/me` | `ProfileUpdateRequest` → `ProfileResponse` | 본 스프린트 필수 아님 |
| GET | `/api/users/{userId}` | `ProfileResponse` | 타인 — 본인 전용 필드 미포함 |

---

## Sprint 7 목표 (백엔드)

- **`ProfileResponse`**: P1·P2 집계 필드 추가 (`UserService#getMyProfile`).
- **별도 화면용 API**: 개설/참여 **매칭 목록**, **내가 쓴 후기**·**작성 대기(미작성) 후기** 목록(또는 구분 조회).
- **제외**: 공지·고객센터 URL API, 탈퇴·연동 요약 전용 확장 등 **B9-5 ~ B9-7**은 **후속 스프린트**.

### 완료 조건 (백엔드)

- [x] B9-1: **개설·참여 매칭 수**가 `GET /api/users/me`에 포함(본인만), P1·P2 집계 규칙 준수
- [x] B9-2 / B9-3: **`GET /api/users/me/matches/hosted`**, **`participated`** — [Sprint7-API.md](../common-docs/api/Sprint7-API.md) §Step 2
- [x] B9-4: **별도 화면**에서 **내가 쓴 후기**와 **아직 안 쓴 후기(작성 대기)** 를 조회할 수 있는 API (기존 Review 도메인·익명·공개 규칙 준수) — [Sprint7-API.md](../common-docs/api/Sprint7-API.md) §Step 3
- [ ] ~~B9-5 / B9-6~~ **본 스프린트 제외**
- [ ] ~~B9-7~~ **본 스프린트 제외** (탈퇴·계정 연동 요약 API 미구현)
- [x] `GET /api/users/{id}`(타인) 응답에 본인 전용 필드 미포함 — `ProfileResponse.ofOther`·`UserService#getUserProfile` (집계 미호출)
- [ ] 회귀 테스트: `getMyProfile`, 신규/확장 목록 API *(테스트 코드 제외)*

---

## Step 0: 정책·응답 스키마 합의

### Step 0 진행 상태

- [x] **P1 ~ P4** 제품 합의 반영
- [x] **P5·P6** — **현재 스프린트 제외**로 확정 (구현·백로그는 후속)
- [x] `Sprint7-API.md`에 집계 정책 + 별도 화면용 API 경로·쿼리 반영

### 합의 확정 (제품)

| ID | 항목 | 확정 내용 |
|----|------|-----------|
| **P1** | **개설 매칭 수** (`hostedMatchCount`) | `Match.host_id = 본인`인 행을 **모든 `MatchStatus`에 대해 집계**. **`CANCELLED` 포함**. |
| **P2** | **참여 매칭 수** (`participatedMatchCount`) | (1) `MatchParticipant`에서 `user_id = 본인`·`status = ACCEPTED`인 매칭 수 + (2) **방장으로 개설한 매칭** 수. 동일 `match_id` **중복 집계 방지**. |
| **P3** | **매칭 내역 UI** | **전부 별도 화면**에서만 본다. `GET /api/users/me`에는 **매칭 목록/프리뷰를 넣지 않음** (숫자 집계만). |
| **P4** | **후기 UI** | **내가 쓴 후기**와 **아직 안 쓴 후기(작성 대기)** 를 보여준다. **이것도 별도 화면** — 프로필 응답에 후기 **목록**을 넣지 않고, **별도 조회 API**로 제공한다. |
| **P5** | 공지·URL 등 | **현재 스프린트 제외** (후속). |
| **P6** | 탈퇴·계정 쓰기 등 | **현재 스프린트 제외** (후속). |

**산출**: P1·P2·P3·P4를 `Sprint7-API.md` 「정책」에 옮기고, `ProfileResponse`·별도 화면용 API 스키마를 기술한다.

---

## Step 1: `ProfileResponse` 집계만 (B9-1)

### 1.1 DTO

- `ProfileResponse`에 **본인 전용** 필드 추가: `hostedMatchCount`, `participatedMatchCount` (필드명은 API 문서와 통일).
- **매칭 프리뷰·후기 목록 필드는 추가하지 않음** (P3·P4 합의).

### 1.2 서비스

- `UserService#getMyProfile` / `toProfileMe`에서 P1·P2 규칙으로 Repository 집계 호출.
- **타인** `ofOther`에서는 위 카운트 필드 **미설정**.

### 1.3 문서·테스트

- 본인/타인 JSON 예시, 집계 단위 테스트.

---

## Step 2: 매칭 내역 별도 화면용 조회 (B9-2, B9-3) — 구현됨

- **경로**: `GET /api/users/me/matches/hosted`, `GET /api/users/me/matches/participated` (`UserController` → `MatchService` → `MatchRepositoryCustom` QueryDSL).
- **참여 목록**: `MatchParticipant.status = ACCEPTED` 만 포함. 정렬: 경기일·시작시각 **내림차순**. 선택 쿼리: `status`(Match), `dateFrom`/`dateTo`(경기일), `page`, `size`.
- **문서**: [Sprint7-API.md](../common-docs/api/Sprint7-API.md) §Step 2.

---

## Step 3: 후기 별도 화면용 조회 (B9-4) — 구현됨

- **경로**: `GET /api/users/me/reviews/written`, `GET /api/users/me/reviews/pending` (`UserController` → `ReviewService` → `ReviewRepository` / 네이티브 pending 쿼리).
- **작성 대기**: FINISHED 매칭·확정 참여(방장 또는 ACCEPTED)에서, 본인이 아직 `(match, reviewee)` 후기를 쓰지 않은 쌍 — `ReviewService#listPendingRevieweeIds`와 동일 도메인.
- **문서**: [Sprint7-API.md](../common-docs/api/Sprint7-API.md) §Step 3.

---

## ~~Step (제외): 공지·고객센터 (B9-5, B9-6)~~

- **본 스프린트 미포함** (P5 합의). 후속 스프린트에서 `제품백로그` Epic 9 재검토.

---

## ~~Step (제외): 계정 탈퇴·연동 요약 (B9-7)~~

- **본 스프린트 미포함** (P6 합의). 후속 스프린트에서 구현.

---

## 공통

| # | 작업 |
|---|------|
| C1 | `common-docs/api/Sprint7-API.md` — 집계 정책, `ProfileResponse`, Step 2·3 API |
| C2 | `SecurityConfig` — 본인 전용 경로 허용 범위 |
| C3 | 테스트 — 집계, 목록 권한, 후기 마스킹 *(선택·제외 가능)* |

---

## Step 완료 후 리뷰

- Step마다 사용자 리뷰 후 다음 Step 진행 (`.cursor/rules/agile-workflow.mdc`).

---

## 참고 코드 위치

- `UserController#getMyProfile`, `ProfileResponse`, `UserService#toProfileMe`
- `MatchController`, `MatchService`, `MatchRepository` / QueryDSL
- `ReviewController`·`ReviewService` (및 관련 Repository)
