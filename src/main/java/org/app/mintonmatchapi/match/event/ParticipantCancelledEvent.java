package org.app.mintonmatchapi.match.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ParticipantCancelledEvent extends ApplicationEvent {

    private final Long matchId;
    private final boolean wasAccepted;
    private final boolean wasReserved;

    /**
     * @param source 이벤트 발행 주체
     * @param matchId 취소된 참여의 매칭 ID
     * @param wasAccepted 취소 전 ACCEPTED 상태였는지
     * @param wasReserved 취소 전 RESERVED 상태였는지 (예약 거절 시)
     */
    public ParticipantCancelledEvent(Object source, Long matchId, boolean wasAccepted, boolean wasReserved) {
        super(source);
        this.matchId = matchId;
        this.wasAccepted = wasAccepted;
        this.wasReserved = wasReserved;
    }

    /** 대기열 승격이 필요한지 (ACCEPTED 취소 또는 RESERVED 거절) */
    public boolean requiresQueuePromotion() {
        return wasAccepted || wasReserved;
    }
}
