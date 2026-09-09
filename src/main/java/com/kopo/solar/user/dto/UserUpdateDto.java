package com.kopo.solar.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDto {

    // 비워두면 기존 비밀번호 유지, 입력했을 때만 8~20자 제약 (@Size 대신 @Pattern 쓴 이유는 TROUBLESHOOTING.md)
    @Pattern(regexp = "^$|.{8,20}$", message = "비밀번호는 8~20자여야 합니다.")
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
