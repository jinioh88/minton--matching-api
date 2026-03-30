# Sprint 2 할일 문서

> 참조: `docs/스프린트플래닝.md`, `docs/제품백로그.md`, `docs/ERD.md`, `docs/요구사항분석.md`
> **상태: 완료** (2025-03-11)

## Sprint 2 목표

매칭 생성·목록·상세가 동작하고, **AWS S3를 이용한 이미지 업로드**(프로필, 매칭)가 가능한 API를 만든다.

### 완료 조건
- [x] 프로필 이미지 Multipart 업로드 → S3 저장 → URL 반영
- [x] 매칭 생성 시 이미지 업로드 가능
- [x] 매칭 생성 후 목록에 노출
- [x] 필터로 목록 조회 가능
- [x] 매칭 상세에서 모든 정보 확인 가능

---

## Step 1: AWS S3 설정 및 파일 업로드 API (B8-1)

### Step 1.1: AWS 인프라 준비
- [x] AWS S3 버킷 생성 (예: minton-match-uploads)
- [x] 버킷 CORS 설정 (클라이언트 업로드 허용 시)
- [x] IAM 사용자/역할 생성 및 S3 접근 권한 부여 (PutObject, GetObject, **DeleteObject**)
- [x] Access Key, Secret Key 발급 (application.yml에 설정)

### Step 1.2: 의존성 추가
- [x] spring-cloud-aws-starter-s3 또는 aws-java-sdk-s3 의존성 추가
- [x] application.yml에 AWS 설정 추가
  - region, bucket-name, credentials (access-key, secret-key)
  - 또는 IAM Role 사용 시 credentials 생략

### Step 1.3: 파일 업로드 공통 API
- [x] POST /api/files/upload (Multipart/form-data)
  - Request: `file` (MultipartFile), `type` (PROFILE, MATCH 등 - 선택)
  - 업로드 허용: 이미지만 (jpeg, png, gif, webp), 최대 크기 제한 (예: 5MB)
  - S3에 저장 후 **공개 URL** 반환
- [x] FileUploadService 또는 S3Service 구현
- [x] 업로드 파일명: UUID 또는 `{type}/{userId}/{uuid}.{ext}` 형식으로 중복 방지

---

## Step 2: 프로필 이미지 업로드 API (B8-2)

### Step 2.1: 프로필 이미지 업로드 엔드포인트
- [x] POST /api/users/me/profile-image (Multipart/form-data)
  - Request: `image` (MultipartFile)
  - 인증 필요
  - **기존 S3 파일 삭제**: 기존 profileImg URL이 본 서비스 S3 버킷 경로인 경우, 업로드 전 해당 객체 삭제
  - S3 업로드 → URL 획득 → User.profileImg 업데이트
- [x] S3Service에 deleteObject(key 또는 URL) 메서드 추가 (프로필 이미지 교체 시 호출)
- [x] UserService에 uploadProfileImage() 메서드 추가

### Step 2.2: PATCH /api/users/me와 연동
- [x] profileImg 필드: URL 문자열 전달 시 그대로 저장 (기존 동작 유지)
- [x] 프로필 이미지 업로드 API 사용 시 → PATCH로 profileImg URL만 전달해도 동작
- [x] API 문서에 프로필 이미지 업로드 흐름 명시

---

## Step 3: Match 엔티티 및 Location (B3-1, B3-2)

### Step 3.1: Location 엔티티 (선택)
- [x] ERD의 Locations 테이블 참조
- [x] Location 엔티티: id, name, address, regionCode 등 (MVP에서는 단순화 가능)
- [x] **간소화 옵션**: Match에 locationName (VARCHAR), locationAddress (TEXT) 직접 저장
  - 장소 검색/지도 연동은 후순위, Sprint 2에서는 문자열 입력으로 처리

