package org.app.mintonmatchapi.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.user.entity.AccountStatus;
import org.app.mintonmatchapi.user.entity.Level;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private Long id;
    private String nickname;
    private String profileImg;
    private Level level;
    private String interestLoc1;
    private String interestLoc2;
    private String racketInfo;
    private String playStyle;
    private Float ratingScore;
    private Integer penaltyCount;
    private LocalDateTime joinedAt;

    /**
     * 본인만: 참여 신청 제한 만료 시각(ISO). 타인 프로필에서는 미포함.
     */
    private LocalDateTime participationBannedUntil;

    /**
     * 본인만: 넓은 정지 만료 시각(ISO). 타인 프로필에서는 미포함.
     */
    private LocalDateTime suspendedUntil;

    /**
     * 본인만. 타인에게는 노출하지 않음(스토킹·불필요 노출 완화).
     */
    private AccountStatus accountStatus;

    /**
     * 패널티 1~(참여 금지 직전) 구간 주의 표시용. 본인·타인 공통(공개 신뢰도).
     */
    private boolean showCautionBadge;

    /**
     * 받은 후기 건수. 후기 목록 API({@code PageResponse})의 {@code totalElements}와 동일 기준.
     */
    private Long receivedReviewCount;

    /**
     * 내 프로필용 — 제재 시각·계정 상태 전부 노출.
     */
    public static ProfileResponse ofMe(User user, long receivedReviewCount, int participationBanStrikeThreshold) {
        return ProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .level(user.getLevel())
                .interestLoc1(user.getInterestLoc1())
                .interestLoc2(user.getInterestLoc2())
                .racketInfo(user.getRacketInfo())
                .playStyle(user.getPlayStyle())
                .ratingScore(user.getRatingScore())
                .penaltyCount(user.getPenaltyCount())
                .joinedAt(user.getCreatedAt())
                .participationBannedUntil(user.getParticipationBannedUntil())
                .suspendedUntil(user.getSuspendedUntil())
                .accountStatus(user.getAccountStatus())
                .showCautionBadge(computeShowCautionBadge(user, participationBanStrikeThreshold))
                .receivedReviewCount(receivedReviewCount)
                .build();
    }

    /**
     * 타인 프로필 — 제재 시각·계정 상태 미포함, 주의 배지·후기 건수·공개 프로필 필드만.
     */
    public static ProfileResponse ofOther(User user, long receivedReviewCount, int participationBanStrikeThreshold) {
        return ProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .level(user.getLevel())
                .interestLoc1(user.getInterestLoc1())
                .interestLoc2(user.getInterestLoc2())
                .racketInfo(user.getRacketInfo())
                .playStyle(user.getPlayStyle())
                .ratingScore(user.getRatingScore())
                .penaltyCount(user.getPenaltyCount())
                .joinedAt(user.getCreatedAt())
                .showCautionBadge(computeShowCautionBadge(user, participationBanStrikeThreshold))
                .receivedReviewCount(receivedReviewCount)
                .build();
    }

    private static boolean computeShowCautionBadge(User user, int participationBanStrikeThreshold) {
        int pc = user.getPenaltyCount() != null ? user.getPenaltyCount() : 0;
        return pc > 0 && pc < participationBanStrikeThreshold;
    }
}
