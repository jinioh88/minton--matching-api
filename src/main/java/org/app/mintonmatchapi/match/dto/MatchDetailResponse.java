package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.CostPolicy;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class MatchDetailResponse {

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
    private Integer currentPeople;
    private String targetLevels;
    private CostPolicy costPolicy;
    private MatchStatus status;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;

    private HostSummary host;
    private List<ParticipantSummary> confirmedParticipants;
    private List<ParticipantSummary> waitingList;
    private int waitingCount;
}
