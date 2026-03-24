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
    INVALID_MATCH_STATUS(HttpStatus.BAD_REQUEST, "해당 작업을 수행할 수 있는 매칭 상태가 아닙니다."),
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "이미 참여 신청한 상태입니다."),
    HOST_CANNOT_APPLY(HttpStatus.BAD_REQUEST, "방장은 참여 신청할 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 신청을 찾을 수 없습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "이미 수락 또는 거절된 신청입니다."),
    MATCH_FULL(HttpStatus.BAD_REQUEST, "정원이 가득 찼습니다."),
    CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "취소할 수 없는 상태입니다."),
    OFFER_EXPIRED(HttpStatus.BAD_REQUEST, "예약 기회가 만료되었습니다."),
    OAUTH_INVALID(HttpStatus.BAD_REQUEST, "OAuth 인증에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "후기를 작성할 수 없습니다."),
    SELF_REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인에게는 후기를 작성할 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.BAD_REQUEST, "이미 해당 모임에서 이 대상에게 후기를 작성했습니다."),
    INVALID_PENALTY_TARGET(HttpStatus.BAD_REQUEST, "패널티를 부여할 수 없는 대상입니다."),
    DUPLICATE_PENALTY(HttpStatus.BAD_REQUEST, "이미 해당 유형의 패널티가 부여되었습니다."),
    USER_PARTICIPATION_BANNED(HttpStatus.BAD_REQUEST, "패널티 누적으로 참여가 제한되었습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "계정 정지 기간 중에는 이 작업을 할 수 없습니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채팅방에 접근할 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
 
 
