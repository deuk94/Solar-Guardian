package com.kopo.solar.board.service;

import com.kopo.solar.board.dto.BoardUpdateDto;
import com.kopo.solar.board.dto.BoardWriteDto;
import com.kopo.solar.board.dto.CommentUpdateDto;
import com.kopo.solar.board.dto.CommentWriteDto;
import com.kopo.solar.board.entity.Board;
import com.kopo.solar.board.entity.Comment;
import com.kopo.solar.exception.ForbiddenException;
import com.kopo.solar.exception.NotFoundException;
import com.kopo.solar.board.repository.BoardRepository;
import com.kopo.solar.board.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // 게시글 목록, 페이지네이션
    public Page<Board> findAll(Pageable pageable) {
        return boardRepository.findByDelYn("N", pageable);
    }

    // 게시글 조회, 삭제됐거나 없으면 예외
    public Board findById(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글입니다."));
        if ("Y".equals(board.getDelYn())) {
            throw new NotFoundException("존재하지 않는 게시글입니다.");
        }
        return board;
    }

    // 상세 조회하면서 조회수 1 증가
    @Transactional
    public Board viewDetail(Long boardId) {
        Board board = findById(boardId);
        board.increaseViewCnt();
        return board;
    }

    // 이전 글
    public Board findPrev(Long boardId) {
        return boardRepository.findFirstByDelYnAndBoardIdGreaterThanOrderByBoardIdAsc("N", boardId).orElse(null);
    }

    // 다음 글
    public Board findNext(Long boardId) {
        return boardRepository.findFirstByDelYnAndBoardIdLessThanOrderByBoardIdDesc("N", boardId).orElse(null);
    }

    // 게시글 작성
    @Transactional
    public Board write(BoardWriteDto dto, String regBy) {
        Board board = Board.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .viewCnt(0L)
                .build();
        board.stampCreator(regBy);
        return boardRepository.save(board);
    }

    // 게시글 수정
    @Transactional
    public void update(Long boardId, BoardUpdateDto dto, String modBy) {
        Board board = findById(boardId);
        board.update(dto.getTitle(), dto.getContent(), modBy);
    }

    // 게시글 삭제
    @Transactional
    public void delete(Long boardId, String delBy) {
        Board board = findById(boardId);
        board.softDelete(delBy);
    }

    // 게시글의 댓글 목록
    public List<Comment> findComments(Long boardId) {
        Board board = findById(boardId);
        return commentRepository.findByBoardAndDelYnOrderByCommentIdAsc(board, "N");
    }

    // 댓글 작성
    @Transactional
    public Comment writeComment(Long boardId, CommentWriteDto dto, String regBy) {
        Board board = findById(boardId);
        Comment comment = Comment.builder()
                .board(board)
                .content(dto.getContent())
                .build();
        comment.stampCreator(regBy);
        return commentRepository.save(comment);
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long commentId, CommentUpdateDto dto, String loginId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글입니다."));
        if ("Y".equals(comment.getDelYn())) {
            throw new NotFoundException("존재하지 않는 댓글입니다.");
        }
        if (!comment.getRegBy().equals(loginId) && !isAdmin) {
            throw new ForbiddenException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }
        comment.update(dto.getContent(), loginId);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, String loginId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글입니다."));
        if ("Y".equals(comment.getDelYn())) {
            throw new NotFoundException("존재하지 않는 댓글입니다.");
        }
        if (!comment.getRegBy().equals(loginId) && !isAdmin) {
            throw new ForbiddenException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }
        comment.softDelete(loginId);
    }
}
