package com.kopo.solar.repository;

import com.kopo.solar.entity.PanelDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PanelDetectionRepository extends JpaRepository<PanelDetection, Long> {

    // 회원의 최신 AI 오염탐지 결과
    Optional<PanelDetection> findFirstByUser_UserIdOrderByRegDtDesc(Long userId);
}
