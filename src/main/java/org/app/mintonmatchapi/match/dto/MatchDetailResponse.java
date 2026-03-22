package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.CostPolicy;
import org.app.mintonmatchapi.match.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
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

    private MyParticipationSummary myParticipation;
    private Boolean canApply;
    private Boolean canCancel;
    private Boolean hasWaitingOffer;

    /**
     * 로그인 사용자가 해당 매칭 방장이고, CLOSED이며, 수동 종료 시각 조건을 만족할 때 true.
     * 비로그인·타 사용자는 false.
     */
    private boolean canFinishMatch;

    /**
     * FINISHED이고 로그인 사용자가 확정 참여자일 때, 본인이 아직 후기를 쓰지 않은 확정 참여자 userId 목록(본인 제외).
     * 그 외에는 빈 목록.
     */
    @Builder.Default
    private List<Long> reviewPendingUserIds = Collections.emptyList();

    /** 서버 시각 (ISO 8601). offerExpiresAt 카운트다운 정확도 향상용 */
    private String serverTime;
    /** 긴급 선착순 모드 여부 (경기 시작 2시간 미만 시 true) */
    private Boolean isEmergencyMode;
}
