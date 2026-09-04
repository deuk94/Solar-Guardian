-- 기기 관리 (등록된 Solar Guardian 기기)
CREATE TABLE device (
    device_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    device_name       VARCHAR(100) NOT NULL,
    serial_no         VARCHAR(50)  NOT NULL,
    firmware_version  VARCHAR(20)  NOT NULL,
    model_name        VARCHAR(50)  NOT NULL,
    manufactured_date DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    reg_dt            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_dt            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 기기에 연결된 패널 어레이
CREATE TABLE panel_array (
    array_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id         BIGINT       NOT NULL,
    array_name        VARCHAR(100) NOT NULL,
    panel_count       INT          NOT NULL,
    design_capacity_w INT          NOT NULL,
    installed_date    DATE         NOT NULL,
    install_location  VARCHAR(255) NOT NULL,
    reg_dt            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_dt            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_panel_array_device FOREIGN KEY (device_id) REFERENCES device(device_id)
);
