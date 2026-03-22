package org.app.mintonmatchapi.match.repository;

import jakarta.persistence.LockModeType;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    @Query("SELECT mp FROM MatchParticipant mp JOIN FETCH mp.user WHERE mp.match.id = :matchId AND mp.status IN :statuses ORDER BY mp.status ASC, mp.queueOrder ASC")
    List<MatchParticipant> findByMatchIdAndStatusInWithUserOrderByQueueOrderAsc(@Param("matchId") Long matchId, @Param("statuses") List<ParticipantStatus> statuses);

    long countByMatchIdAndStatus(Long matchId, ParticipantStatus status);

    @Query("SELECT mp.match.id, COUNT(mp) FROM MatchParticipant mp WHERE mp.match.id IN :matchIds AND mp.status = :status GROUP BY mp.match.id")
    List<Object[]> countByMatchIdsAndStatus(@Param("matchIds") List<Long> matchIds, @Param("status") ParticipantStatus status);

    /**
     * 매칭+사용자별 모든 참여 이력 조회 (재신청 시 CANCELLED/REJECTED 재사용 판단용)
     */
    @Query("SELECT mp FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.user.id = :userId")
    List<MatchParticipant> findByMatchIdAndUserIdAll(@Param("matchId") Long matchId, @Param("userId") Long userId);

    /**
     * 활성 참여(PENDING/ACCEPTED/WAITING/RESERVED) 1건 조회. 여러 건이면 최신 1건 반환
     */
    @Query("SELECT mp FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.user.id = :userId AND mp.status IN :statuses ORDER BY mp.id DESC")
    List<MatchParticipant> findByMatchIdAndUserIdAndStatusInOrderByIdDesc(
            @Param("matchId") Long matchId, @Param("userId") Long userId, @Param("statuses") List<ParticipantStatus> statuses);

    /**
     * 매칭 상세 조회 시 사용자 참여 상태 표시용. REJECTED/CANCELLED 포함 최신 1건 반환
     */
    Optional<MatchParticipant> findFirstByMatch_IdAndUser_IdOrderByIdDesc(Long matchId, Long userId);

    /**
     * 활성 참여 1건 조회 (cancel, accept-offer, reject-offer용)
     */
    default Optional<MatchParticipant> findActiveByMatchIdAndUserId(Long matchId, Long userId) {
        List<MatchParticipant> list = findByMatchIdAndUserIdAndStatusInOrderByIdDesc(matchId, userId,
                List.of(ParticipantStatus.PENDING, ParticipantStatus.ACCEPTED, ParticipantStatus.WAITING, ParticipantStatus.RESERVED));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 대기열 1번 조회용 (특정 상태의 queueOrder 최소 1건)
     */
    List<MatchParticipant> findByMatch_IdAndStatusOrderByQueueOrderAsc(Long matchId, ParticipantStatus status);

    /**
     * 대기열 순번 계산 (WAITING 상태 최대 queueOrder 조회)
     * @return 최대 queueOrder, 없으면 0
     */
    default int findMaxQueueOrderByMatchId(Long matchId) {
        Integer max = findMaxQueueOrderByMatchIdAndStatus(matchId, ParticipantStatus.WAITING);
        return max != null ? max : 0;
    }

    @Query("SELECT MAX(mp.queueOrder) FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.status = :status")
    Integer findMaxQueueOrderByMatchIdAndStatus(@Param("matchId") Long matchId, @Param("status") ParticipantStatus status);

    /**
     * 방장의 PENDING/WAITING/RESERVED 신청 목록 조회
     */
    List<MatchParticipant> findByMatch_IdAndStatusIn(Long matchId, List<ParticipantStatus> statuses);

    /**
     * 방장의 신청 목록 조회 (PENDING → RESERVED → WAITING 순, User fetch)
     * 정렬: PENDING 먼저, RESERVED, 그 다음 WAITING (queueOrder ASC)
     */
    @Query("SELECT mp FROM MatchParticipant mp JOIN FETCH mp.user WHERE mp.match.id = :matchId AND mp.status IN :statuses " +
            "ORDER BY CASE mp.status WHEN 'PENDING' THEN 0 WHEN 'RESERVED' THEN 1 WHEN 'WAITING' THEN 2 END, mp.queueOrder ASC")
    List<MatchParticipant> findByMatch_IdAndStatusInOrderByStatusAndQueueOrder(
            @Param("matchId") Long matchId, @Param("statuses") List<ParticipantStatus> statuses);

    /**
     * RESERVED(예약 중) 조회용
     */
    List<MatchParticipant> findByMatch_IdAndStatus(Long matchId, ParticipantStatus status);

    /**
     * participationId와 matchId로 참여 조회 (방장 수락/거절 시 검증용)
     */
    @Query("SELECT mp FROM MatchParticipant mp JOIN FETCH mp.match m JOIN FETCH m.host WHERE mp.id = :participationId AND mp.match.id = :matchId")
    Optional<MatchParticipant> findByIdAndMatch_IdWithMatch(@Param("participationId") Long participationId, @Param("matchId") Long matchId);

    /**
     * 대기열 1번 조회 (비관적 락 - 동시 승격 방지)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mp FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.status = :status ORDER BY mp.queueOrder ASC")
    List<MatchParticipant> findByMatch_IdAndStatusOrderByQueueOrderAscWithLock(
            @Param("matchId") Long matchId, @Param("status") ParticipantStatus status, Pageable pageable);

    /**
     * 타임아웃된 RESERVED 조회 (offerExpiresAt < now)
     */
    @Query("SELECT mp FROM MatchParticipant mp JOIN FETCH mp.match WHERE mp.status = :status AND mp.offerExpiresAt < :now")
    List<MatchParticipant> findByStatusAndOfferExpiresAtBefore(
            @Param("status") ParticipantStatus status, @Param("now") LocalDateTime now);
}
