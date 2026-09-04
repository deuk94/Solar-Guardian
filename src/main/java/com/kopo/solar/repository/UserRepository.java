package com.kopo.solar.repository;

import com.kopo.solar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 id로 회원 조회
    Optional<User> findByLoginId(String loginId);

    // 권한까지 즉시 로딩해서 회원 조회 (N+1 방지)
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.loginId = :loginId")
    Optional<User> findByLoginIdWithRole(String loginId);

    // 로그인 id 중복 여부
    boolean existsByLoginId(String loginId);

    // 이메일 중복 여부
    boolean existsByEmail(String email);
}
