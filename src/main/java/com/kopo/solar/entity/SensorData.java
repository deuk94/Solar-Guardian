package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_id")
    private Long dataId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
