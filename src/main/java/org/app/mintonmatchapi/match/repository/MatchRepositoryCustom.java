package org.app.mintonmatchapi.match.repository;

import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchRepositoryCustom {

    Page<Match> searchMatches(MatchSearchCondition condition);
}
