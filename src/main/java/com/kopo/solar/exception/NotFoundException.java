package com.kopo.solar.exception;

// 요청한 리소스(회원/기기/게시글 등)가 존재하지 않을 때
// IllegalArgumentException을 상속해서, 아직 이걸로 안 바꾼 화면 컨트롤러의 기존 catch 블록도 계속 잡을 수 있게 함
public class NotFoundException extends IllegalArgumentException {
    public NotFoundException(String message) {
        super(message);
    }
}
