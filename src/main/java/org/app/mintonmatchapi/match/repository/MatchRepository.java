package org.app.mintonmatchapi.match.repository;

import org.app.mintonmatchapi.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long>, MatchRepositoryCustom {

    @Query("SELECT m FROM Match m JOIN FETCH m.host WHERE m.id = :matchId")
    Optional<Match> findByIdWithHost(@Param("matchId") Long matchId);
}
