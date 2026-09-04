package com.kopo.solar.repository;

import com.kopo.solar.entity.PanelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PanelSnapshotRepository extends JpaRepository<PanelSnapshot, Long> {

    // 회원의 가장 최근 패널 스냅샷
    Optional<PanelSnapshot> findFirstByUser_UserIdOrderByCapturedAtDesc(Long userId);
}
