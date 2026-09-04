package com.kopo.solar.dto;

import com.kopo.solar.entity.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class DeviceUpdateDto {

    @NotBlank
    @Size(max = 100)
    private String deviceName;

    @NotBlank
    @Size(max = 50)
    private String serialNo;

    @NotBlank
    @Size(max = 20)
    private String firmwareVersion;

    @NotBlank
    @Size(max = 50)
    private String modelName;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate manufacturedDate;

    @NotNull
    private DeviceStatus status;
}
