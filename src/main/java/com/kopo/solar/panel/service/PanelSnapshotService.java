package com.kopo.solar.panel.service;

import com.kopo.solar.panel.entity.PanelSnapshot;
import com.kopo.solar.user.entity.User;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.panel.repository.PanelSnapshotRepository;
import com.kopo.solar.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelSnapshotService {

    private final PanelSnapshotRepository panelSnapshotRepository;
    private final UserRepository userRepository;

    @Value("${panel.upload-dir}")
    private String uploadDir;

    // 라즈베리파이가 올린 패널 스냅샷 저장
    @Transactional
    public PanelSnapshot save(String loginId, MultipartFile file, LocalDateTime capturedAt) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        Path stored = store(file);

        PanelSnapshot snapshot = PanelSnapshot.builder()
                .user(user)
                .filePath(stored.toString())
                .capturedAt(capturedAt != null ? capturedAt : LocalDateTime.now())
                .build();

        return panelSnapshotRepository.save(snapshot);
    }

    // 최신 스냅샷
    public PanelSnapshot findLatest(Long userId) {
        return panelSnapshotRepository.findFirstByUser_UserIdOrderByCapturedAtDesc(userId).orElse(null);
    }

    // 스냅샷 조회, 없으면 예외
    public PanelSnapshot findById(Long snapshotId) {
        return panelSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 스냅샷입니다."));
    }

    // 업로드 파일을 UUID 이름으로 저장
    private Path store(MultipartFile file) {
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            String originName = file.getOriginalFilename();
            String ext = "";
            if (originName != null && originName.contains(".")) {
                ext = originName.substring(originName.lastIndexOf("."));
            }
            Path target = dir.resolve(UUID.randomUUID() + ext);
            file.transferTo(target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("스냅샷 저장에 실패했습니다.", e);
        }
    }
}