### Step 3.2: Match 엔티티
- [x] Match 엔티티 생성 (`docs/ERD.md` Matches 테이블 참조)
  - matchId, hostId (FK → User)
  - title, description (TEXT)
  - matchDate (DATE), startTime (TIME), durationMin (INT)
  - locationId (FK → Location) 또는 locationName, locationAddress
  - **regionCode** (VARCHAR, 필수) - 검색용 행정구역 코드 (User.interestLoc1/2와 매칭)
  - maxPeople (모집 총원)
  - targetLevels (VARCHAR, 예: "A,B,C" 또는 JSON)
  - costPolicy (ENUM: SPLIT_EQUAL, HOST_PAYS, GUEST_PAYS 등)
  - status (ENUM: RECRUITING, CLOSED, FINISHED, CANCELLED)
  - **imageUrl** (TEXT) - 매칭 대표 이미지 URL
  - **latitude, longitude** (Double, 선택) - 추후 지도 검색 대비
  - createdAt, updatedAt
- [x] MatchRepository (JpaRepository 상속)

### Step 3.3: MatchParticipant 엔티티 (Sprint 2 범위 내 선행)
- [x] 매칭 상세에서 "확정/대기 현황" 표시를 위해 MatchParticipant 필요
- [x] Sprint 2에서는 **조회만** (참여 신청 API는 Sprint 3)
- [x] MatchParticipant: participationId, matchId, userId, status, queueOrder, applyMessage, attendance
- [x] status: PENDING, ACCEPTED, REJECTED, WAITING

---

## Step 4: 매칭 생성 API (B3-1, B3-2, B8-3)

### Step 4.1: 매칭 생성 요청/응답 DTO
- [x] MatchCreateRequest
  - title (필수, 최대 100자)
  - description (필수)
  - matchDate (필수, LocalDate)
  - startTime (필수, LocalTime)
  - durationMin (필수, 30~240 등)
  - locationName 또는 locationId
  - **regionCode** (필수, 행정구역 코드 7~10자리) - 검색 필터용
  - maxPeople (필수, 2~12)
  - targetLevels (선택, "A,B,C" 형식)
  - costPolicy (필수, enum)
  - imageUrl (선택, 파일 업로드 API로 먼저 업로드 후 URL 전달)
  - latitude, longitude (선택, 추후 지도 검색용)

### Step 4.2: 매칭 생성 API
- [x] POST /api/matches (인증 필요)
- [x] MatchService.createMatch(currentUserId, request)
  - hostId = 현재 로그인 사용자
  - status = RECRUITING
  - 유효성 검증 (날짜/시간, 인원 수 등)
- [x] MatchResponse DTO (생성된 매칭 정보 반환)

