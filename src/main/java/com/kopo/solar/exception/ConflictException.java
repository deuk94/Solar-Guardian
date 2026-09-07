package com.kopo.solar.exception;

// 중복 가입, 이미 진행 중인 세척 요청, 쿨다운 중 재요청처럼 지금 상태와 충돌하는 요청일 때
public class ConflictException extends IllegalArgumentException {
    public ConflictException(String message) {
        super(message);
    }
}
