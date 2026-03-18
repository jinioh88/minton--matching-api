package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParticipantApplyResponse {

    private Long participationId;
    private ParticipantStatus status;
    private Integer queueOrder;
    private String applyMessage;
    private LocalDateTime offerExpiresAt;

    public static ParticipantApplyResponse from(MatchParticipant participant) {
        return ParticipantApplyResponse.builder()
                .participationId(participant.getId())
                .status(participant.getStatus())
                .queueOrder(participant.getQueueOrder())
                .applyMessage(participant.getApplyMessage())
                .offerExpiresAt(participant.getOfferExpiresAt())
                .build();
    }
}
