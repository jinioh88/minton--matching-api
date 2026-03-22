package org.app.mintonmatchapi.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.review.entity.Review;
import org.app.mintonmatchapi.review.entity.ReviewHashtag;
import org.app.mintonmatchapi.review.entity.ReviewSentiment;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long matchId;
    private Long revieweeId;
    private ReviewSentiment sentiment;
    private int score;
    private List<String> hashtags;
    private String detail;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        List<String> tagNames = review.getHashtags().stream()
                .map(ReviewHashtag::getCode)
                .map(Enum::name)
                .toList();
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .matchId(review.getMatch().getId())
                .revieweeId(review.getReviewee().getId())
                .sentiment(review.getSentiment())
                .score(review.getScore())
                .hashtags(tagNames)
                .detail(review.getDetail())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
