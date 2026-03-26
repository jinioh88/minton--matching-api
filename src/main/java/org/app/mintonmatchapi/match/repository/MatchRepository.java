package org.app.mintonmatchapi.match.repository;

import jakarta.persistence.LockModeType;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long>, MatchRepositoryCustom {

    /**
     * 방장이 연 매칭 수 (모든 상태 포함, CANCELLED 포함). Sprint7 Step0 P1.
     */
    long countByHost_Id(Long hostId);

    /**
     * "참여" 매칭 수: 방장 매칭 ∪ ACCEPTED 참가, match_id 기준 중복 없음. Sprint7 Step0 P2.
     */
    @Query("SELECT COUNT(DISTINCT m.id) FROM Match m WHERE m.host.id = :userId OR EXISTS (" +
            "SELECT 1 FROM MatchParticipant mp WHERE mp.match.id = m.id AND mp.user.id = :userId AND mp.status = :accepted)")
    long countDistinctParticipatedForUser(@Param("userId") Long userId, @Param("accepted") ParticipantStatus accepted);

    @Query("SELECT m FROM Match m JOIN FETCH m.host WHERE m.id = :matchId")
    Optional<Match> findByIdWithHost(@Param("matchId") Long matchId);

    @Query("SELECT DISTINCT m FROM Match m JOIN FETCH m.host WHERE m.id IN :ids")
    List<Match> findAllWithHostByIdIn(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Match m JOIN FETCH m.host WHERE m.id = :matchId")
    Optional<Match> findByIdWithHostForUpdate(@Param("matchId") Long matchId);
}
