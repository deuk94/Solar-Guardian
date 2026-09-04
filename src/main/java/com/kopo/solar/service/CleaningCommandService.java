package com.kopo.solar.service;

import com.kopo.solar.entity.CleaningCommand;
import com.kopo.solar.entity.CleaningStatus;
import com.kopo.solar.entity.User;
import com.kopo.solar.repository.CleaningCommandRepository;
import com.kopo.solar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CleaningCommandService {

    private final CleaningCommandRepository cleaningCommandRepository;
    private final UserRepository userRepository;

    @Value("${cleaning.auto-request-cooldown-minutes}")
    private long cooldownMinutes;

    // 회원이 직접 세척 요청, 이미 PENDING 있으면 거부
    @Transactional
    public CleaningCommand request(Long userId) {
        if (cleaningCommandRepository.findFirstByUser_UserIdAndStatus(userId, CleaningStatus.PENDING).isPresent()) {
            throw new IllegalArgumentException("이미 진행 중인 세척 요청이 있습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        CleaningCommand command = CleaningCommand.builder()
                .user(user)
                .status(CleaningStatus.PENDING)
                .build();
        return cleaningCommandRepository.save(command);
    }

    // 세척 이력, 페이지네이션
    public Page<CleaningCommand> findByUser(Long userId, Pageable pageable) {
        return cleaningCommandRepository.findByUser_UserIdOrderByRequestedAtDesc(userId, pageable);
    }

    // 가장 최근 세척 명령
    public CleaningCommand findLatest(Long userId) {
        return cleaningCommandRepository.findFirstByUser_UserIdOrderByRequestedAtDesc(userId).orElse(null);
    }

    // AI가 오염 연속 감지 시 자동으로 세척 요청, 완료 직후 쿨다운 중이면 거부
    @Transactional
    public CleaningCommand requestAuto(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        CleaningCommand latest = findLatest(user.getUserId());
        if (latest != null && latest.getStatus() == CleaningStatus.DONE
                && latest.getCompletedAt() != null
                && latest.getCompletedAt().isAfter(LocalDateTime.now().minusMinutes(cooldownMinutes))) {
            throw new IllegalArgumentException("최근 세척 완료 후 대기 시간(쿨다운) 중입니다.");
        }
        return request(user.getUserId());
    }

    // 대기 중인 세척 명령 (라즈베리파이 폴링용)
    public Optional<CleaningCommand> findPending(String loginId) {
        return cleaningCommandRepository.findFirstByUser_LoginIdAndStatusOrderByRequestedAtAsc(
                loginId, CleaningStatus.PENDING);
    }

    // 세척 완료 처리
    @Transactional
    public void complete(Long commandId) {
        CleaningCommand command = cleaningCommandRepository.findById(commandId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세척 명령입니다."));
        command.setStatus(CleaningStatus.DONE);
        command.setCompletedAt(LocalDateTime.now());
    }
}
