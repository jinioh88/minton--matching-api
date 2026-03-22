# Sprint 3 API 문서

> 참여 신청, 수락/거절, 대기열, 취소 API  
> 참조: [Sprint1-API.md](./Sprint1-API.md), [Sprint2-API.md](./Sprint2-API.md)  
> 매칭 인원 정책(maxPeople, currentPeople)은 Sprint2 API 문서 참조.

---

## 대기열 하이브리드 시스템 (Hybrid Timeout System)

참여 취소 시 대기열이 동작하는 방식입니다.

| 단계 | 설명 |
|------|------|
| **1. 순차 기회** | ACCEPTED 취소 시 → 대기 1번에게 RESERVED 부여 (15분 내 수락) |
| **2. 타임아웃** | 15분 내 미응답/거절 → 대기 상태 해제(CANCELLED), 다음 대기 1번에게 기회 이전 |
| **3. 긴급 선착순** | 경기 2시간 미만 시 → WAITING 전체가 accept-offer 호출 가능, 먼저 수락한 사람 확정 |

---

## API 목록 (Sprint 3)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| PATCH | /api/matches/{matchId} | **필요** | 매칭 수정 (방장, 모집 중만) |
| PATCH 또는 POST | /api/matches/{matchId}/cancel | **필요** | **모임 취소** (방장, RECRUITING·CLOSED → CANCELLED). POST는 PATCH가 환경에서 매핑되지 않을 때 대안 |
| POST | /api/matches/{matchId}/participants | **필요** | 참여/대기 신청 |
| PATCH | /api/matches/{matchId}/participants/{participationId} | **필요** | 방장 수락/거절/추방 |
| DELETE | /api/matches/{matchId}/participants/me | **필요** | 참여 취소 |
| POST | /api/matches/{matchId}/participants/me/accept-offer | **필요** | 예약 수락 (대기열 사용자) |
| POST | /api/matches/{matchId}/participants/me/reject-offer | **필요** | 예약 거절 (대기열 사용자) |
| GET | /api/matches/{matchId}/participants/applications | **필요** | 방장용 신청 목록 조회 |

---

## 상세 API

### 1. 참여/대기 신청

**POST** `/api/matches/{matchId}/participants`

매칭에 참여 신청합니다. 정원 여유 시 PENDING, 정원 초과 시 WAITING(대기열)로 등록됩니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| applyMessage | String | X | 참여 멘트 (최대 200자) |

**Request 예시**

```json
{
  "applyMessage": "함께하고 싶습니다!"
}
```

**Response 예시**

