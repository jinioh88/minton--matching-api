package org.app.mintonmatchapi.user.entity;

/**
 * 계정 제재 단계. 영구 정지 등 운영 정책과 연동.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    BANNED
}
