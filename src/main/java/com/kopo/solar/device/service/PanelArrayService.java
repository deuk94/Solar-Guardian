package com.kopo.solar.device.service;

import com.kopo.solar.device.dto.PanelArrayUpdateDto;
import com.kopo.solar.device.dto.PanelArrayWriteDto;
import com.kopo.solar.device.entity.Device;
import com.kopo.solar.device.entity.PanelArray;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.device.repository.PanelArrayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelArrayService {

    private final PanelArrayRepository panelArrayRepository;
    private final DeviceService deviceService;

    // 기기에 속한 패널 어레이 목록, 등록일 순
    public List<PanelArray> findByDevice(Long deviceId) {
        return panelArrayRepository.findByDevice_DeviceIdOrderByRegDtAsc(deviceId);
    }

    // 패널 어레이 조회, 없으면 예외
    public PanelArray findById(Long arrayId) {
        return panelArrayRepository.findById(arrayId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 패널 어레이입니다."));
    }

    // 기기에 패널 어레이 새로 등록
    @Transactional
    public PanelArray create(Long deviceId, PanelArrayWriteDto dto) {
        Device device = deviceService.findById(deviceId);

        PanelArray array = PanelArray.builder()
                .device(device)
                .arrayName(dto.getArrayName())
                .panelCount(dto.getPanelCount())
                .designCapacityW(dto.getDesignCapacityW())
                .installedDate(dto.getInstalledDate())
                .installLocation(dto.getInstallLocation())
                .build();

        return panelArrayRepository.save(array);
    }

    // 패널 어레이 정보 수정
    @Transactional
    public void update(Long arrayId, PanelArrayUpdateDto dto) {
        PanelArray array = findById(arrayId);
        array.update(dto.getArrayName(), dto.getPanelCount(), dto.getDesignCapacityW(),
                dto.getInstalledDate(), dto.getInstallLocation());
    }

    // panel_array는 BaseEntity 상속 안 해서 소프트 삭제 대상 아님 - 실제 row 삭제
    @Transactional
    public void delete(Long arrayId) {
        PanelArray array = findById(arrayId);
        panelArrayRepository.delete(array);
    }
}
