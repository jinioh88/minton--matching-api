package org.app.mintonmatchapi.chat.service;

import org.app.mintonmatchapi.chat.dto.*;
import org.app.mintonmatchapi.chat.entity.ChatMessage;
import org.app.mintonmatchapi.chat.entity.ChatMessageType;
import org.app.mintonmatchapi.chat.entity.ChatRoom;
import org.app.mintonmatchapi.chat.repository.ChatMessageRepository;
import org.app.mintonmatchapi.chat.repository.ChatRoomRepository;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final int EDIT_WINDOW_MINUTES = 15;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    public ChatService(ChatRoomRepository chatRoomRepository,
                      ChatMessageRepository chatMessageRepository,
                      ChatRoomService chatRoomService,
                      UserRepository userRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomService = chatRoomService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomListItemResponse> getMyChatRooms(Long userId, Pageable pageable) {
        Page<ChatRoom> rooms = chatRoomRepository.findChatRoomsForUser(userId, pageable);
        List<Long> roomIds = rooms.getContent().stream().map(ChatRoom::getId).toList();
        Map<Long, ChatMessage> lastByRoom = loadLatestMessagesByRoomIds(roomIds);
        return rooms.map(room -> ChatRoomListItemResponse.of(room, lastByRoom.get(room.getId())));
    }

    private Map<Long, ChatMessage> loadLatestMessagesByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        List<ChatMessage> lasts = chatMessageRepository.findLatestVisibleByRoomIdsWithSender(roomIds);
        return lasts.stream().collect(Collectors.toMap(m -> m.getRoom().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(Long userId, Long roomId) {
        chatRoomService.assertCanAccessChatByRoomId(userId, roomId);
        ChatRoom room = chatRoomService.getByIdWithMatchAndHostOrThrow(roomId);
        ChatMessage last = loadSingleLatestMessage(roomId);
        return ChatRoomDetailResponse.of(room, last);
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetailByMatchId(Long userId, Long matchId) {
        chatRoomService.assertCanAccessChat(userId, matchId);
        ChatRoom room = chatRoomRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoom loaded = chatRoomService.getByIdWithMatchAndHostOrThrow(room.getId());
        ChatMessage last = loadSingleLatestMessage(loaded.getId());
        return ChatRoomDetailResponse.of(loaded, last);
    }

    private ChatMessage loadSingleLatestMessage(Long roomId) {
        Page<ChatMessage> page = chatMessageRepository.findVisibleByRoomIdOrderByIdDesc(
                roomId, PageRequest.of(0, 1));
        return page.getContent().isEmpty() ? null : page.getContent().getFirst();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(Long userId, Long roomId, Long cursor, Long afterId, int size) {
        chatRoomService.assertCanAccessChatByRoomId(userId, roomId);
        int safeSize = Math.min(Math.max(size, 1), 100);

        if (afterId != null) {
            List<ChatMessage> newer = chatMessageRepository
                    .findVisibleByRoomIdAndIdGreaterThan(roomId, afterId, PageRequest.of(0, safeSize));
            return new ChatMessagePageResponse(
                    newer.stream().map(ChatMessageResponse::from).toList(),
                    null);
        }

        int fetchSize = safeSize + 1;
        Pageable p = PageRequest.of(0, fetchSize);
        Page<ChatMessage> page = cursor == null
                ? chatMessageRepository.findVisibleByRoomIdOrderByIdDesc(roomId, p)
                : chatMessageRepository.findVisibleByRoomIdAndIdLessThan(roomId, cursor, p);

        List<ChatMessage> batch = new ArrayList<>(page.getContent());
        boolean hasMore = batch.size() > safeSize;
        if (hasMore) {
            batch = new ArrayList<>(batch.subList(0, safeSize));
        }
        Collections.reverse(batch);

        Long nextCursor = null;
        if (hasMore) {
            nextCursor = batch.stream().map(ChatMessage::getId).min(Long::compareTo).orElse(null);
        }

        return new ChatMessagePageResponse(
                batch.stream().map(ChatMessageResponse::from).toList(),
                nextCursor);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long roomId, ChatMessageSendRequest request) {
        chatRoomService.assertCanWriteChat(userId, roomId);
        ChatRoom room = chatRoomService.getByIdWithMatchAndHostOrThrow(roomId);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        ChatMessageType type = request.getMessageType() != null ? request.getMessageType() : ChatMessageType.TEXT;
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent().trim())
                .messageType(type)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageResponse.from(saved);
    }

    @Transactional
    public ChatMessageResponse editMessage(Long userId, Long roomId, Long messageId, ChatMessagePatchRequest request) {
        chatRoomService.assertCanWriteChat(userId, roomId);
        ChatMessage message = chatMessageRepository.findByIdAndRoom_Id(messageId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (message.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 보낸 메시지만 수정할 수 있습니다.");
        }
        if (message.getCreatedAt().plusMinutes(EDIT_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "메시지 수정 가능 시간이 지났습니다.");
        }
        message.markEdited(request.getContent().trim());
        return ChatMessageResponse.from(message);
    }

    @Transactional
    public void deleteMessage(Long userId, Long roomId, Long messageId) {
        chatRoomService.assertCanWriteChat(userId, roomId);
        ChatMessage message = chatMessageRepository.findByIdAndRoom_Id(messageId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (message.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 보낸 메시지만 삭제할 수 있습니다.");
        }
        message.markDeleted();
    }
}
