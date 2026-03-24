package org.app.mintonmatchapi.notification.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.notification.entity.Notification;
import org.app.mintonmatchapi.notification.entity.NotificationType;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long notificationId;
    private NotificationType type;
    private String title;
    private String body;
    private String payload;
    private Long relatedMatchId;
    private Long relatedParticipantId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .payload(n.getPayload())
                .relatedMatchId(n.getRelatedMatchId())
                .relatedParticipantId(n.getRelatedParticipantId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
