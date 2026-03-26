package org.app.mintonmatchapi.notification.push;

import java.util.Map;

public class FcmPushClientNoop implements FcmPushClient {

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        // no-op
    }
}
