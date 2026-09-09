package com.kopo.solar.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDto {

    @NotBlank
    private String loginId;

    @NotBlank
    private String pwd;
}
