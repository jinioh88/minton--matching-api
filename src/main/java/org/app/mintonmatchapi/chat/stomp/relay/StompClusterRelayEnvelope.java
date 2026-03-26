package org.app.mintonmatchapi.chat.stomp.relay;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 인스턴스 간 STOMP 브로커 메시지 전달용 페이로드(JSON).
 */
public record StompClusterRelayEnvelope(
        @JsonProperty("p") String publisherId,
        @JsonProperty("d") String destination,
        @JsonProperty("b") String payloadJson,
        @JsonProperty("bin") boolean binaryPayload
) {
}
