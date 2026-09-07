package com.kopo.solar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// /api/** REST 컨트롤러에서 던진 예외를 한 곳에서 받아 ErrorEntity(errorCode, message)로 변환
// 화면(Thymeleaf) 컨트롤러는 대상이 아님 - 거긴 실패 시 리다이렉트 경로가 케이스마다 달라서 지금처럼 try-catch 유지
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorEntity> notFoundExceptionHandle(NotFoundException ex) {
        ErrorEntity errorEntity = new ErrorEntity("NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorEntity> conflictExceptionHandle(ConflictException ex) {
        ErrorEntity errorEntity = new ErrorEntity("CONFLICT", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorEntity> forbiddenExceptionHandle(ForbiddenException ex) {
        ErrorEntity errorEntity = new ErrorEntity("FORBIDDEN", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorEntity> authenticationFailedExceptionHandle(AuthenticationFailedException ex) {
        ErrorEntity errorEntity = new ErrorEntity("AUTHENTICATION_FAILED", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEntity> methodArgsNotValidExceptionHandle(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );
        ErrorEntity errorEntity = new ErrorEntity("INVALID_INPUT_VALUE", "형식이 맞지 않습니다.", errors);
        return new ResponseEntity<>(errorEntity, HttpStatus.BAD_REQUEST);
    }

    // 아직 세분화 안 하고 IllegalArgumentException을 직접 던지는 곳이 있으면 여기서 받음
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorEntity> illegalArgumentExceptionHandle(IllegalArgumentException ex) {
        ErrorEntity errorEntity = new ErrorEntity("INVALID_INPUT_VALUE", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEntity> handleExceptionHandle(Exception ex) {
        ErrorEntity errorEntity = new ErrorEntity("INTERNAL_SERVER_ERROR", ex.getMessage());
        return new ResponseEntity<>(errorEntity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
