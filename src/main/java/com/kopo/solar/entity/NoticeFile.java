package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "notice_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "origin_name", nullable = false, length = 255)
    private String originName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "reg_by", nullable = false, length = 20)
    private String regBy;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "avi", "mkv", "wmv");
    private static final Set<String> PDF_EXTENSIONS   = Set.of("pdf");

    // 원본 파일명에서 확장자만 소문자로 추출
    private String ext() {
        if (originName == null || !originName.contains(".")) return "";
        return originName.substring(originName.lastIndexOf('.') + 1).toLowerCase();
    }

    // 이미지 파일 여부 (화면에 바로 미리보기)
    public boolean isImage() {
        return IMAGE_EXTENSIONS.contains(ext());
    }

    // 동영상 파일 여부
    public boolean isVideo() {
        return VIDEO_EXTENSIONS.contains(ext());
    }

    // PDF 파일 여부
    public boolean isPdf()   {
        return PDF_EXTENSIONS.contains(ext());
    }
}
