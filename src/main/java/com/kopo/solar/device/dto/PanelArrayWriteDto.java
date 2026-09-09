package com.kopo.solar.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class PanelArrayWriteDto {

    @NotBlank
    @Size(max = 100)
    private String arrayName;

    @NotNull
    @Positive
    private Integer panelCount;

    @NotNull
    @Positive
    private Integer designCapacityW;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate installedDate;

    @NotBlank
    @Size(max = 255)
    private String installLocation;
}
