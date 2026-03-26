package org.app.mintonmatchapi.notification.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.notification.entity.Notification;

/**
 * STOMP {@code /user/queue/notifications} 및 FCM data 페이로드용 경량 DTO.
 */
@Getter
@Builder
public class NotificationRealtimePayload {

    private final long notificationId;
    private final long recipientUserId;
    private final String type;
    private final String title;
    private final String summary;
    private final Long relatedMatchId;
    private final Long relatedParticipantId;

    public static NotificationRealtimePayload from(Notification n) {
        return NotificationRealtimePayload.builder()
                .notificationId(n.getId())
                .recipientUserId(n.getUser().getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .summary(n.getBody() != null ? n.getBody() : "")
                .relatedMatchId(n.getRelatedMatchId())
                .relatedParticipantId(n.getRelatedParticipantId())
                .build();
    }
}
