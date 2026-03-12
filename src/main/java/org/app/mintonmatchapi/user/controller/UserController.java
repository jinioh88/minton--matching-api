package org.app.mintonmatchapi.user.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.user.dto.NicknameCheckResponse;
import org.app.mintonmatchapi.user.dto.ProfileResponse;
import org.app.mintonmatchapi.user.dto.ProfileUpdateRequest;
import org.app.mintonmatchapi.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/check-nickname")
    public ApiResponse<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        NicknameCheckResponse response = userService.checkNickname(nickname);
        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ProfileResponse response = userService.getMyProfile(userId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/me")
    public ApiResponse<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ProfileResponse response = userService.updateMyProfile(userId, request);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("image") MultipartFile image) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ProfileResponse response = userService.uploadProfileImage(userId, image);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    public ApiResponse<ProfileResponse> getUserProfile(@PathVariable Long userId) {
        ProfileResponse response = userService.getUserProfile(userId);
        return ApiResponse.success(response);
    }
}
