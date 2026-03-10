package org.app.mintonmatchapi.user.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.user.entity.Level;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@Builder
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

    /**
     * 내 프로필용 - 모든 필드 노출
     */
    public static ProfileResponse ofMe(User user) {
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
                .build();
    }

    /**
     * 타인 프로필용 - 공개 가능한 필드만 노출
     */
    public static ProfileResponse ofOther(User user) {
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
                .build();
    }
}
