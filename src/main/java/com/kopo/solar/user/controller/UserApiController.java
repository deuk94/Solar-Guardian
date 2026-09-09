package com.kopo.solar.user.controller;

import com.kopo.solar.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    // 아이디 중복확인
    @GetMapping("/check/loginId")
    public boolean checkLoginId(@RequestParam String loginId) {
        return userService.isLoginIdDuplicate(loginId);
    }

    // 이메일 중복확인
    @GetMapping("/check/email")
    public boolean checkEmail(@RequestParam String email) {
        return userService.isEmailDuplicate(email);
    }
}
