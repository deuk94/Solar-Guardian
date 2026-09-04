-- AI 패널 상태 스냅샷 (웹캠으로 주기 촬영한 패널 사진)
CREATE TABLE panel_snapshot (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    captured_at DATETIME     NOT NULL,
    reg_dt      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_panel_snapshot_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX idx_panel_snapshot_user_captured ON panel_snapshot (user_id, captured_at DESC);
