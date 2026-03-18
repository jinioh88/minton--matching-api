package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParticipantApplicationResponse {

    private Long participationId;
    private Long userId;
    private String nickname;
    private String profileImg;
    private Float ratingScore;
    private ParticipantStatus status;
    private Integer queueOrder;
    private String applyMessage;
    private LocalDateTime appliedAt;
    private LocalDateTime offerExpiresAt;

    public static ParticipantApplicationResponse from(MatchParticipant participant) {
        if (participant == null) {
            return null;
        }
        User user = participant.getUser();
        return ParticipantApplicationResponse.builder()
                .participationId(participant.getId())
                .userId(user != null ? user.getId() : null)
                .nickname(user != null ? user.getNickname() : null)
                .profileImg(user != null ? user.getProfileImg() : null)
                .ratingScore(user != null ? user.getRatingScore() : null)
                .status(participant.getStatus())
                .queueOrder(participant.getQueueOrder())
                .applyMessage(participant.getApplyMessage())
                .appliedAt(participant.getCreatedAt())
                .offerExpiresAt(participant.getOfferExpiresAt())
                .build();
    }
}
