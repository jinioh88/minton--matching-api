package org.app.mintonmatchapi.penalty.repository;

import org.app.mintonmatchapi.penalty.entity.Penalty;
import org.app.mintonmatchapi.penalty.entity.PenaltyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    @EntityGraph(attributePaths = {"match", "host"})
    @Query("select p from Penalty p where p.penalizedUser.id = :userId")
    Page<Penalty> findPageWithRelationsByPenalizedUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByMatch_IdAndPenalizedUser_IdAndType(Long matchId, Long penalizedUserId, PenaltyType type);
}
