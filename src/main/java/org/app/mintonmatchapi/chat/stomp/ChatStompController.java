package org.app.mintonmatchapi.chat.stomp;

import jakarta.validation.Valid;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.common.exception.ErrorResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.security.Principal;

@Controller
public class ChatStompController {

    private final ChatStompService chatStompService;

    public ChatStompController(ChatStompService chatStompService) {
        this.chatStompService = chatStompService;
    }

    @MessageMapping("/chat/messages")
    public void sendMessage(@Payload @Valid ChatStompSendPayload payload, Principal principal) {
        UserPrincipal user = requireUser(principal);
        chatStompService.sendAndBroadcast(user.getUserId(), payload);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleBusiness(BusinessException e) {
        return ErrorResponse.of(e.getErrorCode(), e.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : e.getMessage();
        return ErrorResponse.of(ErrorCode.VALIDATION_ERROR, msg != null ? msg : "입력값이 올바르지 않습니다.");
    }

    private static UserPrincipal requireUser(Principal principal) {
        if (principal instanceof UserPrincipal p) {
            return p;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "STOMP 세션에 사용자 정보가 없습니다.");
    }
}
