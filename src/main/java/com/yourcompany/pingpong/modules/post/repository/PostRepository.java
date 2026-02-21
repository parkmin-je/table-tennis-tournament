package com.yourcompany.pingpong.modules.post.repository;

import com.yourcompany.pingpong.domain.BoardCategory;
import com.yourcompany.pingpong.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ✅ 카테고리별 게시글 조회 (페이징) - 명시적 카운트 쿼리 추가
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.category = :category ORDER BY p.isPinned DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.category = :category")
    Page<Post> findByCategoryOrderByIsPinnedDescCreatedAtDesc(@Param("category") BoardCategory category, Pageable pageable);

    // ✅ 전체 게시글 조회 (고정글 우선, 최신순) - 명시적 카운트 쿼리 추가
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.isPinned DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllByOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // ✅ 게시글 상세 조회 - 작성자 FETCH JOIN
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author WHERE p.id = :id")
    Optional<Post> findByIdWithAuthor(@Param("id") Long id);

    // ✅ 제목 + 내용 검색 (카테고리별) - 명시적 카운트 쿼리 추가
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.category = :category AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) ORDER BY p.isPinned DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.category = :category AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByCategoryAndKeyword(@Param("category") BoardCategory category, @Param("keyword") String keyword, Pageable pageable);

    // ✅ 전체 검색 - 명시적 카운트 쿼리 추가
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.isPinned DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ✅ 인기글 조회 (조회수 기준) - List 반환이므로 FETCH JOIN 사용 가능
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author WHERE p.category = :category ORDER BY p.viewCount DESC")
    List<Post> findTop5ByCategoryOrderByViewCountDesc(@Param("category") BoardCategory category);
}