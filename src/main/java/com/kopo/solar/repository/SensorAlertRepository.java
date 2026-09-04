package com.kopo.solar.repository;

import com.kopo.solar.entity.AlertType;
import com.kopo.solar.entity.SensorAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorAlertRepository extends JpaRepository<SensorAlert, Long> {

    // 회원별 알림 목록, 최신순 페이지네이션
    Page<SensorAlert> findByUser_UserIdOrderByRegDtDesc(Long userId, Pageable pageable);

    // 최근 알림 5건
    List<SensorAlert> findTop5ByUser_UserIdOrderByRegDtDesc(Long userId);

    // 같은 유형의 가장 최근 알림 하나 - 쿨다운 체크용
    Optional<SensorAlert> findTopByUser_UserIdAndAlertTypeOrderByRegDtDesc(Long userId, AlertType alertType);
}
