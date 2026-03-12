package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MatchSearchCondition {

    /**
     * 지역 필터 (regionCode). 로그인 시 interestLoc1/2 기본값 적용 가능.
     * null이면 지역 필터 없음 (전체 목록)
     */
    private final List<String> regionCodes;

    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    /**
     * 급수 필터 (targetLevels에 포함되는지)
     */
    private final String level;

    private final Pageable pageable;
}