```json
{
  "success": true,
  "data": {
    "participationId": 5,
    "status": "PENDING",
    "queueOrder": 0,
    "applyMessage": "함께하고 싶습니다!",
    "offerExpiresAt": null
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| participationId | Long | 참여 내역 ID |
| status | ParticipantStatus | PENDING(참여 대기) 또는 WAITING(대기열) |
| queueOrder | Integer | 대기 순번 (PENDING=0, WAITING=1,2,3...) |
| applyMessage | String | 신청 멘트 |
| offerExpiresAt | LocalDateTime | RESERVED일 때만 (수락 마감 시각) |

**에러**

| 코드 | 설명 |
|------|------|
| MATCH_NOT_FOUND | 매칭 없음 |
| MATCH_NOT_RECRUITING | 모집 중 아님 |
| ALREADY_APPLIED | 이미 신청한 상태 |
| HOST_CANNOT_APPLY | 방장은 참여 신청 불가 |

---

### 2. 방장 수락/거절/추방

**PATCH** `/api/matches/{matchId}/participants/{participationId}`

방장이 참여 신청을 수락·거절하거나, 확정된 참여자를 추방합니다.

**인증** 필요 (방장만 호출 가능)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |
| participationId | Long | 참여 내역 ID (match_participants PK). 수락 대기 목록 또는 확정 참여자 목록에서 획득 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| action | String | O | ACCEPT, REJECT, KICK. KICK은 ACCEPTED(확정) 참여자만 가능 |

**Request 예시**

```json
{ "action": "ACCEPT" }
```
```json
{ "action": "REJECT" }
```
```json
{ "action": "KICK" }
```

**Response 예시**

ACCEPT 시:
```json
{
  "success": true,
  "data": {
    "participationId": 5,
    "status": "ACCEPTED",
    "queueOrder": 0,
    "applyMessage": "함께하고 싶습니다!",
    "offerExpiresAt": null
  }
}
```

KICK 시 (추방된 참여자는 status=CANCELLED):
```json
{
  "success": true,
  "data": {
    "participationId": 5,
    "status": "CANCELLED",
    "queueOrder": 0,
    "applyMessage": "함께하고 싶습니다!",
    "offerExpiresAt": null
  }
}
```

**액션별 동작**

| action | 대상 상태 | participationId 출처 | 동작 |
|--------|----------|---------------------|------|
| ACCEPT | PENDING, WAITING | GET /participants/applications | 확정(ACCEPTED). 정원 초과 시 MATCH_FULL |
| REJECT | PENDING, WAITING | GET /participants/applications | 거절(REJECTED) |
| KICK | ACCEPTED | GET /matches/{id} → confirmedParticipants | 추방(CANCELLED). 대기열 승격 트리거 |

**에러**

| 코드 | 설명 |
|------|------|
| FORBIDDEN | 방장이 아님 |
| PARTICIPANT_NOT_FOUND | 참여 내역 없음 |
| INVALID_STATUS | 이미 수락/거절된 신청, 또는 KICK 시 ACCEPTED가 아님 |
| MATCH_FULL | 수락 시 정원 초과 |

---

### 3. 참여 취소

**DELETE** `/api/matches/{matchId}/participants/me`

현재 사용자의 참여/대기 신청을 취소합니다. ACCEPTED 취소 시 대기열 승격이 트리거됩니다.

**인증** 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Response 예시**

```json
{
  "success": true,
  "data": null
}
```

**에러**

| 코드 | 설명 |
|------|------|
| PARTICIPANT_NOT_FOUND | 참여 내역 없음 |
| CANNOT_CANCEL | 취소 불가 상태 |

---

### 4. 예약 수락 (대기열 사용자)

**POST** `/api/matches/{matchId}/participants/me/accept-offer`

RESERVED 상태인 본인이 참석 기회를 수락합니다. 긴급 모드(경기 2시간 미만)에서는 WAITING도 호출 가능(선착순).

**인증** 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "participationId": 5,
    "status": "ACCEPTED",
    "queueOrder": 0,
    "applyMessage": "함께하고 싶습니다!",
    "offerExpiresAt": null
  }
}
```

**에러**

| 코드 | 설명 |
|------|------|
| PARTICIPANT_NOT_FOUND | 참여 내역 없음 |
| INVALID_STATUS | 예약 수락 가능한 상태 아님 |
| OFFER_EXPIRED | 예약 기회 만료 |
| MATCH_FULL | 정원 초과 |

---

### 5. 예약 거절 (대기열 사용자)

**POST** `/api/matches/{matchId}/participants/me/reject-offer`

RESERVED 상태인 본인이 참석 기회를 거절합니다. 대기 상태 해제(CANCELLED) 후 다음 대기자에게 기회가 이전됩니다.

