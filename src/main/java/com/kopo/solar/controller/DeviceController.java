package com.kopo.solar.controller;

import com.kopo.solar.dto.DeviceUpdateDto;
import com.kopo.solar.dto.DeviceWriteDto;
import com.kopo.solar.dto.PanelArrayUpdateDto;
import com.kopo.solar.dto.PanelArrayWriteDto;
import com.kopo.solar.entity.Device;
import com.kopo.solar.entity.PanelArray;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.DeviceService;
import com.kopo.solar.service.PanelArrayService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/dashboard/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final PanelArrayService panelArrayService;

    // 기기 목록 (등록된 패널 어레이 포함)
    @GetMapping
    public String list(HttpSession session, Model model, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }

        Map<Device, java.util.List<PanelArray>> deviceArrays = new LinkedHashMap<>();
        for (Device device : deviceService.findByUser(loginUser.getUserId())) {
            deviceArrays.put(device, panelArrayService.findByDevice(device.getDeviceId()));
        }
        model.addAttribute("deviceArrays", deviceArrays);
        return "dashboard/device/list";
    }

    // 기기 등록 폼
    @GetMapping("/new")
    public String newForm(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isLoggedIn(session)) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        model.addAttribute("deviceWriteDto", new DeviceWriteDto());
        return "dashboard/device/write";
    }

    // 기기 등록 처리
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute DeviceWriteDto dto, BindingResult result,
                          HttpSession session, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        if (result.hasErrors()) {
            return "dashboard/device/write";
        }
        deviceService.create(loginUser.getUserId(), dto);
        ra.addFlashAttribute("message", "기기가 등록되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 기기 수정 폼
    @GetMapping("/{deviceId}/edit")
    public String editForm(@PathVariable Long deviceId, HttpSession session, Model model, RedirectAttributes ra) {
        Device device;
        try {
            device = deviceService.findById(deviceId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, device)) {
            ra.addFlashAttribute("error", "본인 기기만 수정할 수 있습니다.");
            return "redirect:/dashboard/device";
        }

        DeviceUpdateDto dto = new DeviceUpdateDto();
        dto.setDeviceName(device.getDeviceName());
        dto.setSerialNo(device.getSerialNo());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setModelName(device.getModelName());
        dto.setManufacturedDate(device.getManufacturedDate());
        dto.setStatus(device.getStatus());

        model.addAttribute("deviceId", deviceId);
        model.addAttribute("deviceUpdateDto", dto);
        return "dashboard/device/edit";
    }

    // 기기 수정 처리
    @PostMapping("/{deviceId}/edit")
    public String edit(@PathVariable Long deviceId,
                        @Valid @ModelAttribute DeviceUpdateDto dto, BindingResult result,
                        HttpSession session, Model model, RedirectAttributes ra) {
        Device device;
        try {
            device = deviceService.findById(deviceId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, device)) {
            ra.addFlashAttribute("error", "본인 기기만 수정할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        if (result.hasErrors()) {
            model.addAttribute("deviceId", deviceId);
            return "dashboard/device/edit";
        }
        deviceService.update(deviceId, dto);
        ra.addFlashAttribute("message", "기기 정보가 수정되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 기기 삭제
    @PostMapping("/{deviceId}/delete")
    public String deleteDevice(@PathVariable Long deviceId, HttpSession session, RedirectAttributes ra) {
        Device device;
        try {
            device = deviceService.findById(deviceId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, device)) {
            ra.addFlashAttribute("error", "본인 기기만 삭제할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        deviceService.delete(deviceId);
        ra.addFlashAttribute("message", "기기가 삭제되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 패널 어레이 추가 폼
    @GetMapping("/{deviceId}/array/new")
    public String newArrayForm(@PathVariable Long deviceId, HttpSession session, Model model, RedirectAttributes ra) {
        Device device;
        try {
            device = deviceService.findById(deviceId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, device)) {
            ra.addFlashAttribute("error", "본인 기기에만 어레이를 추가할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        model.addAttribute("device", device);
        model.addAttribute("panelArrayWriteDto", new PanelArrayWriteDto());
        return "dashboard/device/array-write";
    }

    // 패널 어레이 추가 처리
    @PostMapping("/{deviceId}/array/new")
    public String createArray(@PathVariable Long deviceId,
                               @Valid @ModelAttribute PanelArrayWriteDto dto, BindingResult result,
                               HttpSession session, Model model, RedirectAttributes ra) {
        Device device;
        try {
            device = deviceService.findById(deviceId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, device)) {
            ra.addFlashAttribute("error", "본인 기기에만 어레이를 추가할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        if (result.hasErrors()) {
            model.addAttribute("device", device);
            return "dashboard/device/array-write";
        }
        panelArrayService.create(deviceId, dto);
        ra.addFlashAttribute("message", "패널 어레이가 추가되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 패널 어레이 수정 폼
    @GetMapping("/array/{arrayId}/edit")
    public String editArrayForm(@PathVariable Long arrayId, HttpSession session, Model model, RedirectAttributes ra) {
        PanelArray array;
        try {
            array = panelArrayService.findById(arrayId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, array.getDevice())) {
            ra.addFlashAttribute("error", "본인 기기의 어레이만 수정할 수 있습니다.");
            return "redirect:/dashboard/device";
        }

        PanelArrayUpdateDto dto = new PanelArrayUpdateDto();
        dto.setArrayName(array.getArrayName());
        dto.setPanelCount(array.getPanelCount());
        dto.setDesignCapacityW(array.getDesignCapacityW());
        dto.setInstalledDate(array.getInstalledDate());
        dto.setInstallLocation(array.getInstallLocation());

        model.addAttribute("arrayId", arrayId);
        model.addAttribute("device", array.getDevice());
        model.addAttribute("panelArrayUpdateDto", dto);
        return "dashboard/device/array-edit";
    }

    // 패널 어레이 수정 처리
    @PostMapping("/array/{arrayId}/edit")
    public String editArray(@PathVariable Long arrayId,
                             @Valid @ModelAttribute PanelArrayUpdateDto dto, BindingResult result,
                             HttpSession session, Model model, RedirectAttributes ra) {
        PanelArray array;
        try {
            array = panelArrayService.findById(arrayId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, array.getDevice())) {
            ra.addFlashAttribute("error", "본인 기기의 어레이만 수정할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        if (result.hasErrors()) {
            model.addAttribute("arrayId", arrayId);
            model.addAttribute("device", array.getDevice());
            return "dashboard/device/array-edit";
        }
        panelArrayService.update(arrayId, dto);
        ra.addFlashAttribute("message", "패널 어레이가 수정되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 패널 어레이 삭제
    @PostMapping("/array/{arrayId}/delete")
    public String deleteArray(@PathVariable Long arrayId, HttpSession session, RedirectAttributes ra) {
        PanelArray array;
        try {
            array = panelArrayService.findById(arrayId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/device";
        }
        if (!canManage(session, array.getDevice())) {
            ra.addFlashAttribute("error", "본인 기기의 어레이만 삭제할 수 있습니다.");
            return "redirect:/dashboard/device";
        }
        panelArrayService.delete(arrayId);
        ra.addFlashAttribute("message", "패널 어레이가 삭제되었습니다.");
        return "redirect:/dashboard/device";
    }

    // 로그인 여부
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loginUser") != null;
    }

    // 본인 소유 기기인지 확인
    private boolean canManage(HttpSession session, Device device) {
        User loginUser = (User) session.getAttribute("loginUser");
        return loginUser != null && loginUser.getUserId().equals(device.getUser().getUserId());
    }
}
