package org.app.mintonmatchapi.chat.repository;

import org.app.mintonmatchapi.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatRoomRepositoryCustom {

    /**
     * 내가 방장이거나 해당 매칭에서 ACCEPTED 인 경우만, 채팅방이 존재하는 행만.
     */
    Page<ChatRoom> findChatRoomsForUser(Long userId, Pageable pageable);
}
