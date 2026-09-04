package com.kopo.solar.controller;

import com.kopo.solar.entity.SensorAlert;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.SensorAlertService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SensorAlertService sensorAlertService;

    // 최근 알림 5건 (대시보드 표시용)
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> recent(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        List<Map<String, Object>> list = sensorAlertService.findRecent(loginUser.getUserId())
                .stream().map(this::toMap).toList();
        return ResponseEntity.ok(list);
    }

    // 알림 엔티티를 화면 표시용 맵으로 변환
    private Map<String, Object> toMap(SensorAlert alert) {
        return Map.of(
                "typeLabel", alert.getAlertType().getLabel(),
                "message",   alert.getMessage(),
                "regDt",     alert.getRegDt().format(FMT)
        );
    }
}
