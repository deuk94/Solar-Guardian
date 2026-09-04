-- AI 실시간 패널 오염 탐지 결과 (YOLO 추론 서버가 주기적으로 보고)
CREATE TABLE panel_detection (
    detection_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    detected     CHAR(1)       NOT NULL,   -- Y=오염 감지, N=정상
    confidence   DECIMAL(5,4),             -- 감지 시 신뢰도 (미감지 시 NULL)
    checked_at   DATETIME      NOT NULL,
    reg_dt       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_panel_detection_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX idx_panel_detection_user_reg ON panel_detection (user_id, reg_dt DESC);
