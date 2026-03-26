package org.app.mintonmatchapi.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.notification.service.PushTokenService;

import java.util.Map;

@Slf4j
public class FcmPushClientImpl implements FcmPushClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final PushTokenService pushTokenService;

    public FcmPushClientImpl(PushTokenService pushTokenService) {
        this.pushTokenService = pushTokenService;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body != null ? body : "")
                        .build())
                .putAllData(data)
                .build();

        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                FirebaseMessaging.getInstance().send(message);
                return;
            } catch (FirebaseMessagingException e) {
                if (isPermanentTokenFailure(e)) {
                    log.info("FCM 토큰 무효로 비활성화 tokenPrefix={}", tokenPrefix(token));
                    pushTokenService.deactivateByToken(token);
                    return;
                }
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("FCM 전송 실패 (재시도 소진) tokenPrefix={}", tokenPrefix(token), e);
                    return;
                }
                log.debug("FCM 전송 재시도 {}/{} tokenPrefix={}", attempt, MAX_ATTEMPTS, tokenPrefix(token));
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("FCM 재시도 대기 중 인터럽트", ie);
                    return;
                }
                backoff *= 2;
            }
        }
    }

    private static boolean isPermanentTokenFailure(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.SENDER_ID_MISMATCH;
    }

    private static String tokenPrefix(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }
}
