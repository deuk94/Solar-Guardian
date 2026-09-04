package com.kopo.solar.repository;

import com.kopo.solar.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // 권한명(ROLE_USER 등)으로 권한 조회
    Optional<Role> findByRoleNm(String roleNm);
}
