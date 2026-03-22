package org.app.mintonmatchapi.review.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.common.util.StringUtils;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.match.entity.MatchStatus;
import org.app.mintonmatchapi.match.repository.MatchParticipantRepository;
import org.app.mintonmatchapi.match.repository.MatchRepository;
import org.app.mintonmatchapi.review.config.ReviewProperties;
import org.app.mintonmatchapi.review.dto.ReviewCreateRequest;
import org.app.mintonmatchapi.review.dto.ReviewListItemResponse;
import org.app.mintonmatchapi.review.dto.ReviewResponse;
import org.app.mintonmatchapi.review.entity.Review;
import org.app.mintonmatchapi.review.entity.ReviewHashtagCode;
import org.app.mintonmatchapi.review.repository.ReviewRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.policy.ParticipationEligibility;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.app.mintonmatchapi.match.entity.ParticipantStatus.ACCEPTED;

@Service
public class ReviewService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewProperties reviewProperties;

    public ReviewService(MatchRepository matchRepository,
                         MatchParticipantRepository matchParticipantRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         ReviewProperties reviewProperties) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewProperties = reviewProperties;
    }

    @Transactional
    public ReviewResponse createReview(Long reviewerId, Long matchId, ReviewCreateRequest request) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        ParticipationEligibility.assertNotBannedOrSuspendedForWrites(reviewer);

        Match match = matchRepository.findByIdWithHost(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        if (match.getStatus() != MatchStatus.FINISHED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED, "종료된 매칭에서만 후기를 작성할 수 있습니다.");
        }

        Long revieweeId = request.getRevieweeId();
        if (reviewerId.equals(revieweeId)) {
            throw new BusinessException(ErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        if (!isConfirmedParticipant(match, reviewerId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED, "해당 매칭의 확정 참여자만 후기를 작성할 수 있습니다.");
        }

        userRepository.findById(revieweeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!isConfirmedParticipant(match, revieweeId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED, "후기 대상은 해당 매칭의 확정 참여자여야 합니다.");
        }

        if (reviewRepository.existsByMatch_IdAndReviewer_IdAndReviewee_Id(matchId, reviewerId, revieweeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        Set<ReviewHashtagCode> hashtagCodes = resolveHashtagCodes(request.getHashtags());
        String detail = StringUtils.trimOrNull(request.getDetail());

        // 동일 피평가자에 대한 후기 작성은 users 행 비관적 락으로 직렬화된다.
        // 따라서 락을 잡은 뒤 count → save 구간에, 다른 트랜잭션이 같은 피평가자용 후기를 끼워 넣을 수 없다
        // (모든 작성 경로가 이 락을 거치는 한). 락 없는 삽입이 생기면 User.received_review_count 등
        // 사용자 단위 카운터를 락과 함께 갱신하는 방식을 검토한다.
        User revieweeLocked = userRepository.findByIdForUpdate(revieweeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long n = reviewRepository.countByReviewee_Id(revieweeId);
        // Sprint4: n=0 구간에서는 수식에 저장 평점 R을 쓰지 않고 0으로 둔다(무후기·레거시 DB값과 무관).
        float r = n == 0 ? 0f : storedRatingOrZero(revieweeLocked.getRatingScore());

        Review review = Review.builder()
                .match(match)
                .reviewer(reviewer)
                .reviewee(revieweeLocked)
                .sentiment(request.getSentiment())
                .score(request.getScore())
                .detail(detail)
                .build();
        review.replaceHashtags(hashtagCodes);

        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW, ErrorCode.DUPLICATE_REVIEW.getMessage());
        }

        float newRating = ReviewRatingCalculator.computeNewRating(
                r,
                n,
                request.getScore(),
                reviewProperties.getRatingPriorCount(),
                reviewProperties.getRatingPriorMean());
        revieweeLocked.updateRatingScore(newRating);

        return ReviewResponse.from(review);
    }

    /**
     * 피평가자 기준 받은 후기 목록. 비로그인 시 내용 항상 마스킹.
     * 로그인 시: 작성자 본인·유예 경과·(조회자↔작성자 또는 피평가자↔작성자) 상호 후기 완료 시 내용 공개.
     */
    /**
     * 매칭 상세용: 종료된 매칭에서 로그인 사용자가 확정 참여자일 때,
     * 본인을 제외한 확정 참여자 중 아직 본인이 후기를 작성하지 않은 userId 목록(방장·ACCEPTED 순).
     */
    @Transactional(readOnly = true)
    public List<Long> listPendingRevieweeIds(Match match, Long reviewerId, List<MatchParticipant> acceptedParticipants) {
        if (reviewerId == null || match.getStatus() != MatchStatus.FINISHED) {
            return List.of();
        }
        if (!isConfirmedParticipant(match, reviewerId)) {
            return List.of();
        }
        LinkedHashSet<Long> confirmed = new LinkedHashSet<>();
        confirmed.add(match.getHost().getId());
        for (MatchParticipant p : acceptedParticipants) {
            confirmed.add(p.getUser().getId());
        }
        Long matchId = match.getId();
        List<Long> out = new ArrayList<>();
        for (Long uid : confirmed) {
            if (uid.equals(reviewerId)) {
                continue;
            }
            if (reviewRepository.existsByMatch_IdAndReviewer_IdAndReviewee_Id(matchId, reviewerId, uid)) {
                continue;
            }
            out.add(uid);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Page<ReviewListItemResponse> listReceivedReviews(Long revieweeId, Long viewerUserId, Pageable pageable) {
        userRepository.findById(revieweeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Page<Review> page = reviewRepository.findPageWithRelationsByRevieweeId(revieweeId, pageable);
        return page.map(review -> ReviewListItemResponse.of(review, isContentRevealed(review, viewerUserId)));
    }

    /**
     * docs/요구사항분석.md 후기 상호 익명성·공개 시점.
     */
    private boolean isContentRevealed(Review review, Long viewerUserId) {
        if (viewerUserId == null) {
            return false;
        }
        Long reviewerId = review.getReviewer().getId();
        Long revieweeId = review.getReviewee().getId();
        Long matchId = review.getMatch().getId();

        if (Objects.equals(viewerUserId, reviewerId)) {
            return true;
        }
        if (isRevealGraceElapsed(review.getMatch())) {
            return true;
        }
        if (Objects.equals(viewerUserId, revieweeId)) {
            return mutualReviewsWrittenInMatch(matchId, reviewerId, revieweeId);
        }
        return mutualReviewsWrittenInMatch(matchId, viewerUserId, reviewerId);
    }

    private boolean mutualReviewsWrittenInMatch(Long matchId, Long userA, Long userB) {
        return reviewRepository.countBidirectionalReviewsBetweenUsersInMatch(matchId, userA, userB) >= 2;
    }

    /**
     * 기준 시각 = 경기일시 + 소요시간(모임 일정 종료). 별도 finished_at 컬럼 없을 때의 근사값.
     */
    private boolean isRevealGraceElapsed(Match match) {
        LocalDateTime anchor = match.getMatchDate().atTime(match.getStartTime())
                .plusMinutes(match.getDurationMin());
        LocalDateTime revealFrom = anchor.plusHours(reviewProperties.getRevealAfterFinishHours());
        return !LocalDateTime.now().isBefore(revealFrom);
    }

    /**
     * 방장은 MatchParticipant 행 없이 확정 인원으로 본다. 그 외는 ACCEPTED 참여 행이 있어야 한다.
     * 게스트 여부는 참여 목록을 로드하지 않고 {@code existsBy...} 단일 쿼리로만 판별한다.
     */
    private boolean isConfirmedParticipant(Match match, Long userId) {
        if (match.getHost().getId().equals(userId)) {
            return true;
        }
        return matchParticipantRepository.existsByMatch_IdAndUser_IdAndStatus(match.getId(), userId, ACCEPTED);
    }

    private static float storedRatingOrZero(Float stored) {
        return stored != null ? stored : 0f;
    }

    private static Set<ReviewHashtagCode> resolveHashtagCodes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<ReviewHashtagCode> out = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "해시태그 값이 비어 있습니다.");
            }
            String key = s.trim().toUpperCase();
            try {
                if (!out.add(ReviewHashtagCode.valueOf(key))) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "중복된 해시태그입니다: " + key);
                }
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "알 수 없는 해시태그입니다: " + s.trim());
            }
        }
        return out;
    }
}
