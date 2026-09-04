package com.kopo.solar.service;

import com.kopo.solar.dto.UserJoinDto;
import com.kopo.solar.dto.UserLoginDto;
import com.kopo.solar.dto.UserUpdateDto;
import com.kopo.solar.entity.Role;
import com.kopo.solar.entity.RoleType;
import com.kopo.solar.entity.User;
import com.kopo.solar.repository.RoleRepository;
import com.kopo.solar.repository.UserRepository;
import com.kopo.solar.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // 회원가입 - 아이디/이메일 중복 체크 후 ROLE_USER 권한으로 등록
    @Transactional
    public void join(UserJoinDto dto) {
        if (userRepository.existsByLoginId(dto.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Role role = roleRepository.findByRoleNm(RoleType.ROLE_USER.name())
                .orElseThrow(() -> new IllegalStateException("ROLE_USER 권한이 존재하지 않습니다."));

        User user = User.builder()
                .role(role)
                .loginId(dto.getLoginId())
                .pwd(PasswordUtil.encode(dto.getPwd()))
                .name(dto.getName())
                .gender(dto.getGender())
                .telNo(dto.getTelNo())
                .address(dto.getAddress())
                .email(dto.getEmail())
                .build();

        user.setRegBy(dto.getLoginId());
        user.setModBy(dto.getLoginId());

        userRepository.save(user);
    }

    // 로그인 - 아이디/비밀번호 검증, 탈퇴 회원은 거부
    public User login(UserLoginDto dto) {
        User user = userRepository.findByLoginIdWithRole(dto.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if ("Y".equals(user.getDelYn())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }
        if (!PasswordUtil.matches(dto.getPwd(), user.getPwd())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    // 회원 조회, 없으면 예외
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // 회원정보 수정 - 비밀번호는 입력했을 때만 갱신 (빈 값이면 기존 값 유지)
    @Transactional
    public void update(Long userId, UserUpdateDto dto, String modBy) {
        User user = findById(userId);

        if (dto.getPwd() != null && !dto.getPwd().isBlank()) {
            user.setPwd(PasswordUtil.encode(dto.getPwd()));
        }

        user.setTelNo(dto.getTelNo());
        user.setAddress(dto.getAddress());
        user.setEmail(dto.getEmail());
        user.setModBy(modBy);
    }

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void withdraw(Long userId, String delBy) {
        User user = findById(userId);
        user.setDelYn("Y");
        user.setDelDt(LocalDateTime.now());
        user.setDelBy(delBy);
    }

    // 로그인 id 중복 여부 (Ajax 중복확인용)
    public boolean isLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    // 이메일 중복 여부 (Ajax 중복확인용)
    public boolean isEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
