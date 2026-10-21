package com.parkminje.pingpong.player;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @EntityGraph(attributePaths = {"club", "user"})
    Optional<Player> findWithClubById(Long id);

    @EntityGraph(attributePaths = "club")
    Optional<Player> findByUserId(Long userId);

    @EntityGraph(attributePaths = "club")
    @Query(value = """
            select p from Player p left join p.club c
            where (:q is null or p.name like concat('%', :q, '%') or c.name like concat('%', :q, '%'))
              and (:clubId is null or c.id = :clubId)
              and (:division is null or p.division = :division)
              and (:gender is null or p.gender = :gender)
            """,
            countQuery = """
            select count(p) from Player p left join p.club c
            where (:q is null or p.name like concat('%', :q, '%') or c.name like concat('%', :q, '%'))
              and (:clubId is null or c.id = :clubId)
              and (:division is null or p.division = :division)
              and (:gender is null or p.gender = :gender)
            """)
    Page<Player> search(@Param("q") String q, @Param("clubId") Long clubId, @Param("division") Integer division,
                        @Param("gender") Gender gender, Pageable pageable);

    @EntityGraph(attributePaths = "club")
    List<Player> findByClubIdOrderByRatingDesc(Long clubId);

    @EntityGraph(attributePaths = "club")
    @Query(value = "select p from Player p where p.ratedMatches > 0 order by p.rating desc, p.ratedMatches desc",
            countQuery = "select count(p) from Player p where p.ratedMatches > 0")
    Page<Player> ranking(Pageable pageable);

    @EntityGraph(attributePaths = "club")
    List<Player> findTop10ByNameContainingOrderByRatingDesc(String name);

    long countByClubId(Long clubId);

    @EntityGraph(attributePaths = "club")
    List<Player> findAllByOrderByNameAsc();
}
