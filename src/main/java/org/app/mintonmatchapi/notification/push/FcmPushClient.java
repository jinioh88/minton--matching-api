package org.app.mintonmatchapi.notification.push;

import java.util.Map;

/**
 * FCM 단말 전송. 비활성/미설정 시 {@link FcmPushClientNoop} 가 주입된다.
 */
public interface FcmPushClient {

    /**
     * 실제 FCM 전송이 가능한지(초기화 성공).
     */
    boolean isReady();

    /**
     * 알림·data 메시지 전송. 실패 시 로깅만 하고 예외를 밖으로 던지지 않는다.
     */
    void sendToToken(String token, String title, String body, Map<String, String> data);
}
