package com.kopo.solar.device.entity;

import com.kopo.solar.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "serial_no", nullable = false, length = 50)
    private String serialNo;

    @Column(name = "firmware_version", nullable = false, length = 20)
    private String firmwareVersion;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "manufactured_date", nullable = false)
    private LocalDate manufacturedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "mod_dt", nullable = false)
    private LocalDateTime modDt;

    public void update(String deviceName, String serialNo, String firmwareVersion,
                        String modelName, LocalDate manufacturedDate, DeviceStatus status) {
        this.deviceName = deviceName;
        this.serialNo = serialNo;
        this.firmwareVersion = firmwareVersion;
        this.modelName = modelName;
        this.manufacturedDate = manufacturedDate;
        this.status = status;
    }
}
