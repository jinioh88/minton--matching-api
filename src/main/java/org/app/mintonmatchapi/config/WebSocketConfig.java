package org.app.mintonmatchapi.config;

import org.app.mintonmatchapi.chat.stomp.ChatStompAuthChannelInterceptor;
import org.app.mintonmatchapi.chat.stomp.relay.RedisStompRelayBrokerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor;
    private final ObjectProvider<RedisStompRelayBrokerInterceptor> redisStompRelayBrokerInterceptor;
    private final MintonWebProperties mintonWebProperties;

    public WebSocketConfig(ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor,
                          ObjectProvider<RedisStompRelayBrokerInterceptor> redisStompRelayBrokerInterceptor,
                          MintonWebProperties mintonWebProperties) {
        this.chatStompAuthChannelInterceptor = chatStompAuthChannelInterceptor;
        this.redisStompRelayBrokerInterceptor = redisStompRelayBrokerInterceptor;
        this.mintonWebProperties = mintonWebProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        redisStompRelayBrokerInterceptor.ifAvailable(
                relay -> registry.configureBrokerChannel().interceptors(relay));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] patterns = mintonWebProperties.corsAllowedOriginPatternArray();
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns(patterns);
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns(patterns).withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatStompAuthChannelInterceptor);
    }
}
