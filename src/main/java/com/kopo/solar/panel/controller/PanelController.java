package com.kopo.solar.panel.controller;

import com.kopo.solar.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// 패널 상태 화면 + 웹캠 원본/AI 탐지 오버레이 영상을 원본 서버에서 중계하는 스트림 프록시
@Controller
@RequestMapping("/dashboard/panel")
@RequiredArgsConstructor
public class PanelController {

    @Value("${panel.stream-url}")
    private String streamUrl;

    @Value("${panel.annotated-stream-url}")
    private String annotatedStreamUrl;

    // 패널 상태 화면 진입, 비로그인은 로그인 페이지로
    @GetMapping
    public String view(HttpSession session, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        return "dashboard/panel";
    }

    // mjpg-streamer 원본 영상 그대로 중계
    @GetMapping("/live")
    public ResponseEntity<StreamingResponseBody> live(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        return proxyStream(streamUrl);
    }

    // GPU 추론 서버가 내보내는 탐지 박스 그려진 영상 중계
    @GetMapping("/live-detect")
    public ResponseEntity<StreamingResponseBody> liveDetect(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }
        return proxyStream(annotatedStreamUrl);
    }

    // 원본 스트림 서버에 연결해서 응답 바디를 그대로 클라이언트로 흘려보냄
    private ResponseEntity<StreamingResponseBody> proxyStream(String url) {
        HttpURLConnection conn;
        String contentType;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(0);
            conn.connect();
            contentType = conn.getContentType();
        } catch (IOException e) {
            return ResponseEntity.status(502).build();
        }

        HttpURLConnection sourceConn = conn;
        StreamingResponseBody body = out -> {
            try (InputStream in = sourceConn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (IOException ignored) {
                // 클라이언트 연결 종료 또는 스트림 종료
            } finally {
                sourceConn.disconnect();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(body);
    }
}
