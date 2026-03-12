package org.app.mintonmatchapi.match.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.app.mintonmatchapi.match.dto.MatchSearchCondition;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.entity.QMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class MatchRepositoryImpl implements MatchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MatchRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
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
}
