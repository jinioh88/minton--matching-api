package org.app.mintonmatchapi.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.chat.entity.ChatMessage;
import org.app.mintonmatchapi.chat.entity.ChatRoom;

@Getter
@Builder
public class ChatRoomDetailResponse {

    private Long roomId;
    private Long matchId;
    private MatchChatNoticeResponse matchNotice;
    private ChatMessageResponse lastMessage;

    public static ChatRoomDetailResponse of(ChatRoom room, ChatMessage lastMessage) {
        ChatMessageResponse last = lastMessage != null ? ChatMessageResponse.from(lastMessage) : null;
        return ChatRoomDetailResponse.builder()
                .roomId(room.getId())
                .matchId(room.getMatch().getId())
                .matchNotice(MatchChatNoticeResponse.from(room.getMatch()))
                .lastMessage(last)
                .build();
    }
}
