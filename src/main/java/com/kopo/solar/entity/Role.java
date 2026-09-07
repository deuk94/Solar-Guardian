package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_nm", nullable = false, length = 10)
    private String roleNm;

    @Column(name = "role_desc", nullable = false, length = 100)
    private String roleDesc;
}
