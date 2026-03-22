package org.app.mintonmatchapi.penalty.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.penalty.dto.PenaltyGrantRequest;
import org.app.mintonmatchapi.penalty.dto.PenaltyGrantResponse;
import org.app.mintonmatchapi.penalty.service.PenaltyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class PenaltyController {

    private final PenaltyService penaltyService;

    public PenaltyController(PenaltyService penaltyService) {
        this.penaltyService = penaltyService;
    }

    @PostMapping("/{matchId}/penalties")
    public ApiResponse<PenaltyGrantResponse> grantPenalty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId,
            @Valid @RequestBody PenaltyGrantRequest request) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        PenaltyGrantResponse response = penaltyService.grantPenalty(hostUserId, matchId, request);
        return ApiResponse.success(response);
    }
}
