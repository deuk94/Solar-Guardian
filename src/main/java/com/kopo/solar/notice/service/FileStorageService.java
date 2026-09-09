package com.kopo.solar.notice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir);
    }

    // 업로드 파일을 UUID 이름으로 저장하고 저장 경로 반환
    public Path store(MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);

            String originName = file.getOriginalFilename();
            String ext = "";
            if (originName != null && originName.contains(".")) {
                ext = originName.substring(originName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID() + ext;

            Path target = uploadDir.resolve(storedName);
            file.transferTo(target);

            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
        }
    }

    // 저장된 파일 실제 삭제
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제에 실패했습니다.", e);
        }
    }
}
