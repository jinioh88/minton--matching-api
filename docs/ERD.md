📊 배드민턴 매칭 서비스 DB 설계 명세서 (ERD)

1. ERD 논리 구조 (Relationship)

Users (1) <--- (N) Matches: 한 사용자는 여러 매칭을 생성할 수 있음.

Matches (1) <--- (N) MatchParticipants: 하나의 매칭에는 여러 참여자가 존재함.

Users (1) <--- (N) MatchParticipants: 한 사용자는 여러 매칭에 참여 신청할 수 있음.

Matches (1) <--- (1) ChatRooms: 매칭 하나당 전용 채팅방이 하나 생성됨.

ChatRooms (1) <--- (N) Messages: 채팅방 하나에 여러 메시지가 쌓임.

Matches (1) <--- (N) Reviews: 매칭 종료 후 여러 유저가 리뷰를 남김.

2. 테이블 상세 설계 (Table Specs)

2.1 Users (사용자)

사용자의 프로필 및 배드민턴 관련 정보를 저장합니다. OAuth2 소셜 로그인(카카오/네이버/구글/애플) 전용입니다.

컬럼명

타입

제약 조건

설명

user_id

BIGINT

PK, Auto Increment

고유 식별자

provider

VARCHAR(20)

Not Null

OAuth2 제공자 (KAKAO, NAVER, GOOGLE, APPLE)

provider_id

VARCHAR(100)

Not Null

소셜 제공자 내 고유 ID

email

VARCHAR(100)

-

소셜에서 제공한 이메일 (제공자에 따라 nullable)

nickname

VARCHAR(30)

-

서비스 활동명 (최초 프로필 입력 시 필수)

profile_img

TEXT

-

프로필 이미지 URL (소셜에서 제공 가능)

level

ENUM

A, B, C, D, BEGINNER

자칭 급수

interest_loc_1

VARCHAR(50)

-

관심 지역 1 (최초 프로필 입력 시 필수)

interest_loc_2

VARCHAR(50)

-

관심 지역 2

racket_info

VARCHAR(100)

-

사용하는 라켓 정보

play_style

VARCHAR(50)

-

선호 플레이 스타일 (드라이브, 공격형 등)

rating_score

FLOAT

Default 0

매너/실력 표시 점수(무후기 0, 후기 반영 시 갱신 — `docs/요구사항분석.md`)

penalty_count

INT

Default 0

노쇼/지각 등 부여 **건수**(스트라이크 집계에 사용, 유형별 가중치 도입 시 `penalty_points`와 병행 가능)

penalty_points

INT

Default 0

(선택) 유형별 가중치 합산용 — 단계별 제재 판정에 사용할 경우

participation_banned_until

DATETIME

NULL 허용

이 시각 이전까지 **매칭 참여 신청(Apply) 불가**(단계별 제재·제한 단계). `docs/요구사항분석.md` 제재 정책

suspended_until

DATETIME

NULL 허용

이 시각 이전까지 **넓은 기능 제한**(정지 단계). 로그인만 허용 등 정책과 연동

account_status

VARCHAR 또는 ENUM

Default ACTIVE

ACTIVE, SUSPENDED, BANNED 등 — 퇴출·영구 정지 시

created_at

TIMESTAMP

Default NOW()

가입 일시

updated_at

TIMESTAMP

Default NOW() ON UPDATE

수정 일시

**Unique 제약**: (provider, provider_id) - 동일 소셜 계정 중복 가입 방지

2.2 Matches (매칭)

개설된 매칭 모임의 상세 정보를 저장합니다.

컬럼명

타입

제약 조건

설명

match_id

BIGINT

PK, Auto Increment

매칭 고유 번호

host_id

BIGINT

FK (Users.user_id)

모임 개설자

title

VARCHAR(100)

Not Null

매칭 제목

description

TEXT

Not Null

매칭 상세 설명

match_date

DATE

Not Null

경기 날짜

start_time

TIME

Not Null

시작 시간

duration_min

INT

Not Null

소요 시간(분)

location_id

BIGINT

FK (Locations.location_id)

경기 장소

max_people

INT

Not Null

모집 총원

target_levels

VARCHAR(50)

-

희망 급수 (예: "C,D")

cost_policy

VARCHAR(50)

-

비용 분담 방식

status

ENUM

RECRUITING, CLOSED, FINISHED, CANCELLED

매칭 상태

2.3 MatchParticipants (참여 및 대기열)

매칭에 신청한 유저들의 상태와 대기 순번을 관리합니다. **하이브리드 타임아웃 시스템** 적용.

컬럼명

타입

제약 조건

설명

participation_id

BIGINT

PK, Auto Increment

참여 내역 ID

match_id

BIGINT

FK (Matches.match_id)

해당 매칭

user_id

BIGINT

FK (Users.user_id)

신청 유저

status

ENUM

PENDING, ACCEPTED, REJECTED, WAITING, RESERVED, CANCELLED

신청 상태. RESERVED=참여 기회 부여(15분 내 수락 대기), CANCELLED=본인 취소

queue_order

INT

Default 0

대기 순번

apply_message

VARCHAR(200)

-

신청 한마디

offer_expires_at

TIMESTAMP

-

예약 만료 시각 (RESERVED일 때, 15분 타임아웃)

attendance

ENUM

UNDECIDED, ATTENDED, LATE, NOSHOW

출석/지각/노쇼 상태

2.4 Chat (채팅방 및 메시지)

확정 인원 간의 소통 데이터를 저장합니다.

테이블

컬럼명

타입

설명

ChatRooms

room_id

BIGINT (PK)

매칭 ID와 1:1 매핑

Messages

msg_id

BIGINT (PK)

메시지 고유 번호



room_id

FK (ChatRooms)

소속된 방



sender_id

FK (Users)

발신자



content

TEXT

메시지 내용



type

ENUM

TEXT, IMAGE, SYSTEM

2.5 Reviews (후기 및 평가)

모임 종료 후 유저 간 피드백을 저장합니다.

컬럼명

타입

제약 조건

설명

review_id

BIGINT

PK, Auto Increment

리뷰 ID

match_id

BIGINT

FK (Matches.match_id)

매칭 정보

reviewer_id

BIGINT

FK (Users.user_id)

작성자

reviewee_id

BIGINT

FK (Users.user_id)

대상자

score

INT

1 ~ 5

평가 점수

tags

JSON

-

선택한 해시태그 (예: ["매너왕", "고수"])

3. 주요 쿼리 로직 가이드 (Backend Logic)

매칭 목록 조회 (Home)

WHERE interest_loc_1 = ? OR interest_loc_2 = ?

ORDER BY match_date ASC, start_time ASC

참여 수락 처리

UPDATE MatchParticipants SET status = 'ACCEPTED' WHERE participation_id = ?

이후 Matches.current_people 카운트 +1 처리.

노쇼·지각 패널티 반영

Penalties 행 INSERT 후 Users.penalty_count(및 선택 penalty_points) 갱신.

단계별 제재에 따라 participation_banned_until, suspended_until, account_status 갱신(`docs/요구사항분석.md`).

참여 신청 API에서는 participation_banned_until > now 이면 거절.

rating_score는 후기 도메인에서 별도 갱신(패널티와 직접 연동은 선택).