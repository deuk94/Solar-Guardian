package com.kopo.solar.sensor.entity;

import com.kopo.solar.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_alert")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "current_val", nullable = false, precision = 10, scale = 3)
    private BigDecimal currentVal;

    @Column(name = "voltage_val", nullable = false, precision = 10, scale = 3)
    private BigDecimal voltageVal;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;
}
