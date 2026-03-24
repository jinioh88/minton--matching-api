package org.app.mintonmatchapi.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.CostPolicy;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 채팅 상단 공지(B6-3): Match 스냅샷.
 */
@Getter
@Builder
public class MatchChatNoticeResponse {

    private Long matchId;
    private String title;
    private LocalDate matchDate;
    private LocalTime startTime;
    private Integer durationMin;
    private String locationName;
    private CostPolicy costPolicy;
    private MatchStatus status;

    public static MatchChatNoticeResponse from(Match match) {
        return MatchChatNoticeResponse.builder()
                .matchId(match.getId())
                .title(match.getTitle())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .durationMin(match.getDurationMin())
                .locationName(match.getLocationName())
                .costPolicy(match.getCostPolicy())
                .status(match.getStatus())
                .build();
    }
}
