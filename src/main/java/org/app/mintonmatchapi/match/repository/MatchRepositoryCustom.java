package org.app.mintonmatchapi.match.repository;

import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepositoryCustom {

    Page<Match> searchMatches(MatchSearchCondition condition);

    /**
     * CLOSED이면서 (matchDate + startTime)이 cutoff 이하인 매칭 ID 목록.
     */
    List<Long> findClosedMatchIdsStartedOnOrBefore(LocalDateTime cutoff);

    /**
     * 지정 ID 매칭을 FINISHED로 일괄 갱신한다. QueryDSL update.
     */
    long bulkMarkFinishedByIds(List<Long> matchIds);
}
