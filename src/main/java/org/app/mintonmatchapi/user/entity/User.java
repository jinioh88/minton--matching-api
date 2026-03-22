package org.app.mintonmatchapi.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "profile_img", columnDefinition = "TEXT")
    private String profileImg;

    @Column(name = "level", length = 20)
    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(name = "interest_loc_1", length = 50)
    private String interestLoc1;

    @Column(name = "interest_loc_2", length = 50)
    private String interestLoc2;

    @Column(name = "racket_info", length = 100)
    private String racketInfo;

    @Column(name = "play_style", length = 50)
    private String playStyle;

    @Column(name = "rating_score", nullable = false)
    private Float ratingScore = 5.0f;

    @Column(name = "penalty_count", nullable = false)
    private Integer penaltyCount = 0;

    @Column(name = "participation_banned_until")
    private LocalDateTime participationBannedUntil;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "penalty_points", nullable = false)
    private Integer penaltyPoints = 0;

    @Builder
    public User(Provider provider, String providerId, String email, String nickname,
                String profileImg, Level level, String interestLoc1, String interestLoc2,
                String racketInfo, String playStyle) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.profileImg = profileImg;
        this.level = level;
        this.interestLoc1 = interestLoc1;
        this.interestLoc2 = interestLoc2;
        this.racketInfo = racketInfo;
        this.playStyle = playStyle;
    }

    public void updateProfile(String nickname, String profileImg, Level level,
                              String interestLoc1, String interestLoc2,
                              String racketInfo, String playStyle) {
        if (nickname != null) this.nickname = nickname;
        if (profileImg != null) this.profileImg = profileImg;
        if (level != null) this.level = level;
        if (interestLoc1 != null) this.interestLoc1 = interestLoc1;
        if (interestLoc2 != null) this.interestLoc2 = interestLoc2;
        if (racketInfo != null) this.racketInfo = racketInfo;
        if (playStyle != null) this.playStyle = playStyle;
    }

    /**
     * 후기 가중 평점 갱신 결과를 반영한다.
     */
    public void updateRatingScore(float newRatingScore) {
        this.ratingScore = newRatingScore;
    }

    public void recordPenaltyGrant(int penaltyWeightDelta) {
        int pc = penaltyCount != null ? penaltyCount : 0;
        this.penaltyCount = pc + 1;
        int pts = penaltyPoints != null ? penaltyPoints : 0;
        this.penaltyPoints = pts + penaltyWeightDelta;
    }

    /** 더 늦은(긴) 제한 종료 시각을 유지한다. */
    public void mergeParticipationBannedUntil(LocalDateTime candidateEnd) {
        if (candidateEnd == null) {
            return;
        }
        if (participationBannedUntil == null || candidateEnd.isAfter(participationBannedUntil)) {
            this.participationBannedUntil = candidateEnd;
        }
    }

    public void mergeSuspendedUntil(LocalDateTime candidateEnd) {
        if (candidateEnd == null) {
            return;
        }
        if (suspendedUntil == null || candidateEnd.isAfter(suspendedUntil)) {
            this.suspendedUntil = candidateEnd;
        }
    }

    public void markBanned() {
        this.accountStatus = AccountStatus.BANNED;
    }

    public void markSuspendedIfNotBanned() {
        if (this.accountStatus != AccountStatus.BANNED) {
            this.accountStatus = AccountStatus.SUSPENDED;
        }
    }
}
