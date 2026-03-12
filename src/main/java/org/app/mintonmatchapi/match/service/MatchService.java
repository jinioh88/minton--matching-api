package org.app.mintonmatchapi.match.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.match.dto.*;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.WAITING;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;

    public MatchService(MatchRepository matchRepository, MatchParticipantRepository matchParticipantRepository,
                       UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MatchResponse createMatch(Long hostId, MatchCreateRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        validateMatchCreate(request);

        Match match = Match.builder()
                .host(host)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .matchDate(request.getMatchDate())
                .startTime(request.getStartTime())
                .durationMin(request.getDurationMin())
                .locationName(trimOrNull(request.getLocationName()))
                .locationAddress(trimOrNull(request.getLocationAddress()))
                .regionCode(request.getRegionCode().trim())
                .maxPeople(request.getMaxPeople())
                .targetLevels(trimOrNull(request.getTargetLevels()))
                .costPolicy(request.getCostPolicy())
                .status(MatchStatus.RECRUITING)
                .imageUrl(trimOrNull(request.getImageUrl()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        Match saved = matchRepository.save(match);
        return MatchResponse.from(saved);
    }

    private void validateMatchCreate(MatchCreateRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getMatchDate().isBefore(today)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "경기 날짜는 오늘 이후여야 합니다.");
        }

        if (request.getMatchDate().equals(today)) {
            LocalTime now = LocalTime.now();
            if (request.getStartTime().isBefore(now) || request.getStartTime().equals(now)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "시작 시간은 현재 시간 이후여야 합니다.");
            }
        }
    }

    public MatchDetailResponse getMatchDetail(Long matchId) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매칭을 찾을 수 없습니다."));

        List<MatchParticipant> acceptedAndWaiting = matchParticipantRepository.findByMatchIdAndStatusInWithUserOrderByQueueOrderAsc(matchId, List.of(ACCEPTED, WAITING));
        List<MatchParticipant> accepted = acceptedAndWaiting.stream().filter(p -> p.getStatus() == ACCEPTED).toList();
        List<MatchParticipant> waiting = acceptedAndWaiting.stream().filter(p -> p.getStatus() == WAITING).toList();

        return MatchDetailResponse.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .title(match.getTitle())
                .description(match.getDescription())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .durationMin(match.getDurationMin())
                .locationName(match.getLocationName())
                .locationAddress(match.getLocationAddress())
                .regionCode(match.getRegionCode())
                .maxPeople(match.getMaxPeople())
                .currentPeople(accepted.size())
                .targetLevels(match.getTargetLevels())
                .costPolicy(match.getCostPolicy())
                .status(match.getStatus())
                .imageUrl(match.getImageUrl())
                .latitude(match.getLatitude())
                .longitude(match.getLongitude())
                .createdAt(match.getCreatedAt())
                .host(HostSummary.from(match.getHost()))
                .confirmedParticipants(accepted.stream().map(ParticipantSummary::from).collect(Collectors.toList()))
                .waitingList(waiting.stream().map(ParticipantSummary::from).collect(Collectors.toList()))
                .waitingCount(waiting.size())
                .build();
    }

    public Page<MatchListResponse> getMatchList(MatchSearchCondition condition, Long userId) {
        List<String> regionCodes = resolveRegionCodes(condition.getRegionCodes(), userId);
        MatchSearchCondition resolvedCondition = MatchSearchCondition.builder()
                .regionCodes(regionCodes)
                .dateFrom(condition.getDateFrom())
                .dateTo(condition.getDateTo())
                .level(condition.getLevel())
                .pageable(condition.getPageable())
                .build();

        Page<Match> matches = matchRepository.searchMatches(resolvedCondition);

        if (matches.isEmpty()) {
            return matches.map(m -> MatchListResponse.of(m, 0));
        }

        List<Long> matchIds = matches.getContent().stream()
                .map(Match::getId)
                .toList();

        List<Object[]> countResults = matchParticipantRepository.countByMatchIdsAndStatus(matchIds, ParticipantStatus.ACCEPTED);
        Map<Long, Integer> currentPeopleMap = countResults.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        return matches.map(m -> MatchListResponse.of(m, currentPeopleMap.getOrDefault(m.getId(), 0)));
    }

    /**
     * regionCodes 우선순위: 1) 요청 파라미터 2) 로그인 사용자 interestLoc1/2
     */
    private List<String> resolveRegionCodes(List<String> requestRegionCodes, Long userId) {
        if (requestRegionCodes != null && !requestRegionCodes.isEmpty()) {
            return requestRegionCodes;
        }
        if (userId != null) {
            return userRepository.findById(userId)
                    .map(user -> java.util.stream.Stream.of(user.getInterestLoc1(), user.getInterestLoc2())
                            .filter(java.util.Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .toList())
                    .filter(list -> !list.isEmpty())
                    .orElse(null);
        }
        return null;
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
