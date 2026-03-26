package org.app.mintonmatchapi.chat.stomp;

import org.app.mintonmatchapi.chat.dto.ChatMessageResponse;
import org.app.mintonmatchapi.chat.dto.ChatMessageSendRequest;
import org.app.mintonmatchapi.chat.service.ChatService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatStompService {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatStompService(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * REST {@link ChatService#sendMessage} 와 동일 규칙(권한·종료 매칭 거부)으로 저장한 뒤,
     * 커밋 완료 후 {@code /topic/chat.{roomId}} 로 브로드캐스트한다.
     */
    @Transactional
    public void sendAndBroadcast(Long userId, ChatStompSendPayload payload) {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setContent(payload.getContent());
        request.setMessageType(payload.getMessageType());
        ChatMessageResponse response = chatService.sendMessage(userId, payload.getRoomId(), request);
        long roomId = payload.getRoomId();
        String destination = "/topic/chat." + roomId;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(destination, response);
                }
            });
        } else {
            messagingTemplate.convertAndSend(destination, response);
        }
    }
}
