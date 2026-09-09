package com.kopo.solar.notice.repository;

import com.kopo.solar.notice.entity.NoticeFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeFileRepository extends JpaRepository<NoticeFile, Long> {
}
