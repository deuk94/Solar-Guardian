package com.kopo.solar.dashboard.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    // 대시보드 메인 (비로그인은 로그인 페이지로)
    @GetMapping
    public String index(HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("loginUser") == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        return "dashboard/index";
    }
}
