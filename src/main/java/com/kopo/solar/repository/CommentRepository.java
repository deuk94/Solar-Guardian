package com.kopo.solar.repository;

import com.kopo.solar.entity.Board;
import com.kopo.solar.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글의 댓글 목록, 삭제 안 된 것만, 등록순
    List<Comment> findByBoardAndDelYnOrderByCommentIdAsc(Board board, String delYn);
}
