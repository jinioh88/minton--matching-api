package org.app.mintonmatchapi.notification.event;

import org.app.mintonmatchapi.notification.entity.NotificationType;

/**
 * 트랜잭션 커밋 후 인앱 알림을 저장하기 위한 페이로드.
 */
public record NotificationDispatchCommand(
        Long recipientUserId,
        NotificationType type,
        String title,
        String body,
        String payload,
        Long relatedMatchId,
        Long relatedParticipantId
) {
    public static NotificationDispatchCommand of(Long recipientUserId, NotificationType type,
                                                 String title, String body,
                                                 Long relatedMatchId, Long relatedParticipantId) {
        return new NotificationDispatchCommand(recipientUserId, type, title, body, null,
                relatedMatchId, relatedParticipantId);
    }
}
