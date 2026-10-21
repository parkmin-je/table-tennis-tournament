package com.parkminje.pingpong.board;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.ForbiddenException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository likeRepository;

    public Post write(User author, BoardCategory category, String title, String content) {
        checkCategory(author, category);
        return postRepository.save(new Post(category, title.trim(), content.trim(), author));
    }

    public void edit(Long postId, User actor, BoardCategory category, String title, String content) {
        Post post = get(postId);
        requireOwnerOrStaff(post.isWrittenBy(actor), actor);
        checkCategory(actor, category);
        post.edit(category, title.trim(), content.trim());
    }

    public void delete(Long postId, User actor) {
        Post post = get(postId);
        requireOwnerOrStaff(post.isWrittenBy(actor), actor);
        postRepository.delete(post);
    }

    public void togglePin(Long postId) {
        get(postId).togglePin();
    }

    /** 좋아요 토글. 현재 좋아요 여부 반환 */
    public boolean toggleLike(Long postId, User user) {
        Post post = get(postId);
        boolean liked;
        var existing = likeRepository.findByPostIdAndUserId(postId, user.getId());
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            likeRepository.flush();
            liked = false;
        } else {
            likeRepository.saveAndFlush(new PostLike(post, user));
            liked = true;
        }
        post.setLikeCount(likeRepository.countByPostId(postId));
        return liked;
    }

    public Comment comment(Long postId, User author, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("댓글 내용을 입력하세요.");
        }
        if (content.length() > 1000) {
            throw new BusinessException("댓글은 1000자 이내로 입력하세요.");
        }
        Post post = get(postId);
        Comment c = commentRepository.save(new Comment(post, author, content.trim()));
        commentRepository.flush();
        post.setCommentCount(commentRepository.countByPostId(postId));
        return c;
    }

    /** 댓글 삭제 (작성자 또는 운영진). 댓글이 달린 글 ID 반환 */
    public Long deleteComment(Long commentId, User actor) {
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> NotFoundException.of("댓글", commentId));
        requireOwnerOrStaff(c.isWrittenBy(actor), actor);
        Post post = c.getPost();
        commentRepository.delete(c);
        commentRepository.flush();
        post.setCommentCount(commentRepository.countByPostId(post.getId()));
        return post.getId();
    }

    public void increaseView(Long postId) {
        postRepository.increaseView(postId);
    }

    @Transactional(readOnly = true)
    public Post get(Long id) {
        return postRepository.findWithAuthorById(id).orElseThrow(() -> NotFoundException.of("게시글", id));
    }

    @Transactional(readOnly = true)
    public Page<Post> search(BoardCategory category, String q, int page) {
        return postRepository.search(category, q == null || q.isBlank() ? null : q.trim(), PageRequest.of(Math.max(page, 0), 15));
    }

    @Transactional(readOnly = true)
    public List<Comment> comments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Transactional(readOnly = true)
    public boolean likedBy(Long postId, User user) {
        return user != null && likeRepository.existsByPostIdAndUserId(postId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<Post> latestNotices() {
        return postRepository.findTop5ByCategoryOrderByPinnedDescCreatedAtDesc(BoardCategory.NOTICE);
    }

    private static void checkCategory(User user, BoardCategory category) {
        if (category.isStaffOnly() && !user.isStaff()) {
            throw new ForbiddenException(category.getLabel() + "은(는) 운영진만 작성할 수 있습니다.");
        }
    }

    private static void requireOwnerOrStaff(boolean owner, User actor) {
        if (!owner && (actor == null || !actor.isStaff())) {
            throw new ForbiddenException("작성자 또는 운영진만 할 수 있습니다.");
        }
    }
}
