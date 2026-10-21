package com.parkminje.pingpong.board;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    int countByPostId(Long postId);
}
