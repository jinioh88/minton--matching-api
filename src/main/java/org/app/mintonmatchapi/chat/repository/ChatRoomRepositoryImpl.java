package org.app.mintonmatchapi.chat.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.app.mintonmatchapi.chat.entity.ChatRoom;
import org.app.mintonmatchapi.chat.entity.QChatRoom;
import org.app.mintonmatchapi.match.entity.ParticipantStatus;
import org.app.mintonmatchapi.match.entity.QMatch;
import org.app.mintonmatchapi.match.entity.QMatchParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ChatRoomRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ChatRoom> findChatRoomsForUser(Long userId, Pageable pageable) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QMatch match = QMatch.match;
        QMatchParticipant mp = QMatchParticipant.matchParticipant;

        BooleanExpression acceptedForSameMatch = JPAExpressions
                .selectOne()
                .from(mp)
                .where(mp.match.id.eq(match.id)
                        .and(mp.user.id.eq(userId))
                        .and(mp.status.eq(ParticipantStatus.ACCEPTED)))
                .exists();

        BooleanExpression hostOrAccepted = match.host.id.eq(userId).or(acceptedForSameMatch);

        JPAQuery<ChatRoom> query = queryFactory
                .selectFrom(chatRoom)
                .innerJoin(chatRoom.match, match).fetchJoin()
                .leftJoin(match.host).fetchJoin()
                .where(hostOrAccepted)
                .orderBy(chatRoom.id.desc());

        List<ChatRoom> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .innerJoin(chatRoom.match, match)
                .where(hostOrAccepted);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
