package org.app.mintonmatchapi.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@Builder
public class ParticipantSummary {

    private Long participationId;
    private Long userId;
    private String nickname;
    private String profileImg;
    private Float ratingScore;
    private Integer queueOrder;
    private String applyMessage;

    public static ParticipantSummary from(MatchParticipant participant) {
        if (participant == null) {
            return null;
        }
        User user = participant.getUser();
        return ParticipantSummary.builder()
                .participationId(participant.getId())
                .userId(user != null ? user.getId() : null)
                .nickname(user != null ? user.getNickname() : null)
                .profileImg(user != null ? user.getProfileImg() : null)
                .ratingScore(user != null ? user.getRatingScore() : null)
                .queueOrder(participant.getQueueOrder())
                .applyMessage(participant.getApplyMessage())
                .build();
    }
}
