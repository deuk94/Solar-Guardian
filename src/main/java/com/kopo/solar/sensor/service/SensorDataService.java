package com.kopo.solar.sensor.service;

import com.kopo.solar.sensor.dto.SensorDataDto;
import com.kopo.solar.sensor.entity.SensorData;
import com.kopo.solar.user.entity.User;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.sensor.repository.SensorDataRepository;
import com.kopo.solar.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorDataService {

    private static final int RECENT_COUNT = 20;
    private static final int DAILY_STATS_DAYS = 7;

    private final SensorDataRepository sensorDataRepository;
    private final UserRepository userRepository;
    private final SensorAlertService sensorAlertService;

    // 측정값 저장 후 임계값 이탈 여부 확인, 필요하면 알림 생성
    @Transactional
    public SensorData save(SensorDataDto dto) {
        User user = userRepository.findByLoginId(dto.getLoginId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        SensorData sensorData = SensorData.builder()
                .user(user)
                .currentVal(dto.getCurrent())
                .voltageVal(dto.getVoltage())
                .measuredAt(dto.getMeasuredAt() != null ? dto.getMeasuredAt() : LocalDateTime.now())
                .build();

        SensorData saved = sensorDataRepository.save(sensorData);
        sensorAlertService.checkAndCreate(saved);
        return saved;
    }

    // 최근 측정값 하나 (없으면 null)
    public SensorData findLatest(Long userId) {
        return sensorDataRepository.findFirstByUser_UserIdOrderByMeasuredAtDesc(userId).orElse(null);
    }

    // 최근 측정값 RECENT_COUNT건, 시간순(오래된 것부터)으로 뒤집어서 반환
    public List<SensorData> findRecent(Long userId) {
        List<SensorData> recent = sensorDataRepository.findByUser_UserIdOrderByMeasuredAtDesc(
                userId, PageRequest.of(0, RECENT_COUNT));
        Collections.reverse(recent);
        return recent;
    }

    // 오늘 시간대별 발전량 + 전류·전압 최고/최저값
    public TodayStats getTodayStats(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<SensorData> readings = sensorDataRepository
                .findByUser_UserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(userId, start, end);

        if (readings.isEmpty()) {
            return new TodayStats(0, List.of(), 0, null, null, null, null);
        }

        BigDecimal maxCurrent = readings.get(0).getCurrentVal();
        BigDecimal minCurrent = readings.get(0).getCurrentVal();
        BigDecimal maxVoltage = readings.get(0).getVoltageVal();
        BigDecimal minVoltage = readings.get(0).getVoltageVal();
        for (SensorData r : readings) {
            if (r.getCurrentVal().compareTo(maxCurrent) > 0) maxCurrent = r.getCurrentVal();
            if (r.getCurrentVal().compareTo(minCurrent) < 0) minCurrent = r.getCurrentVal();
            if (r.getVoltageVal().compareTo(maxVoltage) > 0) maxVoltage = r.getVoltageVal();
            if (r.getVoltageVal().compareTo(minVoltage) < 0) minVoltage = r.getVoltageVal();
        }

        // 연속된 두 측정값 사이를 사다리꼴로 적분해서 시간대별 발전량(Wh) 추정
        double[] hourlyWh = new double[24];
        for (int i = 0; i < readings.size() - 1; i++) {
            SensorData a = readings.get(i);
            SensorData b = readings.get(i + 1);
            double powerA = a.getCurrentVal().doubleValue() * a.getVoltageVal().doubleValue();
            double powerB = b.getCurrentVal().doubleValue() * b.getVoltageVal().doubleValue();
            double dtHours = Duration.between(a.getMeasuredAt(), b.getMeasuredAt()).toMillis() / 3_600_000.0;
            hourlyWh[a.getMeasuredAt().getHour()] += (powerA + powerB) / 2.0 * dtHours;
        }

        List<HourlyPoint> hourly = new ArrayList<>();
        double totalWh = 0;
        for (int h = 0; h < 24; h++) {
            totalWh += hourlyWh[h];
            if (hourlyWh[h] != 0) {
                hourly.add(new HourlyPoint(h, hourlyWh[h]));
            }
        }

        return new TodayStats(totalWh, hourly, readings.size(), maxCurrent, minCurrent, maxVoltage, minVoltage);
    }

    // 최근 DAILY_STATS_DAYS일간 날짜별 발전량
    public List<DailyPoint> getDailyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(DAILY_STATS_DAYS - 1);
        LocalDateTime start = rangeStart.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<SensorData> readings = sensorDataRepository
                .findByUser_UserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(userId, start, end);

        // 시간대별 발전량과 같은 사다리꼴 적분, 버킷만 시(hour)에서 날짜(date)로 변경
        java.util.Map<LocalDate, Double> dailyWh = new java.util.LinkedHashMap<>();
        for (int i = 0; i < DAILY_STATS_DAYS; i++) {
            dailyWh.put(rangeStart.plusDays(i), 0.0);
        }
        for (int i = 0; i < readings.size() - 1; i++) {
            SensorData a = readings.get(i);
            SensorData b = readings.get(i + 1);
            double powerA = a.getCurrentVal().doubleValue() * a.getVoltageVal().doubleValue();
            double powerB = b.getCurrentVal().doubleValue() * b.getVoltageVal().doubleValue();
            double dtHours = Duration.between(a.getMeasuredAt(), b.getMeasuredAt()).toMillis() / 3_600_000.0;
            LocalDate day = a.getMeasuredAt().toLocalDate();
            dailyWh.merge(day, (powerA + powerB) / 2.0 * dtHours, Double::sum);
        }

        List<DailyPoint> daily = new ArrayList<>();
        for (var entry : dailyWh.entrySet()) {
            daily.add(new DailyPoint(entry.getKey(), entry.getValue()));
        }
        return daily;
    }

    public record HourlyPoint(int hour, double energyWh) {}

    public record DailyPoint(LocalDate date, double energyWh) {}

    public record TodayStats(
            double totalEnergyWh,
            List<HourlyPoint> hourly,
            int readingCount,
            BigDecimal maxCurrent,
            BigDecimal minCurrent,
            BigDecimal maxVoltage,
            BigDecimal minVoltage
    ) {}
}
