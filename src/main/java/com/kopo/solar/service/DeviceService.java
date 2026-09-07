package com.kopo.solar.service;

import com.kopo.solar.dto.DeviceUpdateDto;
import com.kopo.solar.dto.DeviceWriteDto;
import com.kopo.solar.entity.Device;
import com.kopo.solar.entity.DeviceStatus;
import com.kopo.solar.entity.User;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.repository.DeviceRepository;
import com.kopo.solar.repository.PanelArrayRepository;
import com.kopo.solar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final PanelArrayRepository panelArrayRepository;

    // 회원 기기 목록, 등록일 순
    public List<Device> findByUser(Long userId) {
        return deviceRepository.findByUser_UserIdOrderByRegDtAsc(userId);
    }

    // 기기 조회, 없으면 예외
    public Device findById(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 기기입니다."));
    }

    // 새 기기 등록, 상태는 항상 NORMAL로 시작
    @Transactional
    public Device create(Long userId, DeviceWriteDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        Device device = Device.builder()
                .user(user)
                .deviceName(dto.getDeviceName())
                .serialNo(dto.getSerialNo())
                .firmwareVersion(dto.getFirmwareVersion())
                .modelName(dto.getModelName())
                .manufacturedDate(dto.getManufacturedDate())
                .status(DeviceStatus.NORMAL)
                .build();

        return deviceRepository.save(device);
    }

    // 기기 정보 수정
    @Transactional
    public void update(Long deviceId, DeviceUpdateDto dto) {
        Device device = findById(deviceId);
        device.update(dto.getDeviceName(), dto.getSerialNo(), dto.getFirmwareVersion(),
                dto.getModelName(), dto.getManufacturedDate(), dto.getStatus());
    }

    /*
     * device는 소프트 삭제 대상 아니라 실제 row 삭제. panel_array가 device_id를 FK로 참조하고
     * ON DELETE CASCADE 없어서, 딸린 어레이부터 지워야 FK 위반 없이 삭제 가능
     */
    @Transactional
    public void delete(Long deviceId) {
        Device device = findById(deviceId);
        panelArrayRepository.deleteByDevice_DeviceId(deviceId);
        deviceRepository.delete(device);
    }
}
