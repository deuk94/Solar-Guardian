package com.kopo.solar.notice.controller;

import com.kopo.solar.notice.dto.NoticeUpdateDto;
import com.kopo.solar.notice.dto.NoticeWriteDto;
import com.kopo.solar.notice.entity.Notice;
import com.kopo.solar.notice.entity.NoticeFile;
import com.kopo.solar.user.entity.RoleType;
import com.kopo.solar.user.entity.User;
import com.kopo.solar.notice.service.NoticeService;
import com.kopo.solar.common.PaginationUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 공지사항 목록 (전체 공개)
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Notice> noticePage = noticeService.findAll(PageRequest.of(page, 10, Sort.by("noticeId").descending()));
        model.addAttribute("noticePage", noticePage);
        model.addAttribute("pageBlock", PaginationUtil.of(noticePage));
        return "notice/list";
    }

    // 공지사항 상세 (조회수 증가, 전체 공개)
    @GetMapping("/{noticeId}")
    public String detail(@PathVariable Long noticeId, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("notice", noticeService.viewDetail(noticeId));
            model.addAttribute("prevNotice", noticeService.findPrev(noticeId));
            model.addAttribute("nextNotice", noticeService.findNext(noticeId));
            return "notice/detail";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/list";
        }
    }

    // 작성 폼
    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isLoggedIn(session)) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/notice/list";
        }
        model.addAttribute("noticeWriteDto", new NoticeWriteDto());
        return "notice/write";
    }

    // 작성 처리, 첨부파일 업로드
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute NoticeWriteDto dto, BindingResult result,
                         HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/notice/list";
        }
        if (result.hasErrors()) {
            return "notice/write";
        }
        User loginUser = (User) session.getAttribute("loginUser");
        Notice notice = noticeService.write(dto, loginUser.getLoginId());
        ra.addFlashAttribute("message", "등록되었습니다.");
        return "redirect:/notice/" + notice.getNoticeId();
    }

    // 수정 폼
    @GetMapping("/{noticeId}/edit")
    public String editForm(@PathVariable Long noticeId, HttpSession session, Model model, RedirectAttributes ra) {
        try {
            Notice notice = noticeService.findById(noticeId);
            if (!canManage(session, notice)) {
                ra.addFlashAttribute("error", "본인이 작성한 글만 수정할 수 있습니다.");
                return "redirect:/notice/" + noticeId;
            }
            NoticeUpdateDto dto = new NoticeUpdateDto();
            dto.setTitle(notice.getTitle());
            dto.setContent(notice.getContent());
            model.addAttribute("noticeId", noticeId);
            model.addAttribute("notice", notice);
            model.addAttribute("noticeUpdateDto", dto);
            return "notice/edit";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/list";
        }
    }

    // 수정 처리, 첨부파일 추가/삭제
    @PostMapping("/{noticeId}/edit")
    public String edit(@PathVariable Long noticeId,
                        @Valid @ModelAttribute NoticeUpdateDto dto, BindingResult result,
                        HttpSession session, Model model, RedirectAttributes ra) {
        Notice notice;
        try {
            notice = noticeService.findById(noticeId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/list";
        }
        if (!canManage(session, notice)) {
            ra.addFlashAttribute("error", "본인이 작성한 글만 수정할 수 있습니다.");
            return "redirect:/notice/" + noticeId;
        }
        if (result.hasErrors()) {
            model.addAttribute("noticeId", noticeId);
            model.addAttribute("notice", notice);
            return "notice/edit";
        }
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            noticeService.update(noticeId, dto, loginUser.getLoginId());
            ra.addFlashAttribute("message", "수정되었습니다.");
            return "redirect:/notice/" + noticeId;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/" + noticeId + "/edit";
        }
    }

    // 삭제 처리
    @PostMapping("/{noticeId}/delete")
    public String delete(@PathVariable Long noticeId, HttpSession session, RedirectAttributes ra) {
        Notice notice;
        try {
            notice = noticeService.findById(noticeId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/list";
        }
        if (!canManage(session, notice)) {
            ra.addFlashAttribute("error", "본인이 작성한 글만 삭제할 수 있습니다.");
            return "redirect:/notice/" + noticeId;
        }
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            noticeService.delete(noticeId, loginUser.getLoginId());
            ra.addFlashAttribute("message", "삭제되었습니다.");
            return "redirect:/notice/list";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notice/" + noticeId;
        }
    }

    // 첨부파일 다운로드
    @GetMapping("/file/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        NoticeFile file = noticeService.findFile(fileId);
        Resource resource = new FileSystemResource(file.getFilePath());

        String encodedName = URLEncoder.encode(file.getOriginName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // 첨부파일 인라인 보기
    @GetMapping("/file/{fileId}/view")
    public ResponseEntity<Resource> viewImage(@PathVariable Long fileId) {
        NoticeFile file = noticeService.findFile(fileId);
        Resource resource = new FileSystemResource(file.getFilePath());
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getOriginName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
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
    private boolean canManage(HttpSession session, Notice notice) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return false;
        }
        return loginUser.getLoginId().equals(notice.getRegBy()) || isAdmin(session);
    }
}
