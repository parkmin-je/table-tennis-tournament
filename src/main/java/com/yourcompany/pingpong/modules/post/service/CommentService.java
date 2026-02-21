package com.yourcompany.pingpong.modules.post.service;

import com.yourcompany.pingpong.domain.Comment;
import com.yourcompany.pingpong.domain.Post;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.post.repository.CommentRepository;
import com.yourcompany.pingpong.modules.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    // 특정 게시글의 댓글 조회
    public List<Comment> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + postId));
        return commentRepository.findByPostOrderByCreatedAtAsc(post);
    }

    // 댓글 작성
    @Transactional
    public Comment createComment(Long postId, String content, User author) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .content(content)
                .author(author)
                .post(post)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // ✅ 댓글 개수 증가
        post.incrementCommentCount();

        return savedComment;
    }

    // 댓글 수정
    @Transactional
    public Comment updateComment(Long id, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. ID: " + id));
        comment.setContent(content);
        return comment;
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Post post = comment.getPost();
        commentRepository.delete(comment);

        // ✅ 댓글 개수 감소
        if (post != null) {
            post.decrementCommentCount();
        }
    }

    // 댓글 개수 조회
    public Long getCommentCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + postId));
        return commentRepository.countByPost(post);
    }
}