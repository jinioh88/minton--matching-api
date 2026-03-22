package org.app.mintonmatchapi.penalty.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.penalty.entity.Penalty;
import org.app.mintonmatchapi.penalty.entity.PenaltyType;
import org.app.mintonmatchapi.review.dto.MatchReviewSummaryResponse;

import java.time.LocalDateTime;

/**
 * 유저별 패널티 이력 행. {@code User.penaltyCount}는 본 목록의 전체 건수(필터 없음)와 같다.
 */
@Getter
@Builder
public class PenaltyListItemResponse {

    private Long penaltyId;
    private PenaltyType type;
    private MatchReviewSummaryResponse match;
    private LocalDateTime createdAt;
    /** 부여 방장의 공개 닉네임 */
    private String hostNickname;

    public static PenaltyListItemResponse from(Penalty penalty) {
        return PenaltyListItemResponse.builder()
                .penaltyId(penalty.getId())
                .type(penalty.getType())
                .match(MatchReviewSummaryResponse.from(penalty.getMatch()))
                .createdAt(penalty.getCreatedAt())
                .hostNickname(penalty.getHost().getNickname())
                .build();
    }
}
