package org.app.mintonmatchapi.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.chat.entity.ChatMessage;
import org.app.mintonmatchapi.chat.entity.ChatMessageType;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private ChatMessageType messageType;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
