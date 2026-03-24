package org.app.mintonmatchapi.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 메시지 목록.
 * <ul>
 *   <li>{@code cursor} 없음: 최신 {@code size}개(시간 오름차순 정렬로 반환).</li>
 *   <li>{@code cursor}=messageId: 그보다 <strong>오래된</strong> 메시지 다음 페이지(동일 정렬).</li>
 *   <li>{@code afterId}: 폴링용. 해당 id보다 <strong>새로운</strong> 메시지만 오름차순, {@code nextCursor}는 항상 null.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class ChatMessagePageResponse {

    private final List<ChatMessageResponse> messages;
    /**
     * 더 불러올 과거 메시지가 있으면, 다음 요청의 {@code cursor}로 넣을 값(본 응답 중 가장 작은 messageId).
     */
    private final Long nextCursor;
}
