package com.kopo.solar.controller;

import com.kopo.solar.dto.CommentUpdateDto;
import com.kopo.solar.dto.CommentWriteDto;
import com.kopo.solar.entity.Comment;
import com.kopo.solar.entity.RoleType;
import com.kopo.solar.entity.User;
import com.kopo.solar.service.BoardService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardApiController {

    private final BoardService boardService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 댓글 작성 (Ajax)
    @PostMapping("/{boardId}/comment")
    public ResponseEntity<Map<String, Object>> writeComment(
            @PathVariable Long boardId,
            @Valid @RequestBody CommentWriteDto dto,
            BindingResult result,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        if (result.hasErrors()) {
            String msg = result.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        try {
            Comment comment = boardService.writeComment(boardId, dto, loginUser.getLoginId());
            return ResponseEntity.ok(Map.of(
                    "commentId", comment.getCommentId(),
                    "content",   comment.getContent(),
                    "regBy",     comment.getRegBy(),
                    "regDt",     comment.getRegDt().format(FMT)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 댓글 수정 (Ajax, 작성자 본인 또는 관리자)
    @PutMapping("/comment/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateDto dto,
            BindingResult result,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        if (result.hasErrors()) {
            String msg = result.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        boolean isAdmin = RoleType.ROLE_ADMIN.name().equals(loginUser.getRole().getRoleNm());
        try {
            boardService.updateComment(commentId, dto, loginUser.getLoginId(), isAdmin);
            return ResponseEntity.ok(Map.of("content", dto.getContent()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 댓글 삭제 (Ajax, 작성자 본인 또는 관리자)
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        boolean isAdmin = RoleType.ROLE_ADMIN.name().equals(loginUser.getRole().getRoleNm());
        try {
            boardService.deleteComment(commentId, loginUser.getLoginId(), isAdmin);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
