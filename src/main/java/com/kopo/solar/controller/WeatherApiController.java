package com.kopo.solar.controller;

import com.kopo.solar.entity.User;
import com.kopo.solar.service.WeatherService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherApiController {

    private final WeatherService weatherService;

    // 단기/중기예보 캐시를 화면 표시용 형태로 반환
    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> forecast(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        List<Map<String, Object>> hourly = weatherService.getHourly().stream()
                .map(h -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", h.label());
                    row.put("temp", h.temp());
                    row.put("sky", h.sky());
                    row.put("pop", h.pop());
                    row.put("dayMarker", h.dayMarker());
                    row.put("humidity", h.humidity());
                    row.put("windDeg", h.windDeg());
                    row.put("windDir", h.windDir());
                    row.put("windSpeed", h.windSpeed());
                    row.put("pcp", h.pcp());
                    return row;
                })
                .toList();

        List<Map<String, Object>> daily = weatherService.getDaily().stream()
                .map(d -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", d.date().toString());
                    row.put("label", d.label());
                    row.put("minTemp", d.minTemp());
                    row.put("maxTemp", d.maxTemp());
                    row.put("amPop", d.amPop());
                    row.put("pmPop", d.pmPop());
                    row.put("amSky", d.amSky());
                    row.put("pmSky", d.pmSky());
                    return row;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("hourly", hourly);
        result.put("daily", daily);
        return ResponseEntity.ok(result);
    }
}
