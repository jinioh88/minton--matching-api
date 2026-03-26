package org.app.mintonmatchapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.mintonmatchapi.chat.stomp.relay.RedisStompRelayBrokerInterceptor;
import org.app.mintonmatchapi.chat.stomp.relay.RedisStompRelayMessageListener;
import org.app.mintonmatchapi.chat.stomp.relay.StompRelayInstanceId;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * STOMP SimpleBroker 메시지를 Redis Pub/Sub 으로 인스턴스 간 중계한다.
 * {@code minton.chat.stomp-redis-relay.enabled=false}(기본)이면 빈을 등록하지 않는다.
 */
@Configuration
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "minton.chat.stomp-redis-relay", name = "enabled", havingValue = "true")
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisStompRelayConfiguration {

    @Bean
    StompRelayInstanceId stompRelayInstanceId() {
        return new StompRelayInstanceId();
    }

    @Bean
    RedisStompRelayBrokerInterceptor redisStompRelayBrokerInterceptor(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            StompRedisRelayProperties properties,
            StompRelayInstanceId instanceId) {
        return new RedisStompRelayBrokerInterceptor(stringRedisTemplate, objectMapper, properties, instanceId);
    }

    @Bean
    RedisStompRelayMessageListener redisStompRelayMessageListener(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            StompRelayInstanceId instanceId) {
        return new RedisStompRelayMessageListener(messagingTemplate, objectMapper, instanceId);
    }

    @Bean(destroyMethod = "destroy")
    RedisMessageListenerContainer redisStompRelayListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisStompRelayMessageListener listener,
            StompRedisRelayProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(properties.getChannel()));
        return container;
    }
}
