# Sprint 2 API 문서

> AWS S3 파일 업로드, 프로필 이미지, 매칭 API  
> 참조: [Sprint1-API.md](./Sprint1-API.md) (기본 정보, 인증, Enum 등)

---

## API 목록 (Sprint 2)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | /api/files/upload | **필요** | 이미지 파일 업로드 (Multipart) |
| POST | /api/users/me/profile-image | **필요** | 프로필 이미지 업로드 (Multipart) |
| POST | /api/matches | **필요** | 매칭 생성 |
| GET | /api/matches | 불필요 | 매칭 목록 조회 (필터, 페이징) |
| GET | /api/matches/{matchId} | 불필요 | 매칭 상세 조회 |

---

## 상세 API

### 1. 이미지 파일 업로드

**POST** `/api/files/upload`

이미지 파일을 S3에 업로드하고 공개 URL을 반환합니다. 매칭 대표 이미지 등에 사용합니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

**Content-Type** `multipart/form-data`

**Request (form-data)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | MultipartFile | O | 이미지 파일 (jpeg, png, gif, webp, 최대 5MB) |
| type | String | X | PROFILE, MATCH (업로드 용도 구분) |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "url": "https://minton-match-uploads.s3.ap-northeast-2.amazonaws.com/match/abc123.jpg",
    "key": "match/abc123.jpg"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| url | String | S3 공개 접근 URL |
| key | String | S3 객체 키 (삭제 시 사용) |

---

### 2. 프로필 이미지 업로드

**POST** `/api/users/me/profile-image`

프로필 이미지를 S3에 업로드하고 `profileImg` URL을 자동 반영합니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

**Content-Type** `multipart/form-data`

**Request (form-data)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| image | MultipartFile | O | 이미지 파일 (jpeg, png, gif, webp, 최대 5MB) |

**동작**

1. 기존 `profileImg`가 본 서비스 S3 버킷 URL인 경우 → 해당 객체 삭제
2. 새 이미지 S3 업로드 → URL 획득
3. `User.profileImg` 업데이트 후 반환

**Response 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "배드민턴왕",
    "profileImg": "https://minton-match-uploads.s3.ap-northeast-2.amazonaws.com/profile/1/abc123.jpg",
    "level": "B",
    "interestLoc1": "1159010500",
    "interestLoc2": "1168010100",
    "racketInfo": "요넥스 아크세이버",
    "playStyle": "공격형",
    "ratingScore": 5.0,
    "penaltyCount": 0
  }
}
```

**프로필 이미지 업로드 흐름**

1. **Multipart 업로드**: `POST /api/users/me/profile-image` → `image` 파라미터로 파일 전송
2. **PATCH로 URL만 전달**: `POST /api/files/upload`로 업로드 후 URL 획득 → `PATCH /api/users/me`에 `profileImg` 필드로 URL 전달

---

### 3. 매칭 생성

**POST** `/api/matches`

매칭을 생성합니다.

**인증** 필요 (`Authorization: Bearer {accessToken}`)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 제목 (최대 100자) |
| description | String | O | 상세 설명 |
| matchDate | LocalDate | O | 경기 날짜 (yyyy-MM-dd) |
| startTime | LocalTime | O | 시작 시간 (HH:mm) |
| durationMin | Integer | O | 소요 시간(분, 30~240) |
| locationName | String | X | 장소명 (최대 200자) |
| locationAddress | String | X | 장소 주소 |
| regionCode | String | O | 행정구역 코드 (7~10자리 숫자) |
| maxPeople | Integer | O | 모집 인원 (2~12) |
| targetLevels | String | X | 희망 급수 (예: "A,B,C") |
| costPolicy | CostPolicy | O | 비용 분담 방식 (enum) |
| imageUrl | String | X | 매칭 대표 이미지 URL (이미지 파일 업로드 API `POST /api/files/upload` 응답의 `url` 값 사용) |
| latitude | Double | X | 위도 (추후 지도 검색용) |
| longitude | Double | X | 경도 (추후 지도 검색용) |

**CostPolicy enum**: SPLIT_EQUAL, HOST_PAYS, GUEST_PAYS

**Request 예시**

```json
{
  "title": "주말 모임",
  "description": "초보 환영합니다.",
  "matchDate": "2025-03-15",
  "startTime": "14:00",
  "durationMin": 120,
  "locationName": "OO체육관",
  "locationAddress": "서울시 강남구 ...",
  "regionCode": "1168010100",
  "maxPeople": 4,
  "targetLevels": "B,C",
  "costPolicy": "SPLIT_EQUAL",
  "imageUrl": "https://..."
}
```

**Response 예시**

```json
{
  "success": true,
  "data": {
    "matchId": 1,
    "hostId": 1,
    "title": "주말 모임",
    "description": "초보 환영합니다.",
    "matchDate": "2025-03-15",
    "startTime": "14:00",
    "durationMin": 120,
    "locationName": "OO체육관",
    "locationAddress": "서울시 강남구 ...",
    "regionCode": "1168010100",
    "maxPeople": 4,
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

---

### 4. 매칭 목록 조회

**GET** `/api/matches`

매칭 목록을 필터·페이징으로 조회합니다. **비로그인/로그인 모두 접근 가능.**

**인증** 불필요 (선택 시 로그인 사용자 interestLoc1/2를 기본 regionCode로 적용)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| regionCode | String | X | 지역 필터 (행정구역 코드). 단일 또는 다중(쉼표 구분) |
| dateFrom | LocalDate | X | 시작 날짜 (yyyy-MM-dd) |
| dateTo | LocalDate | X | 종료 날짜 (yyyy-MM-dd) |
| level | String | X | 급수 필터 (targetLevels에 포함) |
| page | int | X | 페이지 (기본 0) |
| size | int | X | 페이지 크기 (기본 20) |

**동작**

- **비로그인**: regionCode 쿼리로 전달 또는 미전달 시 전체 목록
- **로그인**: regionCode 미전달 시 interestLoc1/2를 기본 regionCode로 적용, query param으로 override 가능

**Response 예시**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "matchId": 1,
        "title": "주말 모임",
        "matchDate": "2025-03-15",
        "startTime": "14:00",
        "locationName": "OO체육관",
        "maxPeople": 4,
        "currentPeople": 2,
        "targetLevels": "B,C",
        "costPolicy": "SPLIT_EQUAL",
        "imageUrl": "https://...",
        "hostNickname": "배드민턴왕",
        "hostProfileImg": "https://...",
        "hostRatingScore": 5.0,
        "status": "RECRUITING"
      }
    ],
    "pageable": { ... },
    "totalElements": 10,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

