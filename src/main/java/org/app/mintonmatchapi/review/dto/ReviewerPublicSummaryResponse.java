package org.app.mintonmatchapi.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@Builder
public class ReviewerPublicSummaryResponse {

    private Long userId;
    private String nickname;
    private String profileImg;

    public static ReviewerPublicSummaryResponse from(User reviewer) {
        return ReviewerPublicSummaryResponse.builder()
                .userId(reviewer.getId())
                .nickname(reviewer.getNickname())
                .profileImg(reviewer.getProfileImg())
                .build();
    }
}
