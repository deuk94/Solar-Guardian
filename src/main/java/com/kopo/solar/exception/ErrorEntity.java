package com.kopo.solar.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ErrorEntity {

    private String errorCode;

    private String message;

    private Map<String, String> validation;

    public ErrorEntity(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
