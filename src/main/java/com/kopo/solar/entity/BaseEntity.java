package com.kopo.solar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
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
}
