package com.yourcompany.pingpong.modules.post.service;

import com.yourcompany.pingpong.domain.BoardCategory;
import com.yourcompany.pingpong.domain.Comment;
import com.yourcompany.pingpong.domain.Post;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.post.repository.CommentRepository;
import com.yourcompany.pingpong.modules.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 전체 게시글 조회 (페이징)
    public Page<Post> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.findAllByOrderByIsPinnedDescCreatedAtDesc(pageable);
        log.info("전체 게시글 조회 - 총 {}개, 현재 페이지: {}/{}", result.getTotalElements(), page + 1, result.getTotalPages());
        return result;
    }

    // 카테고리별 게시글 조회 (페이징)
    public Page<Post> getPostsByCategory(BoardCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.findByCategoryOrderByIsPinnedDescCreatedAtDesc(category, pageable);
        log.info("카테고리 {} 게시글 조회 - 총 {}개, 현재 페이지: {}/{}", category, result.getTotalElements(), page + 1, result.getTotalPages());

        // ✅ 디버깅: 실제 조회된 게시글 목록 출력
        result.getContent().forEach(post -> {
            log.info("  - ID: {}, 제목: {}, 카테고리: {}", post.getId(), post.getTitle(), post.getCategory());
        });

        return result;
    }

    // 게시글 상세 조회 (조회수 증가)
    @Transactional
    public Post getPostById(Long id) {
        Post post = postRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id));
        post.incrementViewCount();
        return post;
    }

    // 게시글 작성
    @Transactional
    public Post createPost(Post post, User author) {
        post.setAuthor(author);
        Post savedPost = postRepository.save(post);
        log.info("게시글 작성 완료 - ID: {}, 제목: {}, 카테고리: {}, 작성자: {}",
                savedPost.getId(), savedPost.getTitle(), savedPost.getCategory(), author.getUsername());
        return savedPost;
    }

    // 게시글 수정
    @Transactional
    public Post updatePost(Long id, String title, String content, BoardCategory category) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id));
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        log.info("게시글 수정 완료 - ID: {}, 카테고리: {}", id, category);
        return post;
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
        log.info("게시글 삭제 완료 - ID: {}", id);
    }

    // 상단 고정 토글
    @Transactional
    public void togglePin(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id));
        post.setIsPinned(!post.getIsPinned());
    }

    // 좋아요 증가
    @Transactional
    public void likePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id));
        post.incrementLikeCount();
    }

    // 검색
    public Page<Post> searchPosts(BoardCategory category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (category != null) {
            return postRepository.searchByCategoryAndKeyword(category, keyword, pageable);
        } else {
            return postRepository.searchByKeyword(keyword, pageable);
        }
    }

    // 인기글 조회
    public List<Post> getPopularPosts(BoardCategory category) {
        return postRepository.findTop5ByCategoryOrderByViewCountDesc(category);
    }

    // 게시글의 댓글 조회 - FETCH JOIN 사용
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdWithAuthor(postId);
    }
}