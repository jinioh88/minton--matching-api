package org.app.mintonmatchapi.match.entity;

/**
 * 매칭 참여 신청 상태
 *
 * 상태 전이:
 * - PENDING: 참여 신청 (정원 여유 시)
 * - WAITING: 대기 신청 (정원 초과 시), queueOrder 부여
 * - RESERVED: 참여 기회 부여됨 (예약 상태, 15분 내 수락 대기) - 대기열에서 승격 시
 * - ACCEPTED: 방장 수락 또는 예약 수락 완료
 * - REJECTED: 방장 거절 또는 본인 거절/타임아웃
 * - CANCELLED: 본인 취소 (이력 보존)
 */
public enum ParticipantStatus {
    PENDING,   // 대기 중 (방장 미확인)
    ACCEPTED,  // 수락됨 (확정)
    REJECTED,  // 거절됨
    WAITING,   // 대기열
    RESERVED,  // 참여 기회 부여됨 (15분 내 수락 대기)
    CANCELLED;  // 본인 취소

    /**
     * 활성 참여 상태 여부 (취소 가능, 신청 중인 상태)
     * PENDING, ACCEPTED, WAITING, RESERVED일 때 true
     */
    public boolean isActiveParticipation() {
        return this == PENDING || this == ACCEPTED || this == WAITING || this == RESERVED;
    }
}
