package com.kopo.solar.exception;

// 로그인 시 아이디/비밀번호 불일치, 탈퇴 회원 로그인 시도 등
public class AuthenticationFailedException extends IllegalArgumentException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
