package org.app.mintonmatchapi.match.entity;

/**
 * 매칭 상태
 */
public enum MatchStatus {
    RECRUITING,  // 모집 중
    CLOSED,      // 모집 마감
    FINISHED,    // 종료
    CANCELLED    // 취소
}
