package org.app.mintonmatchapi.match.entity;

/**
 * 매칭 비용 분담 방식
 */
public enum CostPolicy {
    SPLIT_EQUAL,   // 균등 분담
    HOST_PAYS,     // 방장 부담
    GUEST_PAYS     // 참가자 부담
}
