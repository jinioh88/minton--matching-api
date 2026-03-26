package org.app.mintonmatchapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 다중 인스턴스에서 STOMP SimpleBroker 간 메시지를 Redis Pub/Sub 으로 중계할 때 사용.
 * 비활성화 시(기본) 로컬·단일 Pod 는 인메모리 SimpleBroker 만 사용한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "minton.chat.stomp-redis-relay")
public class StompRedisRelayProperties {

    /**
     * true 이면 broker 로 나가는 /topic·/queue·/user 메시지를 Redis 로 fan-out 한다.
     */
    private boolean enabled = false;

    /**
     * Redis PUBLISH/SUBSCRIBE 채널명.
     */
    private String channel = "minton:chat:stomp-relay";
}
