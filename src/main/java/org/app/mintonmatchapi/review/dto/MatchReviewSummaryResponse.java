package org.app.mintonmatchapi.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.Match;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class MatchReviewSummaryResponse {

    private Long matchId;
    private String title;
    private LocalDate matchDate;
    private LocalTime startTime;

    public static MatchReviewSummaryResponse from(Match match) {
        return MatchReviewSummaryResponse.builder()
                .matchId(match.getId())
                .title(match.getTitle())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .build();
    }
}
