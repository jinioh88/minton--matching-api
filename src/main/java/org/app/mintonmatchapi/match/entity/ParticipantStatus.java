package org.app.mintonmatchapi.match.entity;

/**
 * 매칭 참여 신청 상태
 */
public enum ParticipantStatus {
    PENDING,   // 대기 중 (방장 미확인)
    ACCEPTED,  // 수락됨 (확정)
    REJECTED,  // 거절됨
    WAITING    // 대기열
}
