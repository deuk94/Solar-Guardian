package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "login_id", nullable = false, length = 30, updatable = false)
    private String loginId;

    @Column(name = "pwd", nullable = false, length = 255)
    private String pwd;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "gender", nullable = false, columnDefinition = "CHAR(1)")
    private String gender;

    @Column(name = "tel_no", nullable = false, length = 15)
    private String telNo;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "email", nullable = false, length = 100)
    private String email;
}
