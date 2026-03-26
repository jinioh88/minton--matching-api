package org.app.mintonmatchapi.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.review.entity.Review;
import org.app.mintonmatchapi.review.entity.ReviewHashtag;
import org.app.mintonmatchapi.review.entity.ReviewSentiment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내가 작성한 후기 목록 행. 작성자 본인 조회이므로 내용·상대 정보 항상 공개.
 */
@Getter
@Builder
public class WrittenReviewListItemResponse {

    private Long reviewId;
    private MatchReviewSummaryResponse match;
    private ReviewerPublicSummaryResponse reviewee;
    private ReviewSentiment sentiment;
    private Integer score;
    private List<String> hashtags;
    private String detail;
    private LocalDateTime createdAt;

    public static WrittenReviewListItemResponse from(Review review) {
        List<String> tagNames = review.getHashtags().stream()
                .map(ReviewHashtag::getCode)
                .map(Enum::name)
                .toList();

        return WrittenReviewListItemResponse.builder()
                .reviewId(review.getId())
                .match(MatchReviewSummaryResponse.from(review.getMatch()))
                .reviewee(ReviewerPublicSummaryResponse.from(review.getReviewee()))
                .sentiment(review.getSentiment())
                .score(review.getScore())
                .hashtags(tagNames)
                .detail(review.getDetail())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
