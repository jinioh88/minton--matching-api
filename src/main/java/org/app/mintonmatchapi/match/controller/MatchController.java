package org.app.mintonmatchapi.match.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.auth.annotation.IfLogin;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.match.dto.MatchCreateRequest;
import org.app.mintonmatchapi.match.dto.MatchDetailResponse;
import org.app.mintonmatchapi.match.dto.MatchUpdateRequest;
import org.app.mintonmatchapi.match.dto.MatchListResponse;
import org.app.mintonmatchapi.match.dto.MatchResponse;
import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.dto.ParticipantApplicationResponse;
import org.app.mintonmatchapi.match.dto.ParticipantApplyRequest;
import org.app.mintonmatchapi.match.dto.ParticipantApplyResponse;
import org.app.mintonmatchapi.match.dto.ParticipantDecisionRequest;
import org.app.mintonmatchapi.match.service.MatchParticipantService;
import org.app.mintonmatchapi.match.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchParticipantService matchParticipantService;

    public MatchController(MatchService matchService, MatchParticipantService matchParticipantService) {
        this.matchService = matchService;
        this.matchParticipantService = matchParticipantService;
    }

    @PostMapping
    public ApiResponse<MatchResponse> createMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MatchCreateRequest request) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        MatchResponse response = matchService.createMatch(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<Page<MatchListResponse>> getMatches(
            @IfLogin UserPrincipal principal,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<String> regionCodes = parseRegionCodes(regionCode);
        Long userId = AuthUtils.getUserIdOrNull(principal);

        MatchSearchCondition condition = MatchSearchCondition.builder()
                .regionCodes(regionCodes)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .level(level)
                .pageable(PageRequest.of(page, size))
                .build();

        Page<MatchListResponse> result = matchService.getMatchList(condition, userId);
        return ApiResponse.success(result);
    }

    @PatchMapping("/{matchId}")
    public ApiResponse<MatchResponse> updateMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId,
            @Valid @RequestBody MatchUpdateRequest request) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        MatchResponse response = matchService.updateMatch(hostUserId, matchId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{matchId}/finish")
    public ApiResponse<MatchResponse> finishMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        MatchResponse response = matchService.finishMatch(hostUserId, matchId);
        return ApiResponse.success(response);
    }

    /**
     * PATCH 권장. 일부 클라이언트/프록시에서 PATCH가 매핑되지 않는 경우를 위해 POST도 동일 동작으로 허용.
     */
    @RequestMapping(value = "/{matchId}/cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ApiResponse<MatchResponse> cancelMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        MatchResponse response = matchService.cancelMatch(hostUserId, matchId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{matchId}")
    public ApiResponse<MatchDetailResponse> getMatchDetail(
            @IfLogin UserPrincipal principal,
            @PathVariable Long matchId) {
        Long userId = AuthUtils.getUserIdOrNull(principal);
        MatchDetailResponse response = matchService.getMatchDetail(matchId, userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{matchId}/participants")
    public ApiResponse<ParticipantApplyResponse> applyParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId,
            @Valid @RequestBody(required = false) ParticipantApplyRequest request) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ParticipantApplyResponse response = matchParticipantService.applyParticipant(
                userId, matchId, Optional.ofNullable(request).orElseGet(ParticipantApplyRequest::new));
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{matchId}/participants/me")
    public ApiResponse<Void> cancelParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        matchParticipantService.cancelParticipant(userId, matchId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{matchId}/participants/me/accept-offer")
    public ApiResponse<ParticipantApplyResponse> acceptOffer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ParticipantApplyResponse response = matchParticipantService.acceptOffer(userId, matchId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{matchId}/participants/me/reject-offer")
    public ApiResponse<ParticipantApplyResponse> rejectOffer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        ParticipantApplyResponse response = matchParticipantService.rejectOffer(userId, matchId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{matchId}/participants/applications")
    public ApiResponse<List<ParticipantApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        List<ParticipantApplicationResponse> response = matchParticipantService.getApplications(hostUserId, matchId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{matchId}/participants/{participationId}")
    public ApiResponse<ParticipantApplyResponse> decideParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long matchId,
            @PathVariable Long participationId,
            @Valid @RequestBody ParticipantDecisionRequest request) {
        Long hostUserId = AuthUtils.getUserIdOrThrow(principal);
        ParticipantApplyResponse response = matchParticipantService.decideParticipant(
                hostUserId, matchId, participationId, request);
        return ApiResponse.success(response);
    }

    private List<String> parseRegionCodes(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return null;
        }
        return Arrays.stream(regionCode.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
