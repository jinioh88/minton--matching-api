package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@Builder
public class HostSummary {

    private Long id;
    private String nickname;
    private String profileImg;
    private Float ratingScore;

    public static HostSummary from(User user) {
        if (user == null) {
            return null;
        }
        return HostSummary.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .ratingScore(user.getRatingScore())
                .build();
    }
}
