package com.kopo.solar.repository;

import com.kopo.solar.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    // 회원 소유 기기 목록, 등록일 순
    List<Device> findByUser_UserIdOrderByRegDtAsc(Long userId);
}
