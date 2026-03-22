package org.app.mintonmatchapi.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "review_hashtags",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_hashtag", columnNames = {"review_id", "code"}),
        indexes = @Index(name = "idx_review_hashtags_code", columnList = "code")
)
public class ReviewHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_hashtag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 30)
    private ReviewHashtagCode code;

    ReviewHashtag(Review review, ReviewHashtagCode code) {
        this.review = review;
        this.code = code;
    }
}
