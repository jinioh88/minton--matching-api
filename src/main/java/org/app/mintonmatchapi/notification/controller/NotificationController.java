package org.app.mintonmatchapi.notification.controller;

import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.notification.dto.NotificationResponse;
import org.app.mintonmatchapi.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        Page<NotificationResponse> result = notificationService.getMyNotifications(userId, PageRequest.of(page, size));
        return ApiResponse.success(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(notificationService.countUnread(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(notificationService.markRead(userId, notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        int updated = notificationService.markAllRead(userId);
        return ApiResponse.success(updated);
    }
}
