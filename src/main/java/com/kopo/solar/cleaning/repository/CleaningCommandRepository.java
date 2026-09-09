package com.kopo.solar.cleaning.repository;

import com.kopo.solar.cleaning.entity.CleaningCommand;
import com.kopo.solar.cleaning.entity.CleaningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CleaningCommandRepository extends JpaRepository<CleaningCommand, Long> {

    // 회원별 세척 이력, 최신순 페이지네이션
    Page<CleaningCommand> findByUser_UserIdOrderByRequestedAtDesc(Long userId, Pageable pageable);

    // 회원의 가장 최근 세척 명령
    Optional<CleaningCommand> findFirstByUser_UserIdOrderByRequestedAtDesc(Long userId);

    // 회원의 특정 상태(PENDING 등) 명령 하나 - 중복 요청 체크용
    Optional<CleaningCommand> findFirstByUser_UserIdAndStatus(Long userId, CleaningStatus status);

    // 로그인 id 기준으로 특정 상태 명령 중 가장 오래된 것 - 라즈베리파이 폴링용
    Optional<CleaningCommand> findFirstByUser_LoginIdAndStatusOrderByRequestedAtAsc(String loginId, CleaningStatus status);
}
