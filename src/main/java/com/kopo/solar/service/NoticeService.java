package com.kopo.solar.service;

import com.kopo.solar.dto.NoticeUpdateDto;
import com.kopo.solar.dto.NoticeWriteDto;
import com.kopo.solar.entity.Notice;
import com.kopo.solar.entity.NoticeFile;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.repository.NoticeFileRepository;
import com.kopo.solar.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeFileRepository noticeFileRepository;
    private final FileStorageService fileStorageService;

    // 공지사항 목록, 페이지네이션
    public Page<Notice> findAll(Pageable pageable) {
        return noticeRepository.findByDelYn("N", pageable);
    }

    // 공지사항 조회, 삭제됐거나 없으면 예외
    public Notice findById(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 공지사항입니다."));
        if ("Y".equals(notice.getDelYn())) {
            throw new NotFoundException("존재하지 않는 공지사항입니다.");
        }
        return notice;
    }

    // 상세 조회하면서 조회수 1 증가
    @Transactional
    public Notice viewDetail(Long noticeId) {
        Notice notice = findById(noticeId);
        notice.setViewCnt(notice.getViewCnt() + 1);
        return notice;
    }

    // 이전 글
    public Notice findPrev(Long noticeId) {
        return noticeRepository.findFirstByDelYnAndNoticeIdGreaterThanOrderByNoticeIdAsc("N", noticeId).orElse(null);
    }

    // 다음 글
    public Notice findNext(Long noticeId) {
        return noticeRepository.findFirstByDelYnAndNoticeIdLessThanOrderByNoticeIdDesc("N", noticeId).orElse(null);
    }

    // 공지사항 작성, 첨부파일 있으면 같이 저장
    @Transactional
    public Notice write(NoticeWriteDto dto, String regBy) {
        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .viewCnt(0L)
                .build();
        notice.setRegBy(regBy);
        notice.setModBy(regBy);

        noticeRepository.save(notice);
        addFiles(notice, dto.getFiles(), regBy);

        return notice;
    }

    // 공지사항 수정, 지정된 첨부파일 삭제 + 새 첨부파일 추가
    @Transactional
    public void update(Long noticeId, NoticeUpdateDto dto, String modBy) {
        Notice notice = findById(noticeId);

        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setModBy(modBy);

        if (dto.getDeleteFileIds() != null) {
            List<NoticeFile> targets = notice.getFiles().stream()
                    .filter(f -> dto.getDeleteFileIds().contains(f.getFileId()))
                    .toList();
            for (NoticeFile file : targets) {
                fileStorageService.delete(file.getFilePath());
                notice.getFiles().remove(file);
            }
        }

        addFiles(notice, dto.getFiles(), modBy);
    }

    // 공지사항 소프트 삭제, 첨부파일은 물리 파일까지 함께 삭제 (notice_file은 hard delete 대상)
    @Transactional
    public void delete(Long noticeId, String delBy) {
        Notice notice = findById(noticeId);

        for (NoticeFile file : notice.getFiles()) {
            fileStorageService.delete(file.getFilePath());
        }
        notice.getFiles().clear();

        notice.setDelYn("Y");
        notice.setDelDt(LocalDateTime.now());
        notice.setDelBy(delBy);
    }

    // 첨부파일 조회
    public NoticeFile findFile(Long fileId) {
        return noticeFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 첨부파일입니다."));
    }

    // 업로드된 파일들을 저장하고 공지사항에 첨부파일로 연결
    private void addFiles(Notice notice, List<MultipartFile> files, String regBy) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            
            Path stored = fileStorageService.store(file);
            NoticeFile noticeFile = NoticeFile.builder()
                    .notice(notice)
                    .originName(file.getOriginalFilename())
                    .storedName(stored.getFileName().toString())
                    .filePath(stored.toString())
                    .fileSize(file.getSize())
                    .regBy(regBy)
                    .build();
            notice.getFiles().add(noticeFile);
        }
    }
}
