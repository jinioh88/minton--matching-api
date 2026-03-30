package org.app.mintonmatchapi.match.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.match.config.MatchProperties;
import org.app.mintonmatchapi.match.config.QueueProperties;
import org.app.mintonmatchapi.match.notification.PostAutoFinishNotifier;
import org.app.mintonmatchapi.notification.entity.NotificationType;
import org.app.mintonmatchapi.notification.event.NotificationDispatchCommand;
import org.app.mintonmatchapi.notification.service.NotificationService;
import org.app.mintonmatchapi.match.dto.*;
import org.app.mintonmatchapi.match.event.MatchChatClosedEvent;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.friendship.service.FriendActivityNotificationService;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.app.mintonmatchapi.review.service.ReviewService;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.ObjectProvider;
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
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.RESERVED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.WAITING;

@Slf4j
@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final QueueProperties queueProperties;
    private final MatchProperties matchProperties;
    private final ObjectProvider<PostAutoFinishNotifier> postAutoFinishNotifier;
    private final ReviewService reviewService;
    private final NotificationService notificationService;
    private final FriendActivityNotificationService friendActivityNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(MatchRepository matchRepository, MatchParticipantRepository matchParticipantRepository,
                       UserRepository userRepository, QueueProperties queueProperties,
                       MatchProperties matchProperties,
                       ObjectProvider<PostAutoFinishNotifier> postAutoFinishNotifier,
                       ReviewService reviewService,
                       NotificationService notificationService,
                       FriendActivityNotificationService friendActivityNotificationService,
                       ApplicationEventPublisher eventPublisher) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
        this.queueProperties = queueProperties;
        this.matchProperties = matchProperties;
        this.postAutoFinishNotifier = postAutoFinishNotifier;
        this.reviewService = reviewService;
        this.notificationService = notificationService;
        this.friendActivityNotificationService = friendActivityNotificationService;
        this.eventPublisher = eventPublisher;
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
        friendActivityNotificationService.publishNewMatchCreatedToFollowers(saved, host);
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
                .canFinishMatch(computeCanFinishMatch(match, userId))
                .reviewPendingUserIds(reviewService.listPendingRevieweeIds(match, userId, accepted))
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

    private boolean computeCanFinishMatch(Match match, Long userId) {
        if (userId == null || !match.getHost().getId().equals(userId)) {
            return false;
        }
        if (match.getStatus() != MatchStatus.CLOSED) {
            return false;
        }
        if (matchProperties.isRequirePastStartForManualFinish()) {
            LocalDateTime matchStart = match.getMatchDate().atTime(match.getStartTime());
            if (matchStart.isAfter(LocalDateTime.now())) {
                return false;
            }
        }
        return true;
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
        return toMatchListResponsePage(matches);
    }

    /**
     * 마이페이지 — 내가 방장인 매칭 목록 (Sprint7 Step2). 경기일 최신순.
     */
    @Transactional(readOnly = true)
    public Page<MatchListResponse> getMyHostedMatches(Long userId, MatchStatus status,
                                                      LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Page<Match> matches = matchRepository.searchHostedByUser(userId, status, dateFrom, dateTo, pageable);
        return toMatchListResponsePage(matches);
    }

    /**
     * 마이페이지 — 확정(ACCEPTED) 참가 매칭 목록 (Sprint7 Step2). 경기일 최신순.
     */
    @Transactional(readOnly = true)
    public Page<MatchListResponse> getMyParticipatedMatches(Long userId, MatchStatus status,
                                                            LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Page<Match> matches = matchRepository.searchParticipatedByUser(userId, status, dateFrom, dateTo, pageable);
        return toMatchListResponsePage(matches);
    }

    private Page<MatchListResponse> toMatchListResponsePage(Page<Match> matches) {
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

        if (request.getStatus() != null && request.getStatus() != MatchStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "status는 모집 마감 시 CLOSED만 지정할 수 있습니다.");
        }
        boolean wantsClose = request.getStatus() == MatchStatus.CLOSED;

        if (match.getStatus() != MatchStatus.RECRUITING) {
            if (wantsClose && match.getStatus() == MatchStatus.CLOSED) {
                return MatchResponse.from(match);
            }
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

        if (wantsClose) {
            match.markClosed();
        }

        Match saved = matchRepository.save(match);
        return MatchResponse.from(saved);
    }

    /**
     * 방장이 매칭을 수동 종료한다. 전제: CLOSED, (선택) 경기 시작 시각 경과.
     */
    @Transactional
    public MatchResponse finishMatch(Long hostUserId, Long matchId) {
        Match match = matchRepository.findByIdWithHostForUpdate(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 매칭을 종료할 수 있습니다.");
        }

        if (match.getStatus() != MatchStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS,
                    "모집 마감(CLOSED) 상태의 매칭만 종료할 수 있습니다. 현재 상태: " + match.getStatus());
        }

        if (matchProperties.isRequirePastStartForManualFinish()) {
            LocalDateTime matchStart = match.getMatchDate().atTime(match.getStartTime());
            if (matchStart.isAfter(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS,
                        "경기 시작 시각 이후에만 수동 종료할 수 있습니다.");
            }
        }

        match.markFinished();
        Match finished = matchRepository.save(match);
        eventPublisher.publishEvent(new MatchChatClosedEvent(matchId));
        return MatchResponse.from(finished);
    }

    /**
     * 방장이 매칭을 취소한다. RECRUITING·CLOSED만 가능. FINISHED·이미 CANCELLED는 불가(취소된 건 멱등 응답).
     */
    @Transactional
    public MatchResponse cancelMatch(Long hostUserId, Long matchId) {
        Match match = matchRepository.findByIdWithHostForUpdate(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 매칭을 취소할 수 있습니다.");
        }

        if (match.getStatus() == MatchStatus.CANCELLED) {
            return MatchResponse.from(match);
        }
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS,
                    "종료된 매칭은 취소할 수 없습니다.");
        }
        if (match.getStatus() != MatchStatus.RECRUITING && match.getStatus() != MatchStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS,
                    "취소할 수 없는 상태입니다: " + match.getStatus());
        }

        match.markCancelled();
        Match saved = matchRepository.save(match);
        notifyAcceptedParticipantsOnMatchCancelled(saved);
        eventPublisher.publishEvent(new MatchChatClosedEvent(matchId));
        return MatchResponse.from(saved);
    }

    /**
     * B6-7: ACCEPTED 확정자에게만 알림. 방장 본인은 제외.
     */
    private void notifyAcceptedParticipantsOnMatchCancelled(Match match) {
        Long hostId = match.getHost().getId();
        Long matchId = match.getId();
        String matchTitle = NotificationService.truncateTitle(match.getTitle());
        List<MatchParticipant> accepted = matchParticipantRepository.findByMatch_IdAndStatus(matchId, ACCEPTED);
        for (MatchParticipant p : accepted) {
            Long uid = p.getUser().getId();
            if (uid.equals(hostId)) {
                continue;
            }
            notificationService.publishAfterCommit(NotificationDispatchCommand.of(
                    uid,
                    NotificationType.MATCH_CANCELLED,
                    "모임이 취소되었습니다",
                    String.format("방장이 '%s' 매칭을 취소했습니다.", matchTitle),
                    matchId,
                    p.getId()));
        }
    }

    /**
     * CLOSED 매칭 중 경기 시작 후 {@link MatchProperties#getAutoFinishAfterStartHours()}시간이 지난 건을 FINISHED로 전환한다.
     */
    @Transactional
    public long autoFinishMatches() {
        int hours = matchProperties.getAutoFinishAfterStartHours();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<Long> ids = matchRepository.findClosedMatchIdsStartedOnOrBefore(cutoff);
        if (ids.isEmpty()) {
            log.debug("자동 종료 대상 없음 (cutoff={})", cutoff);
            return 0L;
        }
        long updated = matchRepository.bulkMarkFinishedByIds(ids);
        log.info("매칭 자동 종료: {}건, matchIds={}, cutoff={}", updated, ids, cutoff);
        for (Long id : ids) {
            eventPublisher.publishEvent(new MatchChatClosedEvent(id));
        }
        postAutoFinishNotifier.ifAvailable(n -> n.onMatchesAutoFinished(ids));
        return updated;
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
