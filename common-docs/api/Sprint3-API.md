# Sprint 3 API 문서

> 참여 신청, 수락/거절, 대기열, 취소 API  
> 참조: [Sprint1-API.md](./Sprint1-API.md), [Sprint2-API.md](./Sprint2-API.md)

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
| POST | /api/matches/{matchId}/participants | **필요** | 참여/대기 신청 |
| PATCH | /api/matches/{matchId}/participants/{participationId} | **필요** | 방장 수락/거절 |
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

### 2. 방장 수락/거절

**PATCH** `/api/matches/{matchId}/participants/{participationId}`

방장이 참여 신청을 수락 또는 거절합니다.

**인증** 필요 (방장만 호출 가능)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |
| participationId | Long | 참여 내역 ID (match_participants PK) |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| action | String | O | ACCEPT 또는 REJECT |

**Request 예시**

```json
{
  "action": "ACCEPT"
}
```

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
| FORBIDDEN | 방장이 아님 |
| PARTICIPANT_NOT_FOUND | 참여 내역 없음 |
| INVALID_STATUS | 이미 수락/거절된 신청 |
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
      "status": "RESERVED",
      "queueOrder": 1,
      "applyMessage": "대기열에서 기다리고 있어요",
      "appliedAt": "2025-03-15T09:30:00",
      "offerExpiresAt": "2025-03-15T10:45:00"
    }
  ]
}
```

정렬: PENDING → RESERVED → WAITING (queueOrder ASC)

---

## 매칭 상세 조회 확장 (Sprint 3)

**GET** `/api/matches/{matchId}` 응답에 **로그인 사용자**에게 다음 필드가 추가됩니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| myParticipation | MyParticipationSummary | 현재 사용자의 참여 상태 (없으면 null) |
| canApply | Boolean | 참여 신청 가능 여부 |
| canCancel | Boolean | 취소 가능 여부 |
| hasWaitingOffer | Boolean | 예약 수락 대기 여부 (RESERVED 상태) |

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
| MATCH_FULL | 400 | 정원 초과 |
| CANNOT_CANCEL | 400 | 취소 불가 상태 |
| OFFER_EXPIRED | 400 | 예약 기회 만료 |
