package org.app.mintonmatchapi.chat.service;

import org.app.mintonmatchapi.chat.entity.ChatRoom;
import org.app.mintonmatchapi.chat.repository.ChatRoomRepository;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;

@Service
public class ChatRoomService {

    private final MatchRepository matchRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    public ChatRoomService(MatchRepository matchRepository,
                          ChatRoomRepository chatRoomRepository,
                          MatchParticipantRepository matchParticipantRepository) {
        this.matchRepository = matchRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.matchParticipantRepository = matchParticipantRepository;
    }

    /**
     * 매칭에 연결된 채팅방이 없으면 생성하고, 있으면 그대로 반환한다.
     * 첫 ACCEPTED(방장 수락·예약 수락) 직후 같은 트랜잭션에서 호출된다.
     */
    @Transactional
    public ChatRoom ensureChatRoomForMatch(Long matchId) {
        return chatRoomRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    Match match = matchRepository.findById(matchId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
                    ChatRoom room = ChatRoom.builder().match(match).build();
                    return chatRoomRepository.save(room);
                });
    }

    /**
     * 방장 또는 해당 매칭에서 ACCEPTED 인 사용자만 채팅 API에 접근할 수 있다.
     */
    @Transactional(readOnly = true)
    public void assertCanAccessChat(Long userId, Long matchId) {
        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        assertCanAccessChat(userId, match);
    }

    /**
     * 이미 {@link Match}(+host)를 로드한 호출 경로용 — 동일 요청에서 매칭을 두 번 조회하지 않도록 한다.
     */
    @Transactional(readOnly = true)
    public void assertCanAccessChat(Long userId, Match match) {
        if (match.getHost().getId().equals(userId)) {
            return;
        }
        Long matchId = match.getId();
        if (matchParticipantRepository.existsByMatch_IdAndUser_IdAndStatus(matchId, userId, ACCEPTED)) {
            return;
        }
        throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Transactional(readOnly = true)
    public ChatRoom getByIdWithMatchAndHostOrThrow(Long roomId) {
        return chatRoomRepository.findByIdWithMatchAndHost(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * roomId 기준 접근 권한 (내부에서 matchId로 host / ACCEPTED 검사).
     */
    @Transactional(readOnly = true)
    public void assertCanAccessChatByRoomId(Long userId, Long roomId) {
        ChatRoom room = getByIdWithMatchAndHostOrThrow(roomId);
        assertCanAccessChat(userId, room.getMatch());
    }

    /**
     * 메시지 전송·수정·삭제: 접근 가능 + 매칭이 종료/취소가 아닐 것.
     */
    @Transactional(readOnly = true)
    public void assertCanWriteChat(Long userId, Long roomId) {
        ChatRoom room = getByIdWithMatchAndHostOrThrow(roomId);
        assertCanAccessChat(userId, room.getMatch());
        MatchStatus status = room.getMatch().getStatus();
        if (status == MatchStatus.FINISHED || status == MatchStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS, "종료되거나 취소된 매칭에서는 메시지를 보낼 수 없습니다.");
        }
    }
}
