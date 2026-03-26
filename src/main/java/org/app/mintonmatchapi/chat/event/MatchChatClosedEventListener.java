package org.app.mintonmatchapi.chat.event;

import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.chat.service.ChatService;
import org.app.mintonmatchapi.match.event.MatchChatClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 종료·취소 커밋 이후 채팅방에 SYSTEM 안내 1건 저장 및 {@code /topic/chat.{roomId}} 발행.
 */
@Slf4j
@Component
public class MatchChatClosedEventListener {

    private final ChatService chatService;

    public MatchChatClosedEventListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMatchChatClosed(MatchChatClosedEvent event) {
        try {
            chatService.publishMatchTerminalSystemMessageAndBroadcast(event.matchId());
        } catch (Exception e) {
            log.warn("매칭 종료 채팅 안내 처리 실패 matchId={}", event.matchId(), e);
        }
    }
}
