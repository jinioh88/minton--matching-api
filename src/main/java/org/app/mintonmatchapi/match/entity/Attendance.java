package org.app.mintonmatchapi.match.entity;

/**
 * 매칭 출석/지각/노쇼 상태
 */
public enum Attendance {
    UNDECIDED,  // 미정
    ATTENDED,   // 참석
    LATE,       // 지각
    NOSHOW      // 노쇼
}
