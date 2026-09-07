package com.kopo.solar.controller;

import com.kopo.solar.dto.UserJoinDto;
import com.kopo.solar.dto.UserLoginDto;
import com.kopo.solar.dto.UserUpdateDto;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입 폼
    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("userJoinDto", new UserJoinDto());
        return "user/join";
    }

    // 회원가입 처리
    @PostMapping("/join")
    public String join(@Valid @ModelAttribute UserJoinDto dto, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "user/join";
        }
        try {
            userService.join(dto);
            ra.addFlashAttribute("message", "회원가입이 완료되었습니다.");
            return "redirect:/user/login";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/join";
        }
    }

    // 로그인 폼
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("userLoginDto", new UserLoginDto());
        return "user/login";
    }

    // 로그인 처리, 성공 시 세션에 loginUser 저장
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute UserLoginDto dto, BindingResult result,
                        HttpSession session, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "user/login";
        }
        try {
            User user = userService.login(dto);
            session.setAttribute("loginUser", user);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/login";
        }
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

    // 회원 상세 조회
    @GetMapping("/{userId}")
    public String detail(@PathVariable Long userId, Model model) {
        model.addAttribute("user", userService.findById(userId));
        return "user/detail";
    }

    // 회원정보 수정 폼
    @GetMapping("/{userId}/edit")
    public String editForm(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId);
        UserUpdateDto dto = new UserUpdateDto();
        dto.setName(user.getName());
        dto.setGender(user.getGender());
        dto.setTelNo(user.getTelNo());
        dto.setAddress(user.getAddress());
        dto.setEmail(user.getEmail());
        model.addAttribute("userId", userId);
        model.addAttribute("userUpdateDto", dto);
        return "user/edit";
    }

    // 회원정보 수정 처리
    @PostMapping("/{userId}/edit")
    public String edit(@PathVariable Long userId,
                       @Valid @ModelAttribute UserUpdateDto dto, BindingResult result,
                       HttpSession session, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("userId", userId);
            return "user/edit";
        }
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            userService.update(userId, dto, loginUser.getLoginId());
            ra.addFlashAttribute("message", "회원정보가 수정되었습니다.");
            return "redirect:/user/" + userId;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/" + userId + "/edit";
        }
    }

    // 회원 탈퇴 처리
    @PostMapping("/{userId}/withdraw")
    public String withdraw(@PathVariable Long userId, HttpSession session, RedirectAttributes ra) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            userService.withdraw(userId, loginUser.getLoginId());
            session.invalidate();
            ra.addFlashAttribute("message", "탈퇴 처리되었습니다.");
            return "redirect:/user/login";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/" + userId;
        }
    }

}
