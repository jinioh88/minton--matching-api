# Sprint 3 리팩토링 제안

> Sprint 3 작업 내용을 검토하여 개선이 필요한 부분을 정리한 문서입니다.
> 참조: `docs/Sprint3-할일.md`, `docs/Sprint3-클래스정리.md`

---

## 1. 중복 코드 제거

### 1.1 `isEmergencyMode()` 중복

**현상**: `MatchParticipantService`와 `QueuePromotionService`에 동일한 로직이 존재합니다.

| 위치 | 라인 |
|------|------|
| MatchParticipantService | 200~204 |
| QueuePromotionService | 57~61 |

```java
// 동일한 로직
LocalDateTime matchStart = match.getMatchDate().atTime(match.getStartTime());
LocalDateTime cutoff = LocalDateTime.now().plusHours(queueProperties.getEmergencyThresholdHours());
return matchStart.isBefore(cutoff);
```

**제안**:
- `Match` 엔티티에 `isWithinEmergencyThreshold(LocalDateTime cutoff)` 메서드 추가

---

### 1.2 `trimOrNull()` 중복

**현상**: `MatchParticipantService`와 `MatchService`에 동일한 private 메서드가 있습니다.

| 위치 | 라인 |
|------|------|
| MatchParticipantService | 222~228 |
| MatchService | 211~216 |

**제안**:
- `common.util.StringUtils` 커스틈 유틸 클래스 생성
- 프로젝트 전역에서 재사용

---

### 1.3 참여 상태 검증 로직 중복

**현상**: "취소 가능", "신청 가능", "이미 신청함" 등 상태 검증 로직이 여러 곳에 분산되어 있습니다.

| 위치 | 용도 |
|------|------|
| MatchParticipantService.applyParticipant | PENDING, ACCEPTED, WAITING, RESERVED → ALREADY_APPLIED |
| MatchParticipantService.cancelParticipant | PENDING, ACCEPTED, WAITING, RESERVED → 취소 가능 |
| MatchService.resolveCanApply | 동일 조건 → canApply false |
| MatchService.resolveCanCancel | 동일 조건 → canCancel true |

**제안**:
- `ParticipantStatus`에 `isActive()` 또는 `canCancel()` 같은 메서드 추가

```java
// ParticipantStatus에 추가 예시
public boolean isActiveParticipation() {
    return this == PENDING || this == ACCEPTED || this == WAITING || this == RESERVED;
}
```

---

## 2. 에러 코드 일관성

**현상**: 동일한 상황(매칭 없음)에 서로 다른 ErrorCode를 사용합니다.

| 위치 | 사용 코드 | 메시지 |
|------|-----------|--------|
| MatchService.getMatchDetail | `ErrorCode.NOT_FOUND` | "매칭을 찾을 수 없습니다." |
| MatchParticipantService 등 | `ErrorCode.MATCH_NOT_FOUND` | (기본 메시지) |

**제안**:
- 매칭 관련 조회 실패 시 `ErrorCode.MATCH_NOT_FOUND`로 통일
- `MatchService.getMatchDetail`에서 `ErrorCode.NOT_FOUND` → `ErrorCode.MATCH_NOT_FOUND`로 변경

---

## 3. 이벤트 발행 일관성

**현상**: `cancelParticipant`(ACCEPTED 취소)는 `ParticipantCancelledEvent`를 발행하지만, `rejectOffer`(RESERVED 거절)는 `queuePromotionService.promoteOnCancelled()`를 직접 호출합니다.

| API | 처리 방식 |
|-----|-----------|
| DELETE participants/me (ACCEPTED 취소) | 이벤트 발행 → QueuePromotionListener → promoteOnCancelled |
| POST reject-offer (RESERVED 거절) | 직접 promoteOnCancelled 호출 |

**제안**:
- `ParticipantCancelledEvent`에 `wasReserved` 플래그 추가하여 RESERVED 거절 케이스도 이벤트로 처리
- **장점**: 취소/거절로 인한 대기열 승격이 모두 이벤트 기반으로 통일되어, 알림(Sprint 5) 연동 시 일관된 처리 가능

