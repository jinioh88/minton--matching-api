package org.app.mintonmatchapi.match.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.entity.QMatch;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class MatchRepositoryImpl implements MatchRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public MatchRepositoryImpl(JPAQueryFactory queryFactory, EntityManager entityManager) {
        this.queryFactory = queryFactory;
        this.entityManager = entityManager;
    }

    @Override
    public Page<Match> searchMatches(MatchSearchCondition condition) {
        QMatch match = QMatch.match;
        BooleanExpression[] searchConditions = searchConditions(condition, match);

        JPAQuery<Match> query = queryFactory
                .selectFrom(match)
                .leftJoin(match.host).fetchJoin()
                .where(searchConditions)
                .orderBy(match.matchDate.asc(), match.startTime.asc());

        Pageable pageable = condition.getPageable();
        List<Match> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(match.count())
                .from(match)
                .where(searchConditions);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression[] searchConditions(MatchSearchCondition condition, QMatch match) {
        return new BooleanExpression[]{
                regionCodesIn(condition.getRegionCodes()),
                dateBetween(condition.getDateFrom(), condition.getDateTo()),
                levelContains(condition.getLevel()),
                statusRecruitingOrClosed(match)
        };
    }

    private BooleanExpression regionCodesIn(List<String> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return null;
        }
        return QMatch.match.regionCode.in(regionCodes);
    }

    private BooleanExpression dateBetween(java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return null;
        }
        if (dateFrom != null && dateTo != null) {
            return QMatch.match.matchDate.between(dateFrom, dateTo);
        }
        if (dateFrom != null) {
            return QMatch.match.matchDate.goe(dateFrom);
        }
        return QMatch.match.matchDate.loe(dateTo);
    }

    private BooleanExpression levelContains(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        return QMatch.match.targetLevels.contains(level);
    }

    private BooleanExpression statusRecruitingOrClosed(QMatch match) {
        return match.status.in(MatchStatus.RECRUITING, MatchStatus.CLOSED);
    }

    @Override
    public List<Long> findClosedMatchIdsStartedOnOrBefore(LocalDateTime cutoff) {
        QMatch match = QMatch.match;
        return queryFactory
                .select(match.id)
                .from(match)
                .where(
                        match.status.eq(MatchStatus.CLOSED),
                        matchStartOnOrBefore(match, cutoff))
                .fetch();
    }

    @Override
    public long bulkMarkFinishedByIds(List<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return 0L;
        }
        QMatch match = QMatch.match;
        LocalDateTime now = LocalDateTime.now();
        long updated = queryFactory
                .update(match)
                .set(match.status, MatchStatus.FINISHED)
                .set(match.updatedAt, now)
                .where(match.id.in(matchIds))
                .execute();
        // 벌크 UPDATE는 영속성 컨텍스트를 갱신하지 않음 → 동일 TX 후속 조회 시 stale 방지
        if (updated > 0) {
            entityManager.flush();
            entityManager.clear();
        }
        return updated;
    }

    /**
     * matchDate.atTime(startTime) &lt;= cutoff 와 동일 (LocalDate/LocalTime 기준).
     */
    private BooleanExpression matchStartOnOrBefore(QMatch match, LocalDateTime cutoff) {
        LocalDate cutoffDate = cutoff.toLocalDate();
        LocalTime cutoffTime = cutoff.toLocalTime();
        return match.matchDate.lt(cutoffDate)
                .or(match.matchDate.eq(cutoffDate).and(match.startTime.loe(cutoffTime)));
    }
}
