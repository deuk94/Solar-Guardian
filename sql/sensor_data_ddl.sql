-- 센서 데이터 (라즈베리파이 전류/전압 측정값)
CREATE TABLE sensor_data (
    data_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    current_val DECIMAL(10,3) NOT NULL,
    voltage_val DECIMAL(10,3) NOT NULL,
    measured_at DATETIME      NOT NULL,
    reg_dt      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sensor_data_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE INDEX idx_sensor_data_user_measured ON sensor_data (user_id, measured_at);
