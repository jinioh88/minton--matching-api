package org.app.mintonmatchapi.match.controller;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.auth.annotation.IfLogin;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.match.dto.MatchCreateRequest;
import org.app.mintonmatchapi.match.dto.MatchDetailResponse;
import org.app.mintonmatchapi.match.dto.MatchListResponse;
import org.app.mintonmatchapi.match.dto.MatchResponse;
import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
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
        Long userId = Optional.ofNullable(principal).map(UserPrincipal::getUserId).orElse(null);

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

    @GetMapping("/{matchId}")
    public ApiResponse<MatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        MatchDetailResponse response = matchService.getMatchDetail(matchId);
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
