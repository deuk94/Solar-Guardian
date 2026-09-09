package com.kopo.solar;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 로그인 여부에 따라 메인/로그인으로
    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/user/login";
        }
        return "index";
    }
}
