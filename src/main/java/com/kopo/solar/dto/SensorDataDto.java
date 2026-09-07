package com.kopo.solar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensorDataDto {

    @NotBlank
    private String loginId;

    @NotNull
    private BigDecimal current;

    @NotNull
    private BigDecimal voltage;

    private LocalDateTime measuredAt;
}
