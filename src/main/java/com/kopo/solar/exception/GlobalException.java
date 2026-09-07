package com.kopo.solar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

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
