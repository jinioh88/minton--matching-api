package org.app.mintonmatchapi.user.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.auth.annotation.IfLogin;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.common.dto.PageResponse;
import org.app.mintonmatchapi.penalty.dto.PenaltyListItemResponse;
import org.app.mintonmatchapi.penalty.service.PenaltyService;
import org.app.mintonmatchapi.review.dto.PendingReviewItemResponse;
import org.app.mintonmatchapi.review.dto.ReviewListItemResponse;
import org.app.mintonmatchapi.review.dto.WrittenReviewListItemResponse;
import org.app.mintonmatchapi.review.service.ReviewService;
import org.app.mintonmatchapi.match.dto.MatchListResponse;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.service.MatchService;
import org.app.mintonmatchapi.user.dto.NicknameCheckResponse;
import org.app.mintonmatchapi.user.dto.ProfileResponse;
import org.app.mintonmatchapi.user.dto.ProfileUpdateRequest;
import org.app.mintonmatchapi.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final MatchService matchService;
    private final ReviewService reviewService;
    private final PenaltyService penaltyService;

    public UserController(UserService userService, MatchService matchService,
                            ReviewService reviewService, PenaltyService penaltyService) {
        this.userService = userService;
        this.matchService = matchService;
        this.reviewService = reviewService;
        this.penaltyService = penaltyService;
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

    /**
     * 마이페이지 — 내가 방장인 매칭 목록 (Sprint7 Step2).
     */
    @GetMapping("/me/matches/hosted")
    public ApiResponse<Page<MatchListResponse>> getMyHostedMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        Page<MatchListResponse> result = matchService.getMyHostedMatches(
                userId, status, dateFrom, dateTo, PageRequest.of(page, size));
        return ApiResponse.success(result);
    }

    /**
     * 마이페이지 — 확정 참가(ACCEPTED) 매칭 목록 (Sprint7 Step2).
     */
    @GetMapping("/me/matches/participated")
    public ApiResponse<Page<MatchListResponse>> getMyParticipatedMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        Page<MatchListResponse> result = matchService.getMyParticipatedMatches(
                userId, status, dateFrom, dateTo, PageRequest.of(page, size));
        return ApiResponse.success(result);
    }

    /**
     * 마이페이지 — 내가 작성한 후기 목록 (Sprint7 Step3).
     */
    @GetMapping("/me/reviews/written")
    public ApiResponse<PageResponse<WrittenReviewListItemResponse>> getMyWrittenReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                PageResponse.of(reviewService.listMyWrittenReviews(userId, pageable)));
    }

    /**
     * 마이페이지 — 작성 대기 후기(종료 매칭·확정 참여 상대 중 미작성) (Sprint7 Step3).
     */
    @GetMapping("/me/reviews/pending")
    public ApiResponse<PageResponse<PendingReviewItemResponse>> getMyPendingReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        PageRequest pageable = PageRequest.of(page, size);
        return ApiResponse.success(
                PageResponse.of(reviewService.listMyPendingReviews(userId, pageable)));
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

    @GetMapping("/{userId}/reviews")
    public ApiResponse<PageResponse<ReviewListItemResponse>> listReceivedReviews(
            @PathVariable Long userId,
            @IfLogin UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long viewerId = AuthUtils.getUserIdOrNull(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                PageResponse.of(reviewService.listReceivedReviews(userId, viewerId, pageable)));
    }

    @GetMapping("/{userId}/penalties")
    public ApiResponse<PageResponse<PenaltyListItemResponse>> listPenalties(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                PageResponse.of(penaltyService.listPenaltiesForUser(userId, pageable)));
    }
}
