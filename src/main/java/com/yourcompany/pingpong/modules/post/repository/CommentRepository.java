package com.yourcompany.pingpong.modules.post.repository;

import com.yourcompany.pingpong.domain.Comment;
import com.yourcompany.pingpong.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 기존 메서드: 게시글의 댓글 조회 (작성일 오름차순)
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    // 기존 메서드: 게시글의 댓글 수 (Long 타입으로 반환)
    Long countByPost(Post post);

    // 새 메서드: 게시글의 댓글 조회 - 작성자 정보 FETCH JOIN
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);
}