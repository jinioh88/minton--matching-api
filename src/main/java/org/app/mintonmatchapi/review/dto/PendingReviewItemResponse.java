package org.app.mintonmatchapi.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.user.entity.User;

/**
 * 아직 작성하지 않은 후기 1건(매칭·피평가자 쌍). 작성 API는 기존 {@code POST /api/matches/{matchId}/reviews} 사용.
 */
@Getter
@Builder
public class PendingReviewItemResponse {

    private Long matchId;
    private MatchReviewSummaryResponse match;
    private Long revieweeId;
    private ReviewerPublicSummaryResponse reviewee;

    public static PendingReviewItemResponse of(Match match, User reviewee) {
        return PendingReviewItemResponse.builder()
                .matchId(match.getId())
                .match(MatchReviewSummaryResponse.from(match))
                .revieweeId(reviewee.getId())
                .reviewee(ReviewerPublicSummaryResponse.from(reviewee))
                .build();
    }
}
