package org.app.mintonmatchapi.match.service;

import org.app.mintonmatchapi.match.config.QueueProperties;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.RESERVED;
import static org.app.mintonmatchapi.match.entity.ParticipantStatus.WAITING;

@Service
public class QueuePromotionService {

    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchRepository matchRepository;
    private final QueueProperties queueProperties;

    public QueuePromotionService(MatchParticipantRepository matchParticipantRepository,
                                 MatchRepository matchRepository,
                                 QueueProperties queueProperties) {
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchRepository = matchRepository;
        this.queueProperties = queueProperties;
    }

    /**
     * ACCEPTED 취소 시 대기열 승격 처리 (순차 기회 또는 긴급 선착순)
     */
    @Transactional
    public void promoteOnCancelled(Long matchId) {
        Match match = matchRepository.findByIdWithHost(matchId).orElse(null);
        if (match == null) {
            return;
        }

        long acceptedCount = matchParticipantRepository.countByMatchIdAndStatus(matchId, ACCEPTED);
        if (acceptedCount >= match.getMaxPeople()) {
            return; // 정원이 이미 찼으면 승격 불필요
        }

        boolean isEmergency = isEmergencyMode(match);
        if (isEmergency) {
            promoteEmergency(matchId, match);
        } else {
            promoteSequential(matchId, match);
        }
    }

    private boolean isEmergencyMode(Match match) {
        LocalDateTime cutoff = LocalDateTime.now().plusHours(queueProperties.getEmergencyThresholdHours());
        return match.isWithinEmergencyThreshold(cutoff);
    }

    private void promoteSequential(Long matchId, Match match) {
        List<MatchParticipant> reserved = matchParticipantRepository.findByMatch_IdAndStatus(matchId, RESERVED);
        if (!reserved.isEmpty()) {
            return; // 이미 예약 중인 사람 있으면 대기
        }

        List<MatchParticipant> waiting = matchParticipantRepository.findByMatch_IdAndStatusOrderByQueueOrderAscWithLock(
                matchId, WAITING, PageRequest.of(0, 1));
        if (waiting.isEmpty()) {
            return;
        }

        MatchParticipant first = waiting.get(0);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(queueProperties.getOfferTimeoutMinutes());
        first.changeToReserved(expiresAt);
        matchParticipantRepository.save(first);
        // TODO Sprint 5: 알림 발송 "참석 기회가 생겼습니다! 15분 내에 수락해 주세요."
    }

    private void promoteEmergency(Long matchId, Match match) {
        // 긴급 모드: RESERVED 없이, accept-offer 호출 시 선착순으로 처리
        // 여기서는 별도 승격 없음 - WAITING 전체가 accept-offer 호출 가능
        // TODO Sprint 5: WAITING 전체에게 동시 알림
    }

    /**
     * 타임아웃된 RESERVED 처리 (스케줄러에서 호출)
     */
    @Transactional
    public void processExpiredReservations() {
        List<MatchParticipant> expired = matchParticipantRepository.findByStatusAndOfferExpiresAtBefore(
                RESERVED, LocalDateTime.now());

        Map<Long, Match> matchCache = new HashMap<>();
        for (MatchParticipant participant : expired) {
            Long matchId = participant.getMatch().getId();
            participant.changeToCancelled();
            matchParticipantRepository.save(participant);
            matchCache.computeIfAbsent(matchId, id -> matchRepository.findByIdWithHost(id).orElse(null));
        }

        for (Map.Entry<Long, Match> entry : matchCache.entrySet()) {
            Match match = entry.getValue();
            if (match != null) {
                promoteSequential(entry.getKey(), match);
            }
        }
    }
}
