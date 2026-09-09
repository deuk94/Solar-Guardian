package com.kopo.solar.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserJoinDto {

    @NotBlank
    @Size(min = 4, max = 30)
    private String loginId;

    @NotBlank
    @Size(min = 8, max = 20)
    private String pwd;

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotBlank
    @Pattern(regexp = "[MF]", message = "M 또는 F만 입력 가능합니다.")
    private String gender;

    @NotBlank
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String telNo;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
}
