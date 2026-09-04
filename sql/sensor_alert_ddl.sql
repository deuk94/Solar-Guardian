-- 센서 이상 알림 (전류/전압 임계값 초과 이력)
CREATE TABLE sensor_alert (
    alert_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    alert_type  VARCHAR(20)   NOT NULL,
    message     VARCHAR(255)  NOT NULL,
    current_val DECIMAL(10,3) NOT NULL,
    voltage_val DECIMAL(10,3) NOT NULL,
    measured_at DATETIME      NOT NULL,
    reg_dt      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sensor_alert_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX idx_sensor_alert_user_reg ON sensor_alert (user_id, reg_dt DESC);
