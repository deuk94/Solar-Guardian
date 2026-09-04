-- 공지사항 (관리자 게시판)
CREATE TABLE notice (
    notice_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    content     TEXT          NOT NULL,
    view_cnt    BIGINT        NOT NULL DEFAULT 0,
    reg_dt      DATETIME      NOT NULL,
    reg_by      VARCHAR(20)   NOT NULL,
    mod_dt      DATETIME      NOT NULL,
    mod_by      VARCHAR(20)   NOT NULL,
    del_yn      CHAR(1)       NOT NULL DEFAULT 'N',
    del_dt      DATETIME      NULL,
    del_by      VARCHAR(20)   NULL
);

-- 공지사항 첨부파일
CREATE TABLE notice_file (
    file_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id   BIGINT        NOT NULL,
    origin_name VARCHAR(255)  NOT NULL,
    stored_name VARCHAR(255)  NOT NULL,
    file_path   VARCHAR(500)  NOT NULL,
    file_size   BIGINT        NOT NULL,
    reg_dt      DATETIME      NOT NULL,
    reg_by      VARCHAR(20)   NOT NULL,
    CONSTRAINT fk_notice_file_notice FOREIGN KEY (notice_id) REFERENCES notice(notice_id)
);
