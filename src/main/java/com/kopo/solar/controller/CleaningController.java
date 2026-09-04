package com.kopo.solar.controller;

import com.kopo.solar.entity.CleaningCommand;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.CleaningCommandService;
import com.kopo.solar.util.PaginationUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/cleaning")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningCommandService cleaningCommandService;

    // 세척 제어 화면 - 현재 상태 + 이력
    @GetMapping
    public String view(@RequestParam(defaultValue = "0") int page,
                        HttpSession session, Model model, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        Page<CleaningCommand> commandPage = cleaningCommandService.findByUser(
                loginUser.getUserId(), PageRequest.of(page, 10, Sort.by("requestedAt").descending()));
        model.addAttribute("commandPage", commandPage);
        model.addAttribute("pageBlock", PaginationUtil.of(commandPage));
        model.addAttribute("latest", cleaningCommandService.findLatest(loginUser.getUserId()));
        return "dashboard/cleaning";
    }

    // 세척 요청 등록
    @PostMapping("/request")
    public String request(HttpSession session, RedirectAttributes ra) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        try {
            cleaningCommandService.request(loginUser.getUserId());
            ra.addFlashAttribute("message", "세척 요청이 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/cleaning";
    }
}
