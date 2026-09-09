package com.kopo.solar.cleaning.controller;

import com.kopo.solar.cleaning.entity.CleaningCommand;
import com.kopo.solar.user.entity.User;
import com.kopo.solar.cleaning.service.CleaningCommandService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/cleaning")
@RequiredArgsConstructor
public class CleaningApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CleaningCommandService cleaningCommandService;

    @Value("${sensor.api-key}")
    private String apiKey;

    // 세척 상태 폴링 (대시보드 화면)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        CleaningCommand latest = cleaningCommandService.findLatest(loginUser.getUserId());
        if (latest == null) {
            return ResponseEntity.ok(Map.of("status", "NONE"));
        }
        return ResponseEntity.ok(Map.of(
                "status", latest.getStatus().name(),
                "statusLabel", latest.getStatus().getLabel(),
                "requestedAt", latest.getRequestedAt().format(FMT),
                "completedAt", latest.getCompletedAt() != null ? latest.getCompletedAt().format(FMT) : ""
        ));
    }

    // 대기 중인 세척 명령 폴링 (라즈베리파이)
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> pending(@RequestHeader("X-API-KEY") String key,
                                                         @RequestParam String loginId) {
        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        return cleaningCommandService.findPending(loginId)
                .map(c -> ResponseEntity.ok(Map.<String, Object>of(
                        "commandId", c.getCommandId(),
                        "requestedAt", c.getRequestedAt().format(FMT))))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    // 세척 완료 보고 (라즈베리파이)
    @PostMapping("/{commandId}/complete")
    public ResponseEntity<Map<String, Object>> complete(@RequestHeader("X-API-KEY") String key,
                                                          @PathVariable Long commandId) {
        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        cleaningCommandService.complete(commandId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // AI가 오염 연속 감지 시 자동 세척 요청 (GPU 추론 서버)
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> request(@RequestHeader("X-API-KEY") String key,
                                                         @RequestParam String loginId) {
        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        CleaningCommand command = cleaningCommandService.requestAuto(loginId);
        return ResponseEntity.ok(Map.of("success", true, "commandId", command.getCommandId()));
    }
}
