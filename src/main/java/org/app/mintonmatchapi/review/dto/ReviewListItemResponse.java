package org.app.mintonmatchapi.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.review.entity.Review;
import org.app.mintonmatchapi.review.entity.ReviewHashtag;
import org.app.mintonmatchapi.review.entity.ReviewSentiment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유저별 받은 후기 목록 행. {@code contentRevealed == false} 이면 내용·작성자 식별을 마스킹한다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewListItemResponse {

    private Long reviewId;
    private MatchReviewSummaryResponse match;
    private ReviewerPublicSummaryResponse reviewer;
    private ReviewSentiment sentiment;
    private Integer score;
    private List<String> hashtags;
    private String detail;
    private LocalDateTime createdAt;
    private boolean contentRevealed;
    /** 해당 행은 항상 실제 후기 레코드이므로 true (작성 완료). */
    private boolean reviewSubmitted;

    public static ReviewListItemResponse of(Review review, boolean contentRevealed) {
        MatchReviewSummaryResponse match = MatchReviewSummaryResponse.from(review.getMatch());
        if (!contentRevealed) {
            return ReviewListItemResponse.builder()
                    .reviewId(review.getId())
                    .match(match)
                    .createdAt(review.getCreatedAt())
                    .contentRevealed(false)
                    .reviewSubmitted(true)
                    .build();
        }

        List<String> tagNames = review.getHashtags().stream()
                .map(ReviewHashtag::getCode)
                .map(Enum::name)
                .toList();

        return ReviewListItemResponse.builder()
                .reviewId(review.getId())
                .match(match)
                .reviewer(ReviewerPublicSummaryResponse.from(review.getReviewer()))
                .sentiment(review.getSentiment())
                .score(review.getScore())
                .hashtags(tagNames)
                .detail(review.getDetail())
                .createdAt(review.getCreatedAt())
                .contentRevealed(true)
                .reviewSubmitted(true)
                .build();
    }
}
