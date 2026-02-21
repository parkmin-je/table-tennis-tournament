package com.yourcompany.pingpong.modules.post.controller;

import com.yourcompany.pingpong.domain.BoardCategory;
import com.yourcompany.pingpong.domain.Post;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.post.service.CommentService;
import com.yourcompany.pingpong.modules.post.service.PostService;
import com.yourcompany.pingpong.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;

    /**
     * 게시판 목록 (카테고리별)
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) BoardCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            Model model) {

        Page<Post> posts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            posts = postService.searchPosts(category, keyword, page, size);
            model.addAttribute("keyword", keyword);
        } else if (category != null) {
            posts = postService.getPostsByCategory(category, page, size);
        } else {
            posts = postService.getAllPosts(page, size);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("category", category);
        model.addAttribute("categories", BoardCategory.values());

        return "board/list";
    }

    /**
     * 게시글 상세보기
     */
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", postService.getCommentsByPostId(id));
        return "board/detail";
    }

    /**
     * 게시글 작성 폼 (공지사항은 관리자만)
     */
    @GetMapping("/write")
    public String writeForm(@RequestParam(required = false) BoardCategory category, Model model) {
        model.addAttribute("categories", BoardCategory.values());
        model.addAttribute("selectedCategory", category);
        return "board/write";
    }

    /**
     * 게시글 작성 처리
     */
    @PostMapping("/write")
    public String write(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam BoardCategory category,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User author = userService.getUserByUsername(userDetails.getUsername());

        // ✅ 공지사항은 관리자만 작성 가능 - 권한 체크 수정
        if (category == BoardCategory.NOTICE &&
                !author.getRole().equals("ADMIN") && !author.getRole().equals("ROLE_ADMIN")) {
            log.warn("공지사항 작성 권한 없음 - 사용자: {}, Role: {}", author.getUsername(), author.getRole());
            redirectAttributes.addFlashAttribute("error", "공지사항은 관리자만 작성할 수 있습니다.");
            return "redirect:/board/list";
        }

        Post post = Post.builder()
                .title(title)
                .content(content)
                .category(category)
                .build();

        Post savedPost = postService.createPost(post, author);
        redirectAttributes.addFlashAttribute("message", "게시글이 작성되었습니다.");

        return "redirect:/board/detail/" + savedPost.getId();
    }

    /**
     * 게시글 수정 폼
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        Post post = postService.getPostById(id);
        User currentUser = userService.getUserByUsername(userDetails.getUsername());

        // ✅ 작성자 본인 또는 관리자만 수정 가능 - 권한 체크 수정
        if (!post.getAuthor().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals("ADMIN") && !currentUser.getRole().equals("ROLE_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/board/detail/" + id;
        }

        model.addAttribute("post", post);
        model.addAttribute("categories", BoardCategory.values());
        return "board/edit";
    }

    /**
     * 게시글 수정 처리
     */
    @PostMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam BoardCategory category,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Post post = postService.getPostById(id);
        User currentUser = userService.getUserByUsername(userDetails.getUsername());

        // ✅ 권한 확인 - 권한 체크 수정
        if (!post.getAuthor().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals("ADMIN") && !currentUser.getRole().equals("ROLE_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/board/detail/" + id;
        }

        postService.updatePost(id, title, content, category);
        redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");

        return "redirect:/board/detail/" + id;
    }

    /**
     * 게시글 삭제
     */
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Post post = postService.getPostById(id);
        User currentUser = userService.getUserByUsername(userDetails.getUsername());

        // ✅ 작성자 본인 또는 관리자만 삭제 가능 - 권한 체크 수정
        if (!post.getAuthor().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals("ADMIN") && !currentUser.getRole().equals("ROLE_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "삭제 권한이 없습니다.");
            return "redirect:/board/detail/" + id;
        }

        postService.deletePost(id);
        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");

        return "redirect:/board/list";
    }

    /**
     * 상단 고정 토글 (관리자 전용)
     */
    @PostMapping("/pin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public String togglePin(@PathVariable Long id) {
        postService.togglePin(id);
        return "success";
    }

    /**
     * 좋아요
     */
    @PostMapping("/like/{id}")
    @ResponseBody
    public String likePost(@PathVariable Long id) {
        postService.likePost(id);
        return "success";
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/comment/{postId}")
    public String addComment(
            @PathVariable Long postId,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User author = userService.getUserByUsername(userDetails.getUsername());
        commentService.createComment(postId, content, author);
        redirectAttributes.addFlashAttribute("message", "댓글이 작성되었습니다.");

        return "redirect:/board/detail/" + postId;
    }

    /**
     * 댓글 삭제
     */
    @PostMapping("/comment/delete/{commentId}")
    public String deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long postId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        commentService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("message", "댓글이 삭제되었습니다.");

        return "redirect:/board/detail/" + postId;
    }
}