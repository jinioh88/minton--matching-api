package org.app.mintonmatchapi.match.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.common.util.StringUtils;
import org.app.mintonmatchapi.match.config.QueueProperties;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.PENDING;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.RESERVED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.WAITING;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final QueueProperties queueProperties;

    public MatchService(MatchRepository matchRepository, MatchParticipantRepository matchParticipantRepository,
                       UserRepository userRepository, QueueProperties queueProperties) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
        this.queueProperties = queueProperties;
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
                .locationName(StringUtils.trimOrNull(request.getLocationName()))
                .regionCode(request.getRegionCode().trim())
                .maxPeople(request.getMaxPeople())
                .targetLevels(StringUtils.trimOrNull(request.getTargetLevels()))
                .costPolicy(request.getCostPolicy())
                .status(MatchStatus.RECRUITING)
                .imageUrl(StringUtils.trimOrNull(request.getImageUrl()))
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

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetail(Long matchId, Long userId) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        List<MatchParticipant> acceptedAndWaiting = matchParticipantRepository.findByMatchIdAndStatusInWithUserOrderByQueueOrderAsc(matchId, List.of(ACCEPTED, WAITING));
        List<MatchParticipant> accepted = acceptedAndWaiting.stream().filter(p -> p.getStatus() == ACCEPTED).toList();
        List<MatchParticipant> waiting = acceptedAndWaiting.stream().filter(p -> p.getStatus() == WAITING).toList();

        MatchDetailResponse.MatchDetailResponseBuilder builder = MatchDetailResponse.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .title(match.getTitle())
                .description(match.getDescription())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .durationMin(match.getDurationMin())
                .locationName(match.getLocationName())
                .regionCode(match.getRegionCode())
                .maxPeople(match.getMaxPeople())
                .currentPeople(accepted.size() + 1)  // 방장 포함
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
                .serverTime(Instant.now().toString())
                .isEmergencyMode(match.isWithinEmergencyThreshold(LocalDateTime.now().plusHours(queueProperties.getEmergencyThresholdHours())));

        if (userId != null) {
            MatchParticipant myParticipation = matchParticipantRepository
                    .findFirstByMatch_IdAndUser_IdOrderByIdDesc(matchId, userId)
                    .orElse(null);

            builder.myParticipation(MyParticipationSummary.from(myParticipation))
                    .canApply(resolveCanApply(match, userId, myParticipation))
                    .canCancel(resolveCanCancel(myParticipation))
                    .hasWaitingOffer(myParticipation != null && myParticipation.getStatus() == RESERVED);
        }

        return builder.build();
    }

    private Boolean resolveCanApply(Match match, Long userId, MatchParticipant myParticipation) {
        if (match.getStatus() != MatchStatus.RECRUITING) {
            return false;
        }
        if (match.getHost().getId().equals(userId)) {
            return false;
        }
        if (myParticipation != null && myParticipation.getStatus().isActiveParticipation()) {
            return false;
        }
        return true;
    }

    private Boolean resolveCanCancel(MatchParticipant myParticipation) {
        return myParticipation != null && myParticipation.getStatus().isActiveParticipation();
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
                        row -> ((Number) row[1]).intValue() + 1  // 방장 포함
                ));

        return matches.map(m -> MatchListResponse.of(m, currentPeopleMap.getOrDefault(m.getId(), 1)));
    }

    @Transactional
    public MatchResponse updateMatch(Long hostUserId, Long matchId, MatchUpdateRequest request) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 매칭을 수정할 수 있습니다.");
        }

        if (match.getStatus() != MatchStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.MATCH_NOT_RECRUITING, "모집 중인 매칭만 수정할 수 있습니다.");
        }

        if (request.getMaxPeople() != null) {
            long acceptedCount = matchParticipantRepository.countByMatchIdAndStatus(matchId, ACCEPTED);
            int currentPeople = (int) acceptedCount + 1;
            if (request.getMaxPeople() < currentPeople) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "정원을 현재 확정 인원(" + currentPeople + "명)보다 낮게 설정할 수 없습니다.");
            }
        }

        if (request.getMatchDate() != null || request.getStartTime() != null) {
            LocalDate date = request.getMatchDate() != null ? request.getMatchDate() : match.getMatchDate();
            LocalTime time = request.getStartTime() != null ? request.getStartTime() : match.getStartTime();
            validateMatchDateTime(date, time);
        }

        if (request.getTitle() != null && request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "제목은 비워둘 수 없습니다.");
        }
        if (request.getDescription() != null && request.getDescription().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "설명은 비워둘 수 없습니다.");
        }

        match.update(
                request.getTitle(),
                request.getDescription(),
                request.getMatchDate(),
                request.getStartTime(),
                request.getDurationMin(),
                request.getLocationName(),
                request.getRegionCode(),
                request.getMaxPeople(),
                request.getTargetLevels(),
                request.getCostPolicy(),
                request.getImageUrl(),
                request.getLatitude(),
                request.getLongitude()
        );

        Match saved = matchRepository.save(match);
        return MatchResponse.from(saved);
    }

    private void validateMatchDateTime(LocalDate matchDate, LocalTime startTime) {
        LocalDate today = LocalDate.now();
        if (matchDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "경기 날짜는 오늘 이후여야 합니다.");
        }
        if (matchDate.equals(today)) {
            LocalTime now = LocalTime.now();
            if (startTime.isBefore(now) || startTime.equals(now)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "시작 시간은 현재 시간 이후여야 합니다.");
            }
        }
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
}
