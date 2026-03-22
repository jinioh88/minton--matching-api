package org.app.mintonmatchapi.review.repository;

import org.app.mintonmatchapi.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByReviewee_Id(Long revieweeId);

    boolean existsByMatch_IdAndReviewer_IdAndReviewee_Id(Long matchId, Long reviewerId, Long revieweeId);

    /**
     * 유저별 후기 목록: match·host·reviewer·hashtags 한 번에 로딩 (N+1 방지).
     */
    @EntityGraph(attributePaths = {"match", "match.host", "reviewer", "hashtags"})
    @Query("select r from Review r where r.reviewee.id = :revieweeId")
    Page<Review> findPageWithRelationsByRevieweeId(@Param("revieweeId") Long revieweeId, Pageable pageable);

    /**
     * 같은 매칭에서 두 유저 간 양방향 후기가 모두 존재하면 2 (유니크 제약 하에서 최대 2).
     */
    @Query("""
            select count(r) from Review r
            where r.match.id = :matchId
              and (
                (r.reviewer.id = :userA and r.reviewee.id = :userB)
                or (r.reviewer.id = :userB and r.reviewee.id = :userA)
              )
            """)
    long countBidirectionalReviewsBetweenUsersInMatch(
            @Param("matchId") Long matchId,
            @Param("userA") Long userIdA,
            @Param("userB") Long userIdB);

    List<Review> findByMatch_IdOrderByCreatedAtAsc(Long matchId);
}