---

## 4. acceptOffer 로직 가독성

**현상**: `MatchParticipantService.acceptOffer`의 조건 분기가 복잡합니다.


**제안**:
- Early return 패턴으로 가독성 개선 + 도메인 메서드 활용
- 가장 추천하는 방식은 제안하신 Early Return 패턴을 사용하되, 앞서 리팩터링했던 ParticipantStatus Enum과 Match 엔티티의 메서드를 적극 활용하는 것입니다.


---

## 5. 스케줄러 다중 인스턴스 대응

**현상**: `QueueTimeoutScheduler`가 1분마다 `processExpiredReservations`를 실행합니다. 서버가 여러 대일 경우 동일 작업이 중복 실행될 수 있습니다.

**제안**:
- **ShedLock** 으로 단일 인스턴스만 실행되도록 제어

---

## 6. processExpiredReservations 최적화

**현상**: `QueuePromotionService.processExpiredReservations`에서 만료된 참여자를 하나씩 처리하며, 각각 `promoteSequential`을 호출합니다. 같은 `matchId`에 여러 만료자가 있으면 `promoteSequential`이 중복 호출됩니다.

**제안**:
- `matchId`별로 그룹핑 후, 각 matchId당 한 번만 `promoteSequential` 호출

---

## 8. Repository 네이밍 일관성

**현상**: `MatchParticipantRepository`에서 `findByMatch_IdAndUser_Id`(언더스코어)와 `findByMatchIdAndStatusInWithUserOrderByQueueOrderAsc`(카멜케이스)가 혼용됩니다.

**제안**:
- Spring Data JPA 규칙상 `match`(엔티티) + `id`(프로퍼티) → `matchId` 또는 `match_id` 모두 허용
- 팀 컨벤션에 맞춰 `findByMatchIdAndUserId` 형태로 통일 권장

---

## 9. Controller 요청 바인딩

**현상**: `applyParticipant`에서 `@RequestBody(required = false)`와 `request != null ? request : new ParticipantApplyRequest()` 처리

```java
@Valid @RequestBody(required = false) ParticipantApplyRequest request
// ...
request != null ? request : new ParticipantApplyRequest()
```

**제안**:
- `ParticipantApplyRequest`에 `@JsonCreator` 또는 기본 생성자로 빈 객체 수신 시 `applyMessage=null` 처리
- `required = false` 유지 시, body가 비어있으면 `null`이므로 null-safe 처리 필요
- 현재 구현은 적절하나, `Optional.ofNullable(request).orElseGet(ParticipantApplyRequest::new)`로 가독성 개선 가능

---

## 10. 응답 DTO 네이밍

**현상**: `decideParticipant`, `acceptOffer`, `rejectOffer`가 모두 `ParticipantApplyResponse`를 반환합니다.

**제안**:
- 기능적으로 "참여 상태 응답"이므로 `ParticipantApplyResponse` 재사용이 타당함
- API 스펙상 participationId, status, queueOrder 등 동일 필드이면 유지
- 별도 `ParticipantDecisionResponse`를 만들면 필드가 중복되므로 현재 구조 유지 권장

---

## 11. MatchService.getMatchDetail - ErrorCode 통일

**현상**: `getMatchDetail`에서 매칭 없을 때 `ErrorCode.NOT_FOUND` 사용

**제안**:
- `ErrorCode.MATCH_NOT_FOUND`로 변경하여 매칭 도메인 에러 코드 통일

---

## 12. QueueProperties setter

**현상**: `@ConfigurationProperties`에서 yml 바인딩을 위해 setter가 필요합니다. 현재 `@Getter`만 있고 setter는 수동 정의되어 있습니다.

**제안**:
- Spring Boot 3.x에서는 `@ConfigurationProperties`에 `@ConstructorBinding`(또는 `@EnableConfigurationProperties`)과 생성자 주입으로 setter 없이 사용 가능
- `@Setter`를 Lombok으로 추가하거나, 생성자 바인딩으로 전환 검토

---