package com.kopo.solar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SensorDataDto {

    @NotBlank
    private String loginId;

    @NotNull
    private BigDecimal current;

    @NotNull
    private BigDecimal voltage;

    private LocalDateTime measuredAt;
}
