package org.app.mintonmatchapi.match.event;

/**
 * 매칭이 {@code FINISHED} 또는 {@code CANCELLED}로 확정된 뒤(원 트랜잭션 커밋 이후),
 * 채팅방이 있으면 시스템 안내 메시지를 남기고 STOMP 로 브로드캐스트한다.
 */
public record MatchChatClosedEvent(long matchId) {
}
