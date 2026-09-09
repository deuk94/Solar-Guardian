package com.kopo.solar.device.repository;

import com.kopo.solar.device.entity.PanelArray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PanelArrayRepository extends JpaRepository<PanelArray, Long> {

    // 기기에 속한 패널 어레이 목록, 등록일 순
    List<PanelArray> findByDevice_DeviceIdOrderByRegDtAsc(Long deviceId);

    // 기기에 속한 패널 어레이 전부 삭제 (기기 삭제 시 FK 제약 피하려고 먼저 호출)
    void deleteByDevice_DeviceId(Long deviceId);
}
