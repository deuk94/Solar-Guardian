package com.kopo.solar.notice.repository;

import com.kopo.solar.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 삭제여부별 공지사항 목록, 페이지네이션
    Page<Notice> findByDelYn(String delYn, Pageable pageable);

    // 다음 글
    Optional<Notice> findFirstByDelYnAndNoticeIdGreaterThanOrderByNoticeIdAsc(String delYn, Long noticeId);

    // 이전 글
    Optional<Notice> findFirstByDelYnAndNoticeIdLessThanOrderByNoticeIdDesc(String delYn, Long noticeId);
}
