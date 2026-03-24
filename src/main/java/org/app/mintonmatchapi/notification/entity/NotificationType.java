package org.app.mintonmatchapi.notification.entity;

/**
 * 인앱 알림 유형 (Epic 6 / Sprint 5).
 */
public enum NotificationType {
    /** 참여 신청·재신청 시 방장에게 */
    MATCH_APPLICATION,
    /** 방장 수락(PENDING/WAITING → ACCEPTED) 시 신청자에게 */
    PARTICIPATION_ACCEPTED,
    /** 방장 거절 시 신청자에게 */
    PARTICIPATION_REJECTED,
    /** 대기열 순차 승격으로 RESERVED 부여 시 해당 사용자에게 */
    WAITLIST_SLOT_OFFER,
    /** 긴급 모드로 대기 전체에 안내 */
    WAITLIST_EMERGENCY_OPEN,
    /**
     * 방장이 매칭을 취소(CANCELLED)한 경우.
     * 수신 범위: 해당 매칭의 <strong>ACCEPTED</strong> 참가자만. <strong>방장 본인은 제외</strong>(본인이 취소 주체).
     */
    MATCH_CANCELLED
}
