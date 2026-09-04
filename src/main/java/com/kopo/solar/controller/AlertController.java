package com.kopo.solar.controller;

import com.kopo.solar.entity.SensorAlert;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.SensorAlertService;
import com.kopo.solar.util.PaginationUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final SensorAlertService sensorAlertService;

    // 알림/리포트 목록 화면
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                        HttpSession session, Model model, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        Page<SensorAlert> alertPage = sensorAlertService.findByUser(
                loginUser.getUserId(), PageRequest.of(page, 10, Sort.by("regDt").descending()));
        model.addAttribute("alertPage", alertPage);
        model.addAttribute("pageBlock", PaginationUtil.of(alertPage));
        return "dashboard/alerts";
    }
}
