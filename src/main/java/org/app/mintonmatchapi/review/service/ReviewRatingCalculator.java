package org.app.mintonmatchapi.review.service;

/**
 * 피평가자 {@code User.rating_score} 가중 갱신.
 * <p>
 * 참고: {@code docs/요구사항분석.md} 평점 절, {@code docs/Sprint4-할일.md} Step 3.
 * </p>
 * <ul>
 *   <li>{@code n = 0} (이번 저장 직전 받은 후기 없음): {@code R' = (M × priorMean + s) / (M + 1)}</li>
 *   <li>{@code n ≥ 1}: {@code R' = (R × (M + n) + s) / (M + n + 1)}</li>
 * </ul>
 * {@code M} = priorCount, {@code priorMean} = 내부 프라이어 평균, {@code R} = 저장된 평점
 * ({@code n = 0} 구간에서는 수식에 R 미사용), {@code s} = 새 후기 점수(1~5), {@code n} = 이번 INSERT 직전 reviewee 기준 후기 건수.
 */
public final class ReviewRatingCalculator {

    private ReviewRatingCalculator() {
    }

    public static float computeNewRating(float storedRatingR, long receivedCountN, int newScoreS,
                                         int priorCountM, double priorMean) {
        double rPrime;
        if (receivedCountN == 0) {
            rPrime = (priorCountM * priorMean + newScoreS) / (double) (priorCountM + 1);
        } else {
            rPrime = (storedRatingR * (priorCountM + receivedCountN) + newScoreS)
                    / (double) (priorCountM + receivedCountN + 1);
        }
        return (float) rPrime;
    }
}
