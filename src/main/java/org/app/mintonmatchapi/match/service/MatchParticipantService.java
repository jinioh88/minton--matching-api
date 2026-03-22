package org.app.mintonmatchapi.match.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.common.util.StringUtils;
import org.app.mintonmatchapi.match.dto.ParticipantApplicationResponse;
import org.app.mintonmatchapi.match.dto.ParticipantApplyRequest;
import org.app.mintonmatchapi.match.dto.ParticipantApplyResponse;
import org.app.mintonmatchapi.match.config.QueueProperties;
import org.app.mintonmatchapi.match.dto.ParticipantDecisionRequest;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.match.event.ParticipantCancelledEvent;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.policy.ParticipationEligibility;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.PENDING;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.RESERVED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.WAITING;

@Service
public class MatchParticipantService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final QueueProperties queueProperties;

    public MatchParticipantService(MatchRepository matchRepository,
                                  MatchParticipantRepository matchParticipantRepository,
                                  UserRepository userRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  QueueProperties queueProperties) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.queueProperties = queueProperties;
    }

    @Transactional
    public ParticipantApplyResponse applyParticipant(Long currentUserId, Long matchId, ParticipantApplyRequest request) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (match.getStatus() != MatchStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.MATCH_NOT_RECRUITING);
        }

        if (match.getHost().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.HOST_CANNOT_APPLY);
        }

        List<MatchParticipant> existingList = matchParticipantRepository.findByMatchIdAndUserIdAll(matchId, currentUserId);
        MatchParticipant existingReusable = existingList.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.CANCELLED || p.getStatus() == ParticipantStatus.REJECTED)
                .findFirst()
                .orElse(null);
        boolean hasActiveParticipation = existingList.stream()
                .anyMatch(p -> p.getStatus().isActiveParticipation());

        if (hasActiveParticipation) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        ParticipationEligibility.assertMayApplyToMatch(user);

        long acceptedCount = matchParticipantRepository.countByMatchIdAndStatus(matchId, ACCEPTED);
        String applyMessage = StringUtils.trimOrNull(request != null ? request.getApplyMessage() : null);

        ParticipantStatus status;
        int queueOrder;

        if (!match.isFull(acceptedCount)) {
            status = PENDING;
            queueOrder = 0;
        } else {
            status = WAITING;
            queueOrder = matchParticipantRepository.findMaxQueueOrderByMatchId(matchId) + 1;
        }

        MatchParticipant saved;
        if (existingReusable != null) {
            existingReusable.reapply(status, queueOrder, applyMessage);
            saved = matchParticipantRepository.save(existingReusable);
        } else {
            MatchParticipant participant = MatchParticipant.builder()
                    .match(match)
                    .user(user)
                    .status(status)
                    .queueOrder(queueOrder)
                    .applyMessage(applyMessage)
                    .build();
            saved = matchParticipantRepository.save(participant);
        }
        return ParticipantApplyResponse.from(saved);
    }

    @Transactional
    public ParticipantApplyResponse decideParticipant(Long hostUserId, Long matchId, Long participationId,
                                                      ParticipantDecisionRequest request) {
        MatchParticipant participant = matchParticipantRepository.findByIdAndMatch_IdWithMatch(participationId, matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        Match match = participant.getMatch();
        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 수락/거절/추방할 수 있습니다.");
        }

        ParticipantStatus status = participant.getStatus();
        ParticipantDecisionRequest.ParticipantDecisionAction action = request.getAction();

        if (action == ParticipantDecisionRequest.ParticipantDecisionAction.KICK) {
            if (status != ACCEPTED) {
                throw new BusinessException(ErrorCode.INVALID_STATUS, "확정된 참여자만 추방할 수 있습니다.");
            }
            participant.changeToCancelled();
            matchParticipantRepository.save(participant);
            eventPublisher.publishEvent(new ParticipantCancelledEvent(this, matchId, true, false));
            return ParticipantApplyResponse.from(participant);
        }

        if (status != PENDING && status != WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        if (action == ParticipantDecisionRequest.ParticipantDecisionAction.ACCEPT) {
            long acceptedCount = matchParticipantRepository.countByMatchIdAndStatus(matchId, ACCEPTED);
            if (match.isFull(acceptedCount)) {
                throw new BusinessException(ErrorCode.MATCH_FULL);
            }
            participant.changeToAccepted();
        } else {
            participant.changeToRejected();
        }

        MatchParticipant saved = matchParticipantRepository.save(participant);
        return ParticipantApplyResponse.from(saved);
    }

    @Transactional
    public void cancelParticipant(Long userId, Long matchId) {
        MatchParticipant participant = matchParticipantRepository.findActiveByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        if (!participant.getStatus().isActiveParticipation()) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL);
        }

        boolean wasAccepted = (participant.getStatus() == ACCEPTED);
        participant.changeToCancelled();
        matchParticipantRepository.save(participant);

        if (wasAccepted) {
            eventPublisher.publishEvent(new ParticipantCancelledEvent(this, matchId, true, false));
        }
    }

    @Transactional
    public ParticipantApplyResponse acceptOffer(Long userId, Long matchId) {
        MatchParticipant participant = matchParticipantRepository.findActiveByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        Match match = matchRepository.findByIdWithHostForUpdate(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        long acceptedCount = matchParticipantRepository.countByMatchIdAndStatus(matchId, ACCEPTED);
        if (match.isFull(acceptedCount)) {
            throw new BusinessException(ErrorCode.MATCH_FULL);
        }

        LocalDateTime emergencyCutoff = LocalDateTime.now().plusHours(queueProperties.getEmergencyThresholdHours());
        if (!participant.canAcceptOffer(emergencyCutoff)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "예약 수락 가능한 상태가 아닙니다.");
        }
        if (participant.isOfferExpired()) {
            throw new BusinessException(ErrorCode.OFFER_EXPIRED);
        }

        participant.changeToAccepted();
        MatchParticipant saved = matchParticipantRepository.save(participant);
        return ParticipantApplyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ParticipantApplicationResponse> getApplications(Long hostUserId, Long matchId) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 신청 목록을 조회할 수 있습니다.");
        }

        List<MatchParticipant> participants = matchParticipantRepository.findByMatch_IdAndStatusInOrderByStatusAndQueueOrder(
                matchId, List.of(PENDING, WAITING, RESERVED));

        return participants.stream()
                .map(ParticipantApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ParticipantApplyResponse rejectOffer(Long userId, Long matchId) {
        MatchParticipant participant = matchParticipantRepository.findActiveByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        if (participant.getStatus() != RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "예약 거절 가능한 상태가 아닙니다.");
        }

        participant.changeToCancelled();
        MatchParticipant saved = matchParticipantRepository.save(participant);

        eventPublisher.publishEvent(new ParticipantCancelledEvent(this, matchId, false, true));

        return ParticipantApplyResponse.from(saved);
    }
}
