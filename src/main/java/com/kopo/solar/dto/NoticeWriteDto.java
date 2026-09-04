package com.kopo.solar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class NoticeWriteDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    private List<MultipartFile> files;
}
