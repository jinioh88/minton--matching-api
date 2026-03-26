package org.app.mintonmatchapi.chat.stomp.relay;

import java.util.UUID;

/**
 * 릴레이 시 동일 인스턴스로 되돌아온 메시지를 무시하기 위한 식별자.
 */
public final class StompRelayInstanceId {

    private final String value = UUID.randomUUID().toString();

    public String value() {
        return value;
    }
}
