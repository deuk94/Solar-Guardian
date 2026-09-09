package com.kopo.solar.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class NoticeUpdateDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    private List<Long> deleteFileIds;

    private List<MultipartFile> files;
}
