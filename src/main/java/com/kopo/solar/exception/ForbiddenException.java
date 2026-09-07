package com.kopo.solar.exception;

// 본인 소유가 아니거나 권한이 없는 리소스에 접근했을 때
public class ForbiddenException extends IllegalArgumentException {
    public ForbiddenException(String message) {
        super(message);
    }
}
