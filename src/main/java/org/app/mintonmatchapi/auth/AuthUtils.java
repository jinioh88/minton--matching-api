package org.app.mintonmatchapi.auth;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;

/**
 * 인증 관련 유틸리티
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    /**
     * UserPrincipal에서 userId를 추출합니다.
     * principal이 null이면 UNAUTHORIZED 예외를 던집니다.
     *
     * @param principal 인증된 사용자 (null 가능)
     * @return userId
     * @throws BusinessException principal이 null인 경우
     */
    public static Long getUserIdOrThrow(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return principal.getUserId();
    }

    /**
     * UserPrincipal에서 userId를 추출합니다.
     * principal이 null이면 null을 반환합니다. (@IfLogin 선택적 인증용)
     *
     * @param principal 인증된 사용자 (null 가능)
     * @return userId 또는 null
     */
    public static Long getUserIdOrNull(UserPrincipal principal) {
        return principal != null ? principal.getUserId() : null;
    }
}
