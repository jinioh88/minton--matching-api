-- Sprint 5 Step 4: 인앱 알림
CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NULL,
    payload TEXT NULL,
    read_at DATETIME(6) NULL,
    related_match_id BIGINT NULL,
    related_participant_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    INDEX idx_notifications_user_created (user_id, created_at)
);
