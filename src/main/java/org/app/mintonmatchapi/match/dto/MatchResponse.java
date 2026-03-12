package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.CostPolicy;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class MatchResponse {

    private Long matchId;
    private Long hostId;
    private String title;
    private String description;
    private LocalDate matchDate;
    private LocalTime startTime;
    private Integer durationMin;
    private String locationName;
    private String locationAddress;
    private String regionCode;
    private Integer maxPeople;
    private String targetLevels;
    private CostPolicy costPolicy;
    private MatchStatus status;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;

    public static MatchResponse from(Match match) {
        return MatchResponse.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .title(match.getTitle())
                .description(match.getDescription())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .durationMin(match.getDurationMin())
                .locationName(match.getLocationName())
                .locationAddress(match.getLocationAddress())
                .regionCode(match.getRegionCode())
                .maxPeople(match.getMaxPeople())
                .targetLevels(match.getTargetLevels())
                .costPolicy(match.getCostPolicy())
                .status(match.getStatus())
                .imageUrl(match.getImageUrl())
                .latitude(match.getLatitude())
                .longitude(match.getLongitude())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
