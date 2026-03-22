package org.app.mintonmatchapi.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭을 찾을 수 없습니다."),
    MATCH_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "모집 중인 매칭이 아닙니다."),
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "이미 참여 신청한 상태입니다."),
    HOST_CANNOT_APPLY(HttpStatus.BAD_REQUEST, "방장은 참여 신청할 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 신청을 찾을 수 없습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "이미 수락 또는 거절된 신청입니다."),
    MATCH_FULL(HttpStatus.BAD_REQUEST, "정원이 가득 찼습니다."),
    CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "취소할 수 없는 상태입니다."),
    OFFER_EXPIRED(HttpStatus.BAD_REQUEST, "예약 기회가 만료되었습니다."),
    OAUTH_INVALID(HttpStatus.BAD_REQUEST, "OAuth 인증에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
 
 