### Step 4.3: 인증 및 권한
- [x] POST /api/matches 인증 필터에 포함
- [x] SecurityConfig에 /api/matches/** 경로 인증 필요 설정

---

## Step 5: 매칭 목록 API (B3-3, B3-4)

### Step 5.1: 목록 조회 조건
- [x] GET /api/matches
  - Query: regionCode (Match.regionCode 기준 필터), dateFrom, dateTo, level, page, size
  - **regionCode 필수**: Match 엔티티의 regionCode로 지역 필터 (행정구역 코드)
  - **비로그인**: regionCode 쿼리로 직접 전달 또는 전체 목록
  - **로그인**: interestLoc1/2를 기본 regionCode로 적용, query param으로 override 가능

### Step 5.2: 필터 및 정렬
- [x] 날짜 필터: matchDate BETWEEN dateFrom AND dateTo
- [x] 급수 필터: targetLevels에 해당 level 포함
- [x] 정렬: matchDate ASC, startTime ASC (가까운 모임 우선)
- [x] 페이징: Pageable (page, size, default size=20)

### Step 5.3: 목록 응답 DTO
- [x] MatchListResponse (카드용 요약 정보)
  - matchId, title, matchDate, startTime, locationName
  - maxPeople, currentPeople (확정 인원 수)
  - targetLevels, costPolicy
  - imageUrl, hostNickname, hostProfileImg, hostRatingScore
  - status

### Step 5.4: QueryDSL 적용
- [x] 동적 필터(지역, 날짜, 급수)가 있으므로 QueryDSL 사용
- [x] MatchRepositoryCustom, MatchRepositoryImpl
- [x] **N+1 방지**: 목록 조회 시 host(User) 정보 필요 → `fetchJoin(match.host)` 적절히 활용
  - `leftJoin(match.host).fetchJoin()` 등으로 한 번에 로드

---

## Step 6: 매칭 상세 API (B3-5, B3-6)

### Step 6.1: 상세 조회
- [x] GET /api/matches/{matchId}
- [x] MatchService.getMatchDetail(matchId)
  - Match 존재 여부 확인
  - 방장 정보 포함 (User 프로필 요약)
  - 확정 인원 목록 (MatchParticipant where status=ACCEPTED)
  - 대기열 현황 (MatchParticipant where status=WAITING, queueOrder)

### Step 6.2: 상세 응답 DTO
- [x] MatchDetailResponse
  - 매칭 전체 정보 (제목, 설명, 일시, 장소, 인원, 급수, 코트비, 이미지)
  - host: HostSummary (id, nickname, profileImg, ratingScore)
  - confirmedParticipants: List<ParticipantSummary>
  - waitingCount 또는 waitingList (대기열 인원 수/목록)
  - status

### Step 6.3: 비로그인 접근
- [x] 매칭 상세는 인증 없이 조회 가능 (공개 정보)
- [x] SecurityConfig: GET /api/matches/{id} 인증 선택 또는 불필요

---

## Step 7: API 문서 업데이트

- [x] common-docs/api/Sprint2-API.md 또는 기존 문서에 Sprint 2 API 추가
  - POST /api/files/upload (Multipart)
  - POST /api/users/me/profile-image (Multipart)
  - POST /api/matches
  - GET /api/matches (필터, 페이징)
  - GET /api/matches/{matchId}
- [x] Request/Response 예시, Enum 정의 (costPolicy, matchStatus 등)

---

## Sprint 2 작업 요약

| Step | 내용 | 백로그 ID |
|------|------|-----------|
| 1 | AWS S3 설정, 파일 업로드 API | B8-1 |
| 2 | 프로필 이미지 업로드 API | B8-2 |
| 3 | Match 엔티티, Location(선택), MatchParticipant | B3-1, B3-2 |
| 4 | 매칭 생성 API | B3-1, B3-2, B8-3 |
| 5 | 매칭 목록 API (필터, 페이징) | B3-3, B3-4 |
| 6 | 매칭 상세 API (방장, 확정/대기 현황) | B3-5, B3-6 |
| 7 | API 문서 | B1-3 |

---

## 의존성 및 진행 순서

```
Step 1 (S3, 파일 업로드) → Step 2 (프로필 이미지)
                    ↘
Step 3 (Match 엔티티) → Step 4 (매칭 생성) → Step 5 (목록) → Step 6 (상세)
```

- Step 1, 3은 병렬 가능 (서로 독립)
- Step 2는 Step 1 완료 후
- Step 4는 Step 3 완료 후
- Step 5, 6은 Step 4(또는 Step 3) 완료 후


---
신규 추가
Config
config/S3Properties.java – S3 버킷/리전 설정
config/QuerydslConfig.java – JPAQueryFactory 빈
config/WebConfig.java – IfLoginArgumentResolver 등록

Auth
auth/annotation/IfLogin.java – 선택적 인증 어노테이션
auth/resolver/IfLoginArgumentResolver.java – @IfLogin 처리

File
file/service/S3Service.java – S3 업로드/삭제
file/service/FileUploadService.java – 파일 업로드 서비스
file/controller/FileUploadController.java – POST /api/files/upload

Match (Entity)
match/entity/Match.java
match/entity/MatchParticipant.java
match/entity/CostPolicy.java (enum)
match/entity/MatchStatus.java (enum)
match/entity/ParticipantStatus.java (enum)
match/entity/Attendance.java (enum)

Match (Repository)
match/repository/MatchRepository.java
match/repository/MatchParticipantRepository.java
match/repository/MatchRepositoryCustom.java
match/repository/MatchRepositoryImpl.java

Match (Service / Controller)
match/service/MatchService.java
match/controller/MatchController.java


수정된 클래스
config/SecurityConfig.java – @EnableMethodSecurity, /api/users/me/**, POST /api/matches 인증 설정
auth/UserPrincipal.java – Role 필드 추가
user/service/UserService.java – S3Service 주입, uploadProfileImage() 추가
user/controller/UserController.java – POST /api/users/me/profile-image 추가