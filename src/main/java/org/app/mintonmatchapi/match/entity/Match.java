package org.app.mintonmatchapi.match.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "matches")
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "location_name", length = 200)
    private String locationName;

    @Column(name = "region_code", nullable = false, length = 50)
    private String regionCode;

    @Column(name = "max_people", nullable = false)
    private Integer maxPeople;

    @Column(name = "target_levels", length = 50)
    private String targetLevels;

    @Column(name = "cost_policy", length = 50)
    @Enumerated(EnumType.STRING)
    private CostPolicy costPolicy;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Builder
    public Match(User host, String title, String description, LocalDate matchDate, LocalTime startTime,
                 Integer durationMin, String locationName, String regionCode,
                 Integer maxPeople, String targetLevels, CostPolicy costPolicy, MatchStatus status,
                 String imageUrl, Double latitude, Double longitude) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.matchDate = matchDate;
        this.startTime = startTime;
        this.durationMin = durationMin;
        this.locationName = locationName;
        this.regionCode = regionCode;
        this.maxPeople = maxPeople;
        this.targetLevels = targetLevels;
        this.costPolicy = costPolicy;
        this.status = status;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 경기 시작 시각이 cutoff 이전인지 확인 (긴급 모드 판단용)
     * @param cutoff 기준 시각 (예: 현재 + 2시간)
     * @return 경기 시작이 cutoff보다 이전이면 true (긴급 선착순 모드)
     */
    public boolean isWithinEmergencyThreshold(LocalDateTime cutoff) {
        LocalDateTime matchStart = matchDate.atTime(startTime);
        return matchStart.isBefore(cutoff);
    }
}
