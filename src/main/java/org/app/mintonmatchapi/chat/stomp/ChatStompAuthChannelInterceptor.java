package org.app.mintonmatchapi.chat.stomp;

import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.auth.service.JwtService;
import org.app.mintonmatchapi.chat.service.ChatRoomService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP CONNECT 시 JWT 검증·{@link UserPrincipal} 설정, SUBSCRIBE/SEND 시 채팅 권한·인증 검사.
 */
@Component
public class ChatStompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APP_CHAT_MESSAGES = "/app/chat/messages";
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat\\.(\\d+)$");

    private final JwtService jwtService;
    private final ChatRoomService chatRoomService;

    public ChatStompAuthChannelInterceptor(JwtService jwtService, ChatRoomService chatRoomService) {
        this.jwtService = jwtService;
        this.chatRoomService = chatRoomService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            String token = extractBearer(accessor);
            if (!StringUtils.hasText(token)) {
                throw new org.springframework.messaging.MessageDeliveryException("STOMP CONNECT에 Authorization(Bearer) 헤더가 필요합니다.");
            }
            try {
                Long userId = jwtService.getUserId(token);
                UserPrincipal principal = new UserPrincipal(userId);
                accessor.setUser(principal);
            } catch (Exception e) {
                throw new org.springframework.messaging.MessageDeliveryException("유효하지 않은 액세스 토큰입니다.");
            }
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            Long userId = requireUserId(accessor);
            String dest = accessor.getDestination();
            if (dest == null) {
                throw new org.springframework.messaging.MessageDeliveryException("구독 destination이 없습니다.");
            }
            Matcher m = CHAT_TOPIC.matcher(dest);
            if (m.matches()) {
                long roomId = Long.parseLong(m.group(1));
                chatRoomService.assertCanAccessChatByRoomId(userId, roomId);
                return message;
            }
            if (dest.equals("/user/queue/errors") || dest.equals("/user/queue/notifications")) {
                return message;
            }
            throw new org.springframework.messaging.MessageDeliveryException("허용되지 않은 구독 경로입니다: " + dest);
        }

        if (command == StompCommand.SEND) {
            requireUserId(accessor);
            String dest = accessor.getDestination();
            if (APP_CHAT_MESSAGES.equals(dest)) {
                return message;
            }
            throw new org.springframework.messaging.MessageDeliveryException("허용되지 않은 전송 경로입니다: " + dest);
        }

        return message;
    }

    private static Long requireUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UserPrincipal p) {
            return p.getUserId();
        }
        throw new org.springframework.messaging.MessageDeliveryException("인증이 필요합니다.");
    }

    private static String extractBearer(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader(AUTHORIZATION);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String raw = headers.getFirst();
        if (!StringUtils.hasText(raw) || !raw.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return raw.substring(BEARER_PREFIX.length()).trim();
    }
}
