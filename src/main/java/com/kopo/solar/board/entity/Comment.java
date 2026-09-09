package com.kopo.solar.board.entity;

import com.kopo.solar.common.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    public void update(String content, String modBy) {
        this.content = content;
        touch(modBy);
    }
}
