package org.app.mintonmatchapi.common.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success;
    private final String message;
    private final String code;

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .message(message != null ? message : errorCode.getMessage())
                .code(errorCode.name())
                .build();
    }
}
