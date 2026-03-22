package org.app.mintonmatchapi.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.user.entity.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_match_reviewer_reviewee",
                columnNames = {"match_id", "reviewer_id", "reviewee_id"}
        ),
        indexes = @Index(name = "idx_reviews_reviewee_created", columnList = "reviewee_id, created_at")
)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", nullable = false, length = 20)
    private ReviewSentiment sentiment;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewHashtag> hashtags = new ArrayList<>();

    @Builder
    public Review(Match match, User reviewer, User reviewee, ReviewSentiment sentiment, int score, String detail) {
        this.match = match;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.sentiment = sentiment;
        this.score = score;
        this.detail = detail;
    }

    /**
     * 해시태그 목록을 교체한다. null이면 전부 제거.
     */
    public void replaceHashtags(Collection<ReviewHashtagCode> codes) {
        hashtags.clear();
        if (codes == null || codes.isEmpty()) {
            return;
        }
        LinkedHashSet<ReviewHashtagCode> distinct = new LinkedHashSet<>(codes);
        for (ReviewHashtagCode code : distinct) {
            hashtags.add(new ReviewHashtag(this, code));
        }
    }
}
