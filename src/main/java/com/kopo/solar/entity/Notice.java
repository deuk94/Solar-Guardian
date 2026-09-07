package com.kopo.solar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "view_cnt", nullable = false)
    private Long viewCnt;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeFile> files = new ArrayList<>();

    public void update(String title, String content, String modBy) {
        this.title = title;
        this.content = content;
        touch(modBy);
    }

    public void increaseViewCnt() {
        this.viewCnt++;
    }
}
