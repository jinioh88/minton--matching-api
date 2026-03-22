package org.app.mintonmatchapi.penalty.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.penalty.entity.Penalty;
import org.app.mintonmatchapi.penalty.entity.PenaltyType;

@Getter
@Builder
public class PenaltyGrantResponse {

    private Long penaltyId;
    private Long matchId;
    private Long penalizedUserId;
    private PenaltyType type;

    public static PenaltyGrantResponse from(Penalty penalty) {
        return PenaltyGrantResponse.builder()
                .penaltyId(penalty.getId())
                .matchId(penalty.getMatch().getId())
                .penalizedUserId(penalty.getPenalizedUser().getId())
                .type(penalty.getType())
                .build();
    }
}
