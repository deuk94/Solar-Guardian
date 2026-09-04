package com.kopo.solar.service;

import com.kopo.solar.entity.PanelDetection;
import com.kopo.solar.entity.User;
import com.kopo.solar.repository.PanelDetectionRepository;
import com.kopo.solar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelDetectionService {

    private final PanelDetectionRepository panelDetectionRepository;
    private final UserRepository userRepository;

    // GPU 추론 서버가 보고한 오염 탐지 결과 저장
    @Transactional
    public PanelDetection save(String loginId, boolean detected, Double confidence) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        PanelDetection detection = PanelDetection.builder()
                .user(user)
                .detected(detected ? "Y" : "N")
                .confidence(confidence != null ? BigDecimal.valueOf(confidence) : null)
                .checkedAt(LocalDateTime.now())
                .build();

        return panelDetectionRepository.save(detection);
    }

    // 최신 오염 탐지 결과 (패널 화면 5초 폴링용)
    public PanelDetection findLatest(Long userId) {
        return panelDetectionRepository.findFirstByUser_UserIdOrderByRegDtDesc(userId).orElse(null);
    }
}
