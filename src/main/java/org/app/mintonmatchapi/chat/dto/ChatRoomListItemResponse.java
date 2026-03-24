package org.app.mintonmatchapi.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.chat.entity.ChatMessage;
import org.app.mintonmatchapi.chat.entity.ChatRoom;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomListItemResponse {

    private Long matchId;
    private Long roomId;
    private String matchTitle;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;

    public static ChatRoomListItemResponse of(ChatRoom room, ChatMessage lastMessage) {
        String preview = null;
        LocalDateTime at = null;
        if (lastMessage != null) {
            at = lastMessage.getCreatedAt();
            String c = lastMessage.getContent();
            if (c != null) {
                preview = c.length() > 120 ? c.substring(0, 120) + "…" : c;
            }
        }
        return ChatRoomListItemResponse.builder()
                .matchId(room.getMatch().getId())
                .roomId(room.getId())
                .matchTitle(room.getMatch().getTitle())
                .lastMessagePreview(preview)
                .lastMessageAt(at)
                .build();
    }
}
