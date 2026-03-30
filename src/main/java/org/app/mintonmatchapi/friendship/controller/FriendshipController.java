package org.app.mintonmatchapi.friendship.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.friendship.dto.FollowingUserResponse;
import org.app.mintonmatchapi.friendship.dto.FriendshipFollowRequest;
import org.app.mintonmatchapi.friendship.service.FriendshipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping
    public ApiResponse<FollowingUserResponse> follow(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FriendshipFollowRequest request) {
        Long followerId = AuthUtils.getUserIdOrThrow(principal);
        FollowingUserResponse body = friendshipService.follow(followerId, request.getFollowingUserId());
        return ApiResponse.success(body);
    }

    @GetMapping
    public ApiResponse<List<FollowingUserResponse>> listFollowing(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long followerId = AuthUtils.getUserIdOrThrow(principal);
        return ApiResponse.success(friendshipService.listFollowing(followerId));
    }

    @DeleteMapping("/{followingUserId}")
    public ApiResponse<Void> unfollow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long followingUserId) {
        Long followerId = AuthUtils.getUserIdOrThrow(principal);
        friendshipService.unfollow(followerId, followingUserId);
        return ApiResponse.success(null);
    }
}
