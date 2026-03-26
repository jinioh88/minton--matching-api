-- ShedLock (JPA 엔티티가 아님). Hibernate 초기화 이후 실행(defer-datasource-initialization).
-- 기존 테이블·데이터 유지: 없을 때만 생성.
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
