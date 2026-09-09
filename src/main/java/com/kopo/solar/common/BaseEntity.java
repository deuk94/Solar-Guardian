package com.kopo.solar.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "reg_by", nullable = false, length = 20)
    private String regBy;

    @UpdateTimestamp
    @Column(name = "mod_dt", nullable = false)
    private LocalDateTime modDt;

    @Column(name = "mod_by", nullable = false, length = 20)
    private String modBy;

    @Column(name = "del_yn", nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String delYn = "N";

    @Column(name = "del_dt")
    private LocalDateTime delDt;

    @Column(name = "del_by", length = 20)
    private String delBy;

    // 생성 시 등록자를 등록자/수정자 양쪽에 동시에 찍음 (신규 row는 둘이 항상 같음)
    public void stampCreator(String loginId) {
        this.regBy = loginId;
        this.modBy = loginId;
    }

    // 수정자만 갱신 (regBy는 최초 작성자로 그대로 유지)
    public void touch(String modBy) {
        this.modBy = modBy;
    }

    // 삭제 - 실제 row는 남기고 del_yn만 Y로, Board/Notice/Comment/User가 공통으로 사용
    public void softDelete(String delBy) {
        this.delYn = "Y";
        this.delDt = LocalDateTime.now();
        this.delBy = delBy;
    }
}
