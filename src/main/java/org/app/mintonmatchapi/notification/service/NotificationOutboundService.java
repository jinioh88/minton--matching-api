package org.app.mintonmatchapi.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.notification.dto.NotificationRealtimePayload;
import org.app.mintonmatchapi.notification.entity.DevicePushToken;
import org.app.mintonmatchapi.notification.push.FcmPushClient;
import org.app.mintonmatchapi.notification.repository.DevicePushTokenRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 DB 저장 이후 STOMP·FCM 으로 단말에 전달한다.
 */
@Slf4j
@Service
public class NotificationOutboundService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DevicePushTokenRepository devicePushTokenRepository;
    private final FcmPushClient fcmPushClient;

    public NotificationOutboundService(SimpMessagingTemplate messagingTemplate,
                                      DevicePushTokenRepository devicePushTokenRepository,
                                      FcmPushClient fcmPushClient) {
        this.messagingTemplate = messagingTemplate;
        this.devicePushTokenRepository = devicePushTokenRepository;
        this.fcmPushClient = fcmPushClient;
    }

    @Async
    public void dispatchAfterPersist(NotificationRealtimePayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(payload.getRecipientUserId()),
                    "/queue/notifications",
                    payload);
        } catch (Exception e) {
            log.warn("STOMP 알림 전송 실패 userId={} notificationId={}",
                    payload.getRecipientUserId(), payload.getNotificationId(), e);
        }

        if (!fcmPushClient.isReady()) {
            return;
        }
        List<DevicePushToken> tokens = devicePushTokenRepository.findByUser_IdAndDisabledAtIsNull(
                payload.getRecipientUserId());
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, String> data = toFcmData(payload);
        for (DevicePushToken t : tokens) {
            fcmPushClient.sendToToken(t.getToken(), payload.getTitle(), payload.getSummary(), data);
        }
    }

    private static Map<String, String> toFcmData(NotificationRealtimePayload p) {
        Map<String, String> m = new HashMap<>();
        m.put("notificationId", Long.toString(p.getNotificationId()));
        m.put("type", p.getType());
        if (p.getRelatedMatchId() != null) {
            m.put("relatedMatchId", Long.toString(p.getRelatedMatchId()));
        }
        if (p.getRelatedParticipantId() != null) {
            m.put("relatedParticipantId", Long.toString(p.getRelatedParticipantId()));
        }
        return m;
    }
}