---

### 5. 매칭 상세 조회

**GET** `/api/matches/{matchId}`

매칭 상세 정보를 조회합니다. **비로그인/로그인 모두 접근 가능.**

**인증** 불필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매칭 ID |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "matchId": 1,
    "hostId": 1,
    "title": "주말 모임",
    "description": "초보 환영합니다.",
    "matchDate": "2025-03-15",
    "startTime": "14:00",
    "durationMin": 120,
    "locationName": "OO체육관",
    "locationAddress": "서울시 강남구 ...",
    "regionCode": "1168010100",
    "maxPeople": 4,
    "currentPeople": 2,
    "targetLevels": "B,C",
    "costPolicy": "SPLIT_EQUAL",
    "status": "RECRUITING",
    "imageUrl": "https://...",
    "latitude": null,
    "longitude": null,
    "createdAt": "2025-03-11T12:00:00",
    "host": {
      "id": 1,
      "nickname": "배드민턴왕",
      "profileImg": "https://...",
      "ratingScore": 5.0
    },
    "confirmedParticipants": [
      {
        "participationId": 1,
        "userId": 2,
        "nickname": "참가자1",
        "profileImg": "https://...",
        "ratingScore": 4.8,
        "queueOrder": 0,
        "applyMessage": "함께하고 싶습니다"
      }
    ],
    "waitingList": [],
    "waitingCount": 0
  }
}
```

---

## Enum 참조 (Sprint 2)

### CostPolicy (비용 분담 방식)

| 값 | 설명 |
|----|------|
| SPLIT_EQUAL | 균등 분담 |
| HOST_PAYS | 방장 부담 |
| GUEST_PAYS | 참가자 부담 |

### MatchStatus (매칭 상태)

| 값 | 설명 |
|----|------|
| RECRUITING | 모집 중 |
| CLOSED | 모집 마감 |
| FINISHED | 종료 |
| CANCELLED | 취소 |

### ParticipantStatus (참여 신청 상태)

| 값 | 설명 |
|----|------|
| PENDING | 대기 중 (방장 미확인) |
| ACCEPTED | 수락됨 (확정) |
| REJECTED | 거절됨 |
| WAITING | 대기열 |

### FileUploadType (파일 업로드 용도)

| 값 | 설명 |
|----|------|
| PROFILE | 프로필 이미지 |
| MATCH | 매칭 대표 이미지 |

---

## 에러 코드

Sprint 1과 동일. [Sprint1-API.md](./Sprint1-API.md) 참조.
