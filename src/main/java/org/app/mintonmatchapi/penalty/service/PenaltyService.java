package org.app.mintonmatchapi.penalty.service;

import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.app.mintonmatchapi.penalty.config.SanctionProperties;
import org.app.mintonmatchapi.penalty.dto.PenaltyGrantRequest;
import org.app.mintonmatchapi.penalty.dto.PenaltyGrantResponse;
import org.app.mintonmatchapi.penalty.dto.PenaltyListItemResponse;
import org.app.mintonmatchapi.penalty.entity.Penalty;
import org.app.mintonmatchapi.penalty.repository.PenaltyRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;

@Slf4j
@Service
public class PenaltyService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PenaltyRepository penaltyRepository;
    private final UserRepository userRepository;
    private final SanctionProperties sanctionProperties;

    public PenaltyService(MatchRepository matchRepository,
                          MatchParticipantRepository matchParticipantRepository,
                          PenaltyRepository penaltyRepository,
                          UserRepository userRepository,
                          SanctionProperties sanctionProperties) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.penaltyRepository = penaltyRepository;
        this.userRepository = userRepository;
        this.sanctionProperties = sanctionProperties;
    }

    @Transactional
    public PenaltyGrantResponse grantPenalty(Long hostUserId, Long matchId, PenaltyGrantRequest request) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (match.getStatus() != MatchStatus.FINISHED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS, "종료된 매칭에서만 패널티를 부여할 수 있습니다.");
        }

        if (!match.getHost().getId().equals(hostUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "방장만 패널티를 부여할 수 있습니다.");
        }

        Long targetUserId = request.getUserId();
        if (targetUserId.equals(hostUserId)) {
            throw new BusinessException(ErrorCode.INVALID_PENALTY_TARGET, "방장 본인에게는 패널티를 부여할 수 없습니다.");
        }

        if (!matchParticipantRepository.existsByMatch_IdAndUser_IdAndStatus(matchId, targetUserId, ACCEPTED)) {
            throw new BusinessException(ErrorCode.INVALID_PENALTY_TARGET, "해당 매칭의 확정 참여자에게만 패널티를 부여할 수 있습니다.");
        }

        if (penaltyRepository.existsByMatch_IdAndPenalizedUser_IdAndType(matchId, targetUserId, request.getType())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PENALTY);
        }

        User host = match.getHost();
        User penalizedLocked = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Penalty penalty = Penalty.builder()
                .match(match)
                .host(host)
                .penalizedUser(penalizedLocked)
                .type(request.getType())
                .build();

        try {
            penaltyRepository.save(penalty);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_PENALTY, ErrorCode.DUPLICATE_PENALTY.getMessage());
        }

        int weight = sanctionProperties.penaltyWeight(request.getType());
        penalizedLocked.recordPenaltyGrant(weight);

        applyGraduatedSanctions(penalizedLocked, LocalDateTime.now());

        return PenaltyGrantResponse.from(penalty);
    }

    /**
     * 대상 유저가 받은 패널티 이력(최신순). {@link org.app.mintonmatchapi.user.dto.ProfileResponse#getPenaltyCount()}와
     * 동일 기준(해당 유저를 penalized_user로 하는 행 전체)이며, 본 API의 {@code Page#getTotalElements()}와 일치한다.
     */
    @Transactional(readOnly = true)
    public Page<PenaltyListItemResponse> listPenaltiesForUser(Long penalizedUserId, Pageable pageable) {
        userRepository.findById(penalizedUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return penaltyRepository.findPageWithRelationsByPenalizedUserId(penalizedUserId, pageable)
                .map(PenaltyListItemResponse::from);
    }

    /**
     * 누적 {@code penaltyCount} 기준 단계별 제재. 기존 until 시각보다 짧게는 덮어쓰지 않는다.
     */
    private void applyGraduatedSanctions(User user, LocalDateTime now) {
        int strikes = user.getPenaltyCount() != null ? user.getPenaltyCount() : 0;
        SanctionProperties.StrikeThresholds t = sanctionProperties.getStrikeThresholds();
        SanctionProperties.Durations d = sanctionProperties.getDurations();

        if (strikes >= t.getPermanentBan()) {
            user.markBanned();
            log.warn("영구 제재: userId={}, penaltyCount={}", user.getId(), strikes);
            return;
        }

        if (strikes >= t.getFullSuspension()) {
            LocalDateTime until = now.plusDays(d.getSuspensionDays());
            user.mergeSuspendedUntil(until);
            user.markSuspendedIfNotBanned();
        }

        if (strikes >= t.getParticipationBan()) {
            LocalDateTime until = now.plusDays(d.getParticipationBanDays());
            user.mergeParticipationBannedUntil(until);
        }
    }
}
