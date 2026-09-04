-- 게시판
CREATE TABLE board (
    board_id BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title    VARCHAR(200) NOT NULL,
    content  TEXT         NOT NULL,
    view_cnt BIGINT       NOT NULL DEFAULT 0,
    reg_dt   DATETIME     NOT NULL,
    reg_by   VARCHAR(20)  NOT NULL,
    mod_dt   DATETIME     NOT NULL,
    mod_by   VARCHAR(20)  NOT NULL,
    del_yn   CHAR(1)      NOT NULL DEFAULT 'N',
    del_dt   DATETIME     NULL,
    del_by   VARCHAR(20)  NULL
);

-- 댓글
CREATE TABLE comment (
    comment_id BIGINT      AUTO_INCREMENT PRIMARY KEY,
    board_id   BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    reg_dt     DATETIME    NOT NULL,
    reg_by     VARCHAR(20) NOT NULL,
    mod_dt     DATETIME    NOT NULL,
    mod_by     VARCHAR(20) NOT NULL,
    del_yn     CHAR(1)     NOT NULL DEFAULT 'N',
    del_dt     DATETIME    NULL,
    del_by     VARCHAR(20) NULL,
    CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES board (board_id)
);
