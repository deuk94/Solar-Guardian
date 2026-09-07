package com.kopo.solar.controller;

import com.kopo.solar.dto.SensorDataDto;
import com.kopo.solar.entity.SensorData;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.SensorDataService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorDataApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SensorDataService sensorDataService;

    @Value("${sensor.api-key}")
    private String apiKey;

    // 센서 측정값 수신 (라즈베리파이)
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader("X-API-KEY") String key,
            @Valid @RequestBody SensorDataDto dto) {

        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        sensorDataService.save(dto);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 최신 측정값
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> latest(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        SensorData data = sensorDataService.findLatest(loginUser.getUserId());
        if (data == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(toMap(data));
    }

    // 최근 측정값 목록
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> recent(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        List<Map<String, Object>> list = sensorDataService.findRecent(loginUser.getUserId())
                .stream().map(this::toMap).toList();
        return ResponseEntity.ok(list);
    }

    // 오늘 시간대별 발전량 통계 (대시보드 차트)
    @GetMapping("/today-hourly")
    public ResponseEntity<Map<String, Object>> todayHourly(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        SensorDataService.TodayStats stats = sensorDataService.getTodayStats(loginUser.getUserId());

        List<Map<String, Object>> hourly = stats.hourly().stream()
                .map(p -> Map.<String, Object>of(
                        "hour", p.hour(),
                        "energyWh", round(p.energyWh())))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("totalEnergyWh", round(stats.totalEnergyWh()));
        result.put("readingCount", stats.readingCount());
        result.put("maxCurrent", stats.maxCurrent());
        result.put("minCurrent", stats.minCurrent());
        result.put("maxVoltage", stats.maxVoltage());
        result.put("minVoltage", stats.minVoltage());
        result.put("hourly", hourly);
        return ResponseEntity.ok(result);
    }

    // 최근 며칠간 날짜별 발전량
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> daily(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        List<Map<String, Object>> daily = sensorDataService.getDailyStats(loginUser.getUserId()).stream()
                .map(p -> Map.<String, Object>of(
                        "date", p.date().toString(),
                        "energyWh", round(p.energyWh())))
                .toList();

        return ResponseEntity.ok(Map.of("daily", daily));
    }

    // 소수점 둘째 자리로 반올림
    private double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    // 측정값을 화면 표시용 맵으로 변환, 전력(W)은 여기서 즉시 계산
    private Map<String, Object> toMap(SensorData data) {
        double power = data.getCurrentVal().doubleValue() * data.getVoltageVal().doubleValue();
        return Map.of(
                "current",    data.getCurrentVal(),
                "voltage",    data.getVoltageVal(),
                "power",      round(power),
                "measuredAt", data.getMeasuredAt().format(FMT)
        );
    }
}
