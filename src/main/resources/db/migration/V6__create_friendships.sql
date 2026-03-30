-- 단방향 팔로우(친구 추가). follower_id = 나, following_id = 상대
-- 운영 Flyway: 기존 DB에 V1~V5 등이 이미 적용된 경우 버전 번호를 환경에 맞게 조정할 수 있다.
CREATE TABLE friendships (
    friendship_id BIGINT NOT NULL AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (friendship_id),
    CONSTRAINT uk_friendships_follower_following UNIQUE (follower_id, following_id),
    CONSTRAINT fk_friendships_follower FOREIGN KEY (follower_id) REFERENCES users (user_id),
    CONSTRAINT fk_friendships_following FOREIGN KEY (following_id) REFERENCES users (user_id),
    CONSTRAINT chk_friendships_not_self CHECK (follower_id <> following_id)
);

CREATE INDEX idx_friendships_following ON friendships (following_id);
