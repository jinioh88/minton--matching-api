package org.app.mintonmatchapi.review.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {

    /**
     * 가중 평점 프라이어 가상 표본 수 M ({@code docs/요구사항분석.md}, Sprint4 할일).
     */
    private int ratingPriorCount = 10;

    /**
     * 프라이어 평균(내부 가중용). DB 초기 rating_score 와 별개.
     */
    private double ratingPriorMean = 5.0d;

    /**
     * 후기 내용 공개 유예: 기준 시각(경기 시작+소요시간)으로부터 이 시간(시간 단위)이 지나면
     * 상호 미작성이어도 내용 노출. {@code docs/요구사항분석.md} 후기 상호 익명성 절.
     */
    private int revealAfterFinishHours = 72;

    public void setRatingPriorCount(int ratingPriorCount) {
        this.ratingPriorCount = ratingPriorCount;
    }

    public void setRatingPriorMean(double ratingPriorMean) {
        this.ratingPriorMean = ratingPriorMean;
    }

    public void setRevealAfterFinishHours(int revealAfterFinishHours) {
        this.revealAfterFinishHours = revealAfterFinishHours;
    }
}
