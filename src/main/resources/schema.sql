-- ShedLock (JPA 엔티티가 아님). Hibernate ddl-auto=create 이후 실행(defer-datasource-initialization).
-- 기존 shedlock 행이 있으면 테이블만 갈아끼운다.
DROP TABLE IF EXISTS shedlock;
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
