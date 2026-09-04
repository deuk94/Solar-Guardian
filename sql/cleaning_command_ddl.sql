-- 세척 제어 명령 (원격 세척 요청/완료 이력)
CREATE TABLE cleaning_command (
    command_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME     NULL,
    CONSTRAINT fk_cleaning_command_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX idx_cleaning_command_user ON cleaning_command (user_id, requested_at DESC);
