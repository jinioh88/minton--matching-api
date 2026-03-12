package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.CostPolicy;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class MatchListResponse {

    private Long matchId;
    private String title;
    private LocalDate matchDate;
    private LocalTime startTime;
    private String locationName;
    private Integer maxPeople;
    private Integer currentPeople;
    private String targetLevels;
    private CostPolicy costPolicy;
    private String imageUrl;
    private String hostNickname;
    private String hostProfileImg;
    private Float hostRatingScore;
    private MatchStatus status;

    public static MatchListResponse of(Match match, int currentPeople) {
        User host = match.getHost();
        return MatchListResponse.builder()
                .matchId(match.getId())
                .title(match.getTitle())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .locationName(match.getLocationName())
                .maxPeople(match.getMaxPeople())
                .currentPeople(currentPeople)
                .targetLevels(match.getTargetLevels())
                .costPolicy(match.getCostPolicy())
                .imageUrl(match.getImageUrl())
                .hostNickname(host != null ? host.getNickname() : null)
                .hostProfileImg(host != null ? host.getProfileImg() : null)
                .hostRatingScore(host != null ? host.getRatingScore() : null)
                .status(match.getStatus())
                .build();
    }
}
