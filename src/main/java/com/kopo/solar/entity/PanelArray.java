package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "panel_array")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelArray {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "array_id")
    private Long arrayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "array_name", nullable = false, length = 100)
    private String arrayName;

    @Column(name = "panel_count", nullable = false)
    private Integer panelCount;

    @Column(name = "design_capacity_w", nullable = false)
    private Integer designCapacityW;

    @Column(name = "installed_date", nullable = false)
    private LocalDate installedDate;

    @Column(name = "install_location", nullable = false, length = 255)
    private String installLocation;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "mod_dt", nullable = false)
    private LocalDateTime modDt;
}
