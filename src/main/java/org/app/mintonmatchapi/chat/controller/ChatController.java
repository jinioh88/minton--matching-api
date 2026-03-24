package org.app.mintonmatchapi.chat.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.chat.dto.*;
import org.app.mintonmatchapi.chat.service.ChatService;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/rooms")
    public ApiResponse<Page<ChatRoomListItemResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        Page<ChatRoomListItemResponse> result = chatService.getMyChatRooms(userId, PageRequest.of(page, size));
        return ApiResponse.success(result);
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<ChatRoomDetailResponse> getChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(chatService.getChatRoomDetail(userId, roomId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessagePageResponse> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(chatService.getMessages(userId, roomId, cursor, afterId, size));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(chatService.sendMessage(userId, roomId, request));
    }

    @PatchMapping("/rooms/{roomId}/messages/{messageId}")
    public ApiResponse<ChatMessageResponse> editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody ChatMessagePatchRequest request) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(chatService.editMessage(userId, roomId, messageId, request));
    }

    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        chatService.deleteMessage(userId, roomId, messageId);
        return ApiResponse.success(null);
    }
}
