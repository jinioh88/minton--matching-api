package org.app.mintonmatchapi.user.policy;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.user.entity.AccountStatus;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;

/**
 * 참여 신청(매칭 참가) 가능 여부. 방장 개설 등 다른 쓰기는 별도 정책.
 */
public final class ParticipationEligibility {

    private ParticipationEligibility() {
    }

    public static void assertMayApplyToMatch(User user) {
        assertNotBannedOrSuspendedForWrites(user);
        LocalDateTime now = LocalDateTime.now();
        if (user.getParticipationBannedUntil() != null && user.getParticipationBannedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.USER_PARTICIPATION_BANNED, "패널티 누적으로 참여가 제한되었습니다.");
        }
    }

    /**
     * 참여 신청 전용 제한(participation_banned)과 별개로, 정지·영구 정지 시 일반 쓰기(후기 등)도 막는다.
     */
    public static void assertNotBannedOrSuspendedForWrites(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getAccountStatus() == AccountStatus.BANNED) {
            throw new BusinessException(ErrorCode.USER_BANNED, "이용이 제한된 계정입니다.");
        }
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED, "계정 정지 기간 중에는 이 작업을 할 수 없습니다.");
        }
    }
}