**인증** 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "participationId": 5,
    "status": "CANCELLED",
    "queueOrder": 0,
    "applyMessage": "함께하고 싶습니다!",
    "offerExpiresAt": null
  }
}
```

---

### 6. 방장 신청 목록 조회

**GET** `/api/matches/{matchId}/participants/applications`

방장이 PENDING/WAITING/RESERVED 신청 목록을 조회합니다.

**인증** 필요 (방장만 호출 가능)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Response 예시**

```json
{
  "success": true,
  "data": [
    {
      "participationId": 3,
      "userId": 5,
      "nickname": "참가자A",
      "profileImg": "https://...",
      "ratingScore": 4.8,
      "level": "B",
      "interestRegions": ["SEOUL_GANGNAM", "SEOUL_JONGNO"],
      "status": "PENDING",
      "queueOrder": 0,
      "applyMessage": "함께하고 싶습니다",
      "appliedAt": "2025-03-15T10:00:00",
      "offerExpiresAt": null
    },
    {
      "participationId": 4,
      "userId": 6,
      "nickname": "참가자B",
      "profileImg": "https://...",
      "ratingScore": 4.5,
      "level": "A",
      "interestRegions": ["INCHEON"],
      "status": "RESERVED",
      "queueOrder": 1,
      "applyMessage": "대기열에서 기다리고 있어요",
      "appliedAt": "2025-03-15T09:30:00",
      "offerExpiresAt": "2025-03-15T10:45:00"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| level | String | 신청자 급수 (A, B, C, D, BEGINNER). 방장 수락/거절 판단용 |
| interestRegions | String[] | 관심 지역 (최대 2곳). 방장 수락/거절 판단용 |

정렬: PENDING → RESERVED → WAITING (queueOrder ASC)

---

### 7. 매칭 수정 (Step 8)

**PATCH** `/api/matches/{matchId}`

방장이 모집 중(RECRUITING)인 매칭의 정보를 수정합니다. **partial update**: 요청에 포함된 필드만 변경되며, null/미포함 필드는 기존 값 유지.

**인증** 필요 (방장만 호출 가능)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Request Body** (모든 필드 선택)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| status | MatchStatus | X | **`CLOSED`만** 허용. RECRUITING → CLOSED(모집 마감). 이미 CLOSED면 동일 요청 시 멱등(200). 그 외 값은 `BAD_REQUEST` |
| title | String | X | 제목 (최대 100자, 비워둘 수 없음) |
| description | String | X | 상세 설명 (비워둘 수 없음) |
| matchDate | LocalDate | X | 경기 날짜 (yyyy-MM-dd, 오늘 이후) |
| startTime | LocalTime | X | 시작 시간 (HH:mm, 오늘 경기 시 현재 시간 이후) |
| durationMin | Integer | X | 소요 시간(분, 30~240) |
| locationName | String | X | 장소명 (최대 200자) |
| regionCode | String | X | 행정구역 코드 (7~10자리 숫자) |
| maxPeople | Integer | X | 방장 포함 총 확정 인원 상한 (2~12). **현재 확정 인원 이상** 필수 |
| targetLevels | String | X | 희망 급수 (예: "A,B,C") |
| costPolicy | CostPolicy | X | 비용 분담 방식 (SPLIT_EQUAL, HOST_PAYS, GUEST_PAYS) |
| imageUrl | String | X | 매칭 대표 이미지 URL (최대 2000자) |
| latitude | Double | X | 위도 |
| longitude | Double | X | 경도 |

**Request 예시 (일부 필드만 수정)**

```json
{
  "title": "수정된 제목",
  "maxPeople": 6
}
```

**Response 예시**

```json
{
  "success": true,
  "data": {
    "matchId": 1,
    "hostId": 1,
    "title": "수정된 제목",
    "description": "초보 환영합니다.",
    "matchDate": "2025-03-15",
    "startTime": "14:00",
    "durationMin": 120,
    "locationName": "OO체육관",
    "regionCode": "1168010100",
    "maxPeople": 6,
    "targetLevels": "B,C",
    "costPolicy": "SPLIT_EQUAL",
    "status": "RECRUITING",
    "imageUrl": "https://...",
    "latitude": null,
    "longitude": null,
    "createdAt": "2025-03-11T12:00:00"
  }
}
```

**에러**

| 코드 | HTTP | 설명 |
|------|------|------|
| MATCH_NOT_FOUND | 404 | 매칭 없음 |
| FORBIDDEN | 403 | 방장이 아님 |
| MATCH_NOT_RECRUITING | 400 | 모집 중 아님 (CLOSED, CANCELLED 등) |
| BAD_REQUEST | 400 | 정원을 현재 확정 인원보다 낮게 수정 시도 / 제목·설명 비움 / 날짜·시간 유효성 위반 / `status`에 CLOSED 외 값 |

**모임 취소는 `PATCH` 본문의 `status: CANCELLED`가 아니라 전용 API를 사용합니다.**

### 7-1. 모임 취소 (매칭 CANCELLED)

**PATCH** 또는 **POST** `/api/matches/{matchId}/cancel`

방장이 모임 전체를 취소합니다. Request body 없음. (동일 서버에서 **POST**도 같은 처리로 허용)

| 전제 | 설명 |
|------|------|
| 권한 | 방장만 |
| 허용 상태 | **RECRUITING**, **CLOSED** |
| 불가 | **FINISHED**, 그 외 |
| 멱등 | 이미 **CANCELLED**이면 200·동일 응답 |

**에러**

| 코드 | 설명 |
|------|------|
| INVALID_MATCH_STATUS | 종료(FINISHED) 후 취소 시도 등 |
| FORBIDDEN | 방장 아님 |

---

## 매칭 상세 조회 확장 (Sprint 3)

**GET** `/api/matches/{matchId}` 응답에 다음 필드가 추가됩니다.

### 공통 필드 (모든 요청)

| 필드 | 타입 | 설명 |
|------|------|------|
| serverTime | String | 서버 시각 (ISO 8601). offerExpiresAt 카운트다운 시 클라이언트-서버 시계 차이 보정용 |
| isEmergencyMode | Boolean | 긴급 선착순 모드 여부. 경기 시작 2시간 미만일 때 true. WAITING 사용자 accept-offer 버튼 노출 판단용 |

### 로그인 사용자 전용 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| myParticipation | MyParticipationSummary | 현재 사용자의 참여 상태 (없으면 null) |
| canApply | Boolean | 참여 신청 가능 여부 |
| canCancel | Boolean | 취소 가능 여부 |
| hasWaitingOffer | Boolean | 예약 수락 대기 여부 (RESERVED 상태) |

**RESERVED 상태 UI 가이드**: `offerExpiresAt`은 서버 기준으로 계산된 값입니다. 클라이언트는 `serverTime`으로 시계 차이를 보정해 카운트다운을 표시하거나, 1분 미만일 때 '곧 만료' UI 표시를 권장합니다.

**MyParticipationSummary**

| 필드 | 타입 | 설명 |
|------|------|------|
| participationId | Long | 참여 내역 ID |
| status | ParticipantStatus | PENDING/ACCEPTED/REJECTED/WAITING/RESERVED |
| queueOrder | Integer | 대기 순번 |
| applyMessage | String | 신청 멘트 |
| offerExpiresAt | LocalDateTime | RESERVED일 때 수락 마감 시각 |

---

## ParticipantStatus 확장 (Sprint 3)

| 값 | 설명 |
|----|------|
| PENDING | 대기 중 (방장 미확인) |
| ACCEPTED | 수락됨 (확정) |
| REJECTED | 거절됨 |
| WAITING | 대기열 |
| **RESERVED** | 참여 기회 부여됨 (15분 내 수락 대기) |
| **CANCELLED** | 본인 취소 (대기 상태 해제) |

---

## 에러 코드 (Sprint 3 추가)

| 코드 | HTTP | 설명 |
|------|------|------|
| MATCH_NOT_FOUND | 404 | 매칭 없음 |
| MATCH_NOT_RECRUITING | 400 | 모집 중 아님 |
| ALREADY_APPLIED | 400 | 이미 신청한 상태 |
| HOST_CANNOT_APPLY | 400 | 방장은 참여 신청 불가 |
| PARTICIPANT_NOT_FOUND | 404 | 참여 내역 없음 |
| INVALID_STATUS | 400 | 이미 수락/거절된 신청 또는 예약 수락 불가 상태 |
| MATCH_FULL | 400 | 정원 초과 (방장 포함 기준, 메시지: "정원이 가득 찼습니다") |
| CANNOT_CANCEL | 400 | 취소 불가 상태 |
| OFFER_EXPIRED | 400 | 예약 기회 만료 |

**OFFER_EXPIRED 처리 가이드**: 프론트엔드에서는 해당 에러 시 자동 새로고침 또는 대기열 상태로 UI 전환을 권장합니다.

---

## 변경 사항 (프론트엔드 연동 참고)

### 1. 방장 추방(KICK) 기능 추가

| 항목 | 내용 |
|------|------|
| **API** | `PATCH /api/matches/{matchId}/participants/{participationId}` |
| **추가 액션** | `action: "KICK"` |
| **대상** | ACCEPTED(확정) 참여자만 |
| **participationId 획득** | `GET /api/matches/{matchId}` → `confirmedParticipants` 각 항목의 `participationId` |
| **UI 제안** | 확정 참여자 목록 각 항목에 [추방] 버튼 노출 (방장만) |

### 2. maxPeople / currentPeople 정책 (Sprint2 참조)

- **maxPeople**: 방장 포함 총 확정 인원 상한
- **currentPeople**: 방장 포함 현재 인원 (ACCEPTED 수 + 1)
- MATCH_FULL 메시지: "정원이 가득 찼습니다"

### 3. myParticipation에 REJECTED 포함

- 거절된 사용자도 `myParticipation.status: "REJECTED"`로 응답
- REJECTED 시 `canApply: true` (재신청 가능)

### 4. 매칭 수정 API (Step 8)

| 항목 | 내용 |
|------|------|
| **API** | `PATCH /api/matches/{matchId}` |
| **권한** | 방장만 |
| **조건** | 모집 중(RECRUITING) 상태에서만 수정 가능 |
| **정원 수정** | maxPeople ≥ currentPeople 필수 (현재 확정 인원보다 낮게 설정 불가) |
| **partial update** | 요청에 포함된 필드만 수정, 미포함 필드는 기존 값 유지 |
