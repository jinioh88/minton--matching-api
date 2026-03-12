package org.app.mintonmatchapi.match.repository;

import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    @Query("SELECT mp FROM MatchParticipant mp JOIN FETCH mp.user WHERE mp.match.id = :matchId AND mp.status IN :statuses ORDER BY mp.status ASC, mp.queueOrder ASC")
    List<MatchParticipant> findByMatchIdAndStatusInWithUserOrderByQueueOrderAsc(@Param("matchId") Long matchId, @Param("statuses") List<ParticipantStatus> statuses);

    long countByMatchIdAndStatus(Long matchId, ParticipantStatus status);

    @Query("SELECT mp.match.id, COUNT(mp) FROM MatchParticipant mp WHERE mp.match.id IN :matchIds AND mp.status = :status GROUP BY mp.match.id")
    List<Object[]> countByMatchIdsAndStatus(@Param("matchIds") List<Long> matchIds, @Param("status") ParticipantStatus status);
}
