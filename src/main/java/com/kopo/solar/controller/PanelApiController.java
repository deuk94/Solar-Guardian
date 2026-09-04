package com.kopo.solar.controller;

import com.kopo.solar.entity.PanelDetection;
import com.kopo.solar.entity.PanelSnapshot;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.PanelDetectionService;
import com.kopo.solar.service.PanelSnapshotService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PanelApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PanelSnapshotService panelSnapshotService;
    private final PanelDetectionService panelDetectionService;

    @Value("${sensor.api-key}")
    private String apiKey;

    /* ─── 라즈베리파이 업로드(API 키 기반) ─── */

    // 패널 스냅샷 업로드 (라즈베리파이)
    @PostMapping("/api/panel/snapshot")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestHeader("X-API-KEY") String key,
            @RequestParam String loginId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt) {

        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        try {
            PanelSnapshot snapshot = panelSnapshotService.save(loginId, file, capturedAt);
            return ResponseEntity.ok(Map.of("success", true, "snapshotId", snapshot.getSnapshotId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ─── AI 추론 서버의 실시간 탐지 상태 보고(API 키 기반) ─── */

    // AI 실시간 오염 탐지 상태 보고 (GPU 추론 서버)
    @PostMapping("/api/panel/detection")
    public ResponseEntity<Map<String, Object>> reportDetection(
            @RequestHeader("X-API-KEY") String key,
            @RequestParam String loginId,
            @RequestParam boolean detected,
            @RequestParam(required = false) Double confidence) {

        if (!apiKey.equals(key)) {
            return ResponseEntity.status(403).body(Map.of("error", "유효하지 않은 API 키입니다."));
        }
        try {
            PanelDetection detection = panelDetectionService.save(loginId, detected, confidence);
            return ResponseEntity.ok(Map.of("success", true, "detectionId", detection.getDetectionId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ─── 대시보드 화면(세션 기반) ─── */

    // 최신 오염 탐지 상태 (패널 화면 5초 폴링)
    @GetMapping("/api/panel/detection/latest")
    public ResponseEntity<Map<String, Object>> latestDetection(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        PanelDetection detection = panelDetectionService.findLatest(loginUser.getUserId());
        if (detection == null) {
            return ResponseEntity.ok(Map.of());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("detected", "Y".equals(detection.getDetected()));
        result.put("confidence", detection.getConfidence());
        result.put("checkedAt", detection.getCheckedAt().format(FMT));
        return ResponseEntity.ok(result);
    }

    // 최신 패널 스냅샷 정보
    @GetMapping("/api/panel/latest")
    public ResponseEntity<Map<String, Object>> latest(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        PanelSnapshot snapshot = panelSnapshotService.findLatest(loginUser.getUserId());
        if (snapshot == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of(
                "snapshotId", snapshot.getSnapshotId(),
                "capturedAt", snapshot.getCapturedAt().format(FMT)
        ));
    }

    // 스냅샷 이미지 조회 (본인 소유만)
    @GetMapping("/dashboard/panel/{snapshotId}/image")
    public ResponseEntity<Resource> image(@PathVariable Long snapshotId, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        PanelSnapshot snapshot = panelSnapshotService.findById(snapshotId);
        if (!snapshot.getUser().getUserId().equals(loginUser.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        Resource resource = new FileSystemResource(snapshot.getFilePath());
        MediaType mediaType = MediaTypeFactory.getMediaType(snapshot.getFilePath())
                .orElse(MediaType.IMAGE_JPEG);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
