package org.app.mintonmatchapi.chat.stomp.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Redis 에서 수신한 브로커 메시지를 로컬 SimpleBroker 로 재주입한다.
 */
@Slf4j
public class RedisStompRelayMessageListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final StompRelayInstanceId instanceId;

    public RedisStompRelayMessageListener(SimpMessagingTemplate messagingTemplate,
                                         ObjectMapper objectMapper,
                                         StompRelayInstanceId instanceId) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            StompClusterRelayEnvelope envelope = objectMapper.readValue(raw, StompClusterRelayEnvelope.class);
            if (instanceId.value().equals(envelope.publisherId())) {
                return;
            }
            Object payload = envelope.binaryPayload()
                    ? Base64.getDecoder().decode(envelope.payloadJson())
                    : objectMapper.readValue(envelope.payloadJson(), Object.class);

            SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
            accessor.setDestination(envelope.destination());
            accessor.setHeader(RedisStompRelayHeaders.RELAY_APPLIED, Boolean.TRUE);
            accessor.setLeaveMutable(true);
            messagingTemplate.convertAndSend(envelope.destination(), payload, accessor.getMessageHeaders());
        } catch (Exception e) {
            log.warn("STOMP Redis relay 수신 처리 실패 body={}", raw, e);
        }
    }
}
