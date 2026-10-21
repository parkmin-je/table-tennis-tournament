package com.parkminje.pingpong.board;

import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private static final String VIEWED = "viewedPosts";

    private final PostService postService;

    @Getter
    @Setter
    public static class PostForm {
        @NotNull(message = "게시판을 선택하세요.")
        private BoardCategory category = BoardCategory.FREE;
        @NotBlank(message = "제목을 입력하세요.")
        @Size(max = 200, message = "제목은 200자 이내로 입력하세요.")
        private String title;
        @NotBlank(message = "내용을 입력하세요.")
        @Size(max = 10000, message = "내용은 10000자 이내로 입력하세요.")
        private String content;
    }

    @GetMapping("/board")
    public String list(@RequestParam(required = false) BoardCategory category, @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", postService.search(category, q, page));
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("categories", BoardCategory.values());
        return "board/list";
    }

    @GetMapping("/board/{id:\\d+}")
    public String detail(@PathVariable Long id, @LoginUser User user, HttpSession session, Model model) {
        @SuppressWarnings("unchecked")
        Set<Long> viewed = (Set<Long>) session.getAttribute(VIEWED);
        if (viewed == null) {
            viewed = new HashSet<>();
        }
        if (viewed.add(id)) {
            postService.increaseView(id);
            session.setAttribute(VIEWED, viewed);
        }
        Post post = postService.get(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", postService.comments(id));
        model.addAttribute("liked", postService.likedBy(id, user));
        model.addAttribute("canEdit", user != null && (post.isWrittenBy(user) || user.isStaff()));
        model.addAttribute("currentUser", user);
        return "board/detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/board/write")
    public String writeForm(@RequestParam(required = false) BoardCategory category, Model model) {
        PostForm form = new PostForm();
        if (category != null) {
            form.setCategory(category);
        }
        model.addAttribute("form", form);
        model.addAttribute("categories", BoardCategory.values());
        return "board/form";
    }

    @PostMapping("/board/write")
    public String write(@Valid @ModelAttribute("form") PostForm form, BindingResult binding, @LoginUser User user,
                        Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("categories", BoardCategory.values());
            return "board/form";
        }
        Post post = postService.write(user, form.getCategory(), form.getTitle(), form.getContent());
        ra.addFlashAttribute("success", "글을 등록했습니다.");
        return "redirect:/board/" + post.getId();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/board/{id:\\d+}/edit")
    public String editForm(@PathVariable Long id, @LoginUser User user, Model model) {
        Post post = postService.get(id);
        if (!post.isWrittenBy(user) && !user.isStaff()) {
            throw new com.parkminje.pingpong.common.ForbiddenException("작성자 또는 운영진만 수정할 수 있습니다.");
        }
        PostForm form = new PostForm();
        form.setCategory(post.getCategory());
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        model.addAttribute("form", form);
        model.addAttribute("post", post);
        model.addAttribute("categories", BoardCategory.values());
        return "board/form";
    }

    @PostMapping("/board/{id:\\d+}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") PostForm form, BindingResult binding,
                       @LoginUser User user, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("post", postService.get(id));
            model.addAttribute("categories", BoardCategory.values());
            return "board/form";
        }
        postService.edit(id, user, form.getCategory(), form.getTitle(), form.getContent());
        ra.addFlashAttribute("success", "글을 수정했습니다.");
        return "redirect:/board/" + id;
    }

    @PostMapping("/board/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, @LoginUser User user, RedirectAttributes ra) {
        postService.delete(id, user);
        ra.addFlashAttribute("success", "글을 삭제했습니다.");
        return "redirect:/board";
    }

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/board/{id:\\d+}/pin")
    public String pin(@PathVariable Long id) {
        postService.togglePin(id);
        return "redirect:/board/" + id;
    }

    @PostMapping("/board/{id:\\d+}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> like(@PathVariable Long id, @LoginUser User user) {
        boolean liked = postService.toggleLike(id, user);
        return ResponseEntity.ok(Map.of("liked", liked, "count", postService.get(id).getLikeCount()));
    }

    @PostMapping("/board/{id:\\d+}/comments")
    public String comment(@PathVariable Long id, @RequestParam String content, @LoginUser User user, RedirectAttributes ra) {
        postService.comment(id, user, content);
        return "redirect:/board/" + id + "#comments";
    }

    @PostMapping("/board/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId, @LoginUser User user, RedirectAttributes ra) {
        Long postId = postService.deleteComment(commentId, user);
        ra.addFlashAttribute("success", "댓글을 삭제했습니다.");
        return "redirect:/board/" + postId + "#comments";
    }
}
