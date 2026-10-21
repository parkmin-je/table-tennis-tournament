package com.parkminje.pingpong.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    @Query(value = """
            select p from Post p
            where (:category is null or p.category = :category)
              and (:q is null or p.title like concat('%', :q, '%') or p.content like concat('%', :q, '%'))
            order by p.pinned desc, p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            where (:category is null or p.category = :category)
              and (:q is null or p.title like concat('%', :q, '%') or p.content like concat('%', :q, '%'))
            """)
    Page<Post> search(@Param("category") BoardCategory category, @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Post> findWithAuthorById(Long id);

    @EntityGraph(attributePaths = "author")
    List<Post> findTop5ByCategoryOrderByPinnedDescCreatedAtDesc(BoardCategory category);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseView(@Param("id") Long id);
}
