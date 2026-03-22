package org.app.mintonmatchapi.review.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.review.dto.ReviewCreateRequest;
import org.app.mintonmatchapi.review.dto.ReviewResponse;
import org.app.mintonmatchapi.review.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{matchId}/reviews")
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId,
            @Valid @RequestBody ReviewCreateRequest request) {
        Long reviewerId = AuthUtils.getUserIdOrThrow(principal);
        ReviewResponse response = reviewService.createReview(reviewerId, matchId, request);
        return ApiResponse.success(response);
    }
}
