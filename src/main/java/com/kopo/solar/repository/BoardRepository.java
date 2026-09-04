package com.kopo.solar.repository;

import com.kopo.solar.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 삭제여부별 게시글 목록, 페이지네이션
    Page<Board> findByDelYn(String delYn, Pageable pageable);

    // 다음 글(id가 더 큰 것 중 가장 가까운 글)
    Optional<Board> findFirstByDelYnAndBoardIdGreaterThanOrderByBoardIdAsc(String delYn, Long boardId);

    // 이전 글(id가 더 작은 것 중 가장 가까운 글)
    Optional<Board> findFirstByDelYnAndBoardIdLessThanOrderByBoardIdDesc(String delYn, Long boardId);
}
