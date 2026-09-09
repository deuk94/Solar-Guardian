package com.kopo.solar.panel.entity;

import com.kopo.solar.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "panel_detection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_id")
    private Long detectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "detected", nullable = false, length = 1)
    private String detected;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;
}
