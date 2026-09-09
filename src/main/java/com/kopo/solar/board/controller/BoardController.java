package com.kopo.solar.board.controller;

import com.kopo.solar.board.dto.BoardUpdateDto;
import com.kopo.solar.board.dto.BoardWriteDto;
import com.kopo.solar.board.entity.Board;
import com.kopo.solar.board.entity.Comment;
import com.kopo.solar.user.entity.RoleType;
import com.kopo.solar.user.entity.User;
import com.kopo.solar.board.service.BoardService;
import com.kopo.solar.common.PaginationUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 게시글 목록
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Board> boardPage = boardService.findAll(PageRequest.of(page, 10, Sort.by("boardId").descending()));
        model.addAttribute("boardPage", boardPage);
        model.addAttribute("pageBlock", PaginationUtil.of(boardPage));
        return "board/list";
    }

    // 게시글 상세
    @GetMapping("/{boardId}")
    public String detail(@PathVariable Long boardId, Model model, RedirectAttributes ra) {
        try {
            Board board = boardService.viewDetail(boardId);
            List<Comment> comments = boardService.findComments(boardId);
            model.addAttribute("board", board);
            model.addAttribute("comments", comments);
            return "board/detail";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/list";
        }
    }

    // 작성 폼
    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isLoggedIn(session)) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/board/list";
        }
        model.addAttribute("boardWriteDto", new BoardWriteDto());
        return "board/write";
    }

    // 작성 처리
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute BoardWriteDto dto, BindingResult result,
                        HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/board/list";
        }
        if (result.hasErrors()) {
            return "board/write";
        }
        User loginUser = (User) session.getAttribute("loginUser");
        Board board = boardService.write(dto, loginUser.getLoginId());
        ra.addFlashAttribute("message", "등록되었습니다.");
        return "redirect:/board/" + board.getBoardId();
    }

    // 수정 폼
    @GetMapping("/{boardId}/edit")
    public String editForm(@PathVariable Long boardId, HttpSession session, Model model, RedirectAttributes ra) {
        try {
            Board board = boardService.findById(boardId);
            if (!canManage(session, board)) {
                ra.addFlashAttribute("error", "본인이 작성한 글만 수정할 수 있습니다.");
                return "redirect:/board/" + boardId;
            }
            BoardUpdateDto dto = new BoardUpdateDto();
            dto.setTitle(board.getTitle());
            dto.setContent(board.getContent());
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            model.addAttribute("boardUpdateDto", dto);
            return "board/edit";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/list";
        }
    }

    // 수정 처리
    @PostMapping("/{boardId}/edit")
    public String edit(@PathVariable Long boardId,
                       @Valid @ModelAttribute BoardUpdateDto dto, BindingResult result,
                       HttpSession session, Model model, RedirectAttributes ra) {
        Board board;
        try {
            board = boardService.findById(boardId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/list";
        }
        if (!canManage(session, board)) {
            ra.addFlashAttribute("error", "본인이 작성한 글만 수정할 수 있습니다.");
            return "redirect:/board/" + boardId;
        }
        if (result.hasErrors()) {
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "board/edit";
        }
        User loginUser = (User) session.getAttribute("loginUser");
        boardService.update(boardId, dto, loginUser.getLoginId());
        ra.addFlashAttribute("message", "수정되었습니다.");
        return "redirect:/board/" + boardId;
    }

    // 삭제 처리
    @PostMapping("/{boardId}/delete")
    public String delete(@PathVariable Long boardId, HttpSession session, RedirectAttributes ra) {
        Board board;
        try {
            board = boardService.findById(boardId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/board/list";
        }
        if (!canManage(session, board)) {
            ra.addFlashAttribute("error", "본인이 작성한 글만 삭제할 수 있습니다.");
            return "redirect:/board/" + boardId;
        }
        User loginUser = (User) session.getAttribute("loginUser");
        boardService.delete(boardId, loginUser.getLoginId());
        ra.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/board/list";
    }

    // 로그인 여부
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loginUser") != null;
    }

    // 관리자 여부
    private boolean isAdmin(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        return loginUser != null && RoleType.ROLE_ADMIN.name().equals(loginUser.getRole().getRoleNm());
    }

    // 작성자 본인 또는 관리자만 수정/삭제 가능
    private boolean canManage(HttpSession session, Board board) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return false;
        return loginUser.getLoginId().equals(board.getRegBy()) || isAdmin(session);
    }
}
