package org.app.mintonmatchapi.match.repository;

import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepositoryCustom {

    Page<Match> searchMatches(MatchSearchCondition condition);

    /**
     * 내가 방장인 매칭 (모든 상태). 경기일 필터·상태 필터 선택.
     */
    Page<Match> searchHostedByUser(Long hostUserId, MatchStatus status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    /**
     * 내가 ACCEPTED 참가자인 매칭. 경기일·매칭 상태 필터 선택.
     */
    Page<Match> searchParticipatedByUser(Long userId, MatchStatus status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    /**
     * CLOSED이면서 (matchDate + startTime)이 cutoff 이하인 매칭 ID 목록.
     */
    List<Long> findClosedMatchIdsStartedOnOrBefore(LocalDateTime cutoff);

    /**
     * 지정 ID 매칭을 FINISHED로 일괄 갱신한다. QueryDSL update.
     */
    long bulkMarkFinishedByIds(List<Long> matchIds);
}
