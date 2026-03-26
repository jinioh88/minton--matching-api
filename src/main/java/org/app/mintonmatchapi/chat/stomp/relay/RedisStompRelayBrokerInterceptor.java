package org.app.mintonmatchapi.chat.stomp.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.config.StompRedisRelayProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.Base64;

/**
 * 애플리케이션이 {@code brokerChannel} 로 보낸 브로커 목적지 메시지를 Redis 로 발행해
 * 다른 노드의 구독자에게도 전달되게 한다.
 */
@Slf4j
public class RedisStompRelayBrokerInterceptor implements ChannelInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final StompRedisRelayProperties properties;
    private final StompRelayInstanceId instanceId;

    public RedisStompRelayBrokerInterceptor(StringRedisTemplate stringRedisTemplate,
                                           ObjectMapper objectMapper,
                                           StompRedisRelayProperties properties,
                                           StompRelayInstanceId instanceId) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.instanceId = instanceId;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (Boolean.TRUE.equals(message.getHeaders().get(RedisStompRelayHeaders.RELAY_APPLIED))) {
            return message;
        }
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String dest = accessor.getDestination();
        if (dest == null || !isRelayDestination(dest)) {
            return message;
        }
        try {
            Object payload = message.getPayload();
            boolean binary = payload instanceof byte[];
            String payloadJson = binary
                    ? Base64.getEncoder().encodeToString((byte[]) payload)
                    : objectMapper.writeValueAsString(payload);
            StompClusterRelayEnvelope envelope = new StompClusterRelayEnvelope(
                    instanceId.value(),
                    dest,
                    payloadJson,
                    binary);
            stringRedisTemplate.convertAndSend(properties.getChannel(), objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("STOMP Redis relay publish 실패 destination={}", dest, e);
        }
        return message;
    }

    private static boolean isRelayDestination(String dest) {
        return dest.startsWith("/topic/")
                || dest.startsWith("/queue/")
                || dest.startsWith("/user/");
    }
}
