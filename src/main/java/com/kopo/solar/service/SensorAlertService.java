package com.kopo.solar.service;

import com.kopo.solar.entity.AlertType;
import com.kopo.solar.entity.SensorAlert;
import com.kopo.solar.entity.SensorData;
import com.kopo.solar.repository.SensorAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorAlertService {

    private final SensorAlertRepository sensorAlertRepository;

    @Value("${sensor.threshold.current-min}")
    private double currentMin;

    @Value("${sensor.threshold.current-max}")
    private double currentMax;

    @Value("${sensor.threshold.voltage-min}")
    private double voltageMin;

    @Value("${sensor.threshold.voltage-max}")
    private double voltageMax;

    @Value("${sensor.alert.cooldown-minutes}")
    private long cooldownMinutes;

    // 측정값이 임계값을 벗어나면 알림 생성
    @Transactional
    public void checkAndCreate(SensorData data) {
        double current = data.getCurrentVal().doubleValue();
        double voltage = data.getVoltageVal().doubleValue();

        if (current > currentMax) {
            save(data, AlertType.CURRENT_HIGH, String.format(
                    "전류가 정상범위(%.1f~%.1fA)를 초과했습니다: %.2fA - 배선/장비 이상 점검이 필요합니다.",
                    currentMin, currentMax, current));
        } else if (current < currentMin) {
            save(data, AlertType.CURRENT_LOW, String.format(
                    "전류가 정상범위(%.1f~%.1fA)보다 낮습니다: %.2fA - 패널 오염으로 인한 발전량 저하일 수 있으니 세척을 권장합니다.",
                    currentMin, currentMax, current));
        }

        if (voltage > voltageMax) {
            save(data, AlertType.VOLTAGE_HIGH, String.format(
                    "전압이 정상범위(%.1f~%.1fV)를 초과했습니다: %.2fV - 배선/장비 이상 점검이 필요합니다.",
                    voltageMin, voltageMax, voltage));
        } else if (voltage < voltageMin) {
            save(data, AlertType.VOLTAGE_LOW, String.format(
                    "전압이 정상범위(%.1f~%.1fV)보다 낮습니다: %.2fV - 배선/장비 이상 점검이 필요합니다.",
                    voltageMin, voltageMax, voltage));
        }
    }

    // 쿨다운 중이 아니면 알림 저장
    private void save(SensorData data, AlertType type, String message) {
        if (isInCooldown(data.getUser().getUserId(), type)) {
            return;
        }

        SensorAlert alert = SensorAlert.builder()
                .user(data.getUser())
                .alertType(type)
                .message(message)
                .currentVal(data.getCurrentVal())
                .voltageVal(data.getVoltageVal())
                .measuredAt(data.getMeasuredAt())
                .build();
        sensorAlertRepository.save(alert);
    }

    // 같은 유형 알림이 쿨다운 시간 내에 이미 발생했는지 확인 (알림 도배 방지)
    private boolean isInCooldown(Long userId, AlertType type) {
        return sensorAlertRepository.findTopByUser_UserIdAndAlertTypeOrderByRegDtDesc(userId, type)
                .map(latest -> latest.getRegDt().isAfter(LocalDateTime.now().minusMinutes(cooldownMinutes)))
                .orElse(false);
    }

    // 알림 이력, 페이지네이션
    public Page<SensorAlert> findByUser(Long userId, Pageable pageable) {
        return sensorAlertRepository.findByUser_UserIdOrderByRegDtDesc(userId, pageable);
    }

    // 최근 알림 5건
    public List<SensorAlert> findRecent(Long userId) {
        return sensorAlertRepository.findTop5ByUser_UserIdOrderByRegDtDesc(userId);
    }
}
