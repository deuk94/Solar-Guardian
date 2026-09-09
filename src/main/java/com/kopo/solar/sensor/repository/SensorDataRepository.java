package com.kopo.solar.sensor.repository;

import com.kopo.solar.sensor.entity.SensorData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    // 최신 측정값 하나
    Optional<SensorData> findFirstByUser_UserIdOrderByMeasuredAtDesc(Long userId);

    // 최근 측정값 N건, 최신순
    List<SensorData> findByUser_UserIdOrderByMeasuredAtDesc(Long userId, Pageable pageable);

    // 기간 내 측정값, 오래된 순 (통계/차트 계산용)
    List<SensorData> findByUser_UserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
