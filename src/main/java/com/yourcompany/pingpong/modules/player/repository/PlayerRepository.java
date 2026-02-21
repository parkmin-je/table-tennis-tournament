package com.yourcompany.pingpong.modules.player.repository;

import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * ✅ 클럽까지 Fetch (지연로딩 방지용, 메인 페이지/선수 목록용)
     */
    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.club ORDER BY p.name ASC")
    List<Player> findAllWithClub();

    /**
     * ✅ 클럽 + 유저까지 Fetch (단일 선수 상세 조회용)
     */
    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.club LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Player> findByIdWithClubAndUser(@Param("id") Long id);

    /**
     * ⭐ Club만 Fetch (detail 페이지용)
     */
    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.club WHERE p.id = :id")
    Optional<Player> findByIdWithClub(@Param("id") Long id);

    /**
     * ✅ 이름으로 검색
     */
    List<Player> findByNameContainingIgnoreCase(String name);

    /**
     * ⭐ 전역 검색용: 이름 포함 검색 + 클럽 Fetch
     */
    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.club " +
            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY p.ranking ASC NULLS LAST")
    List<Player> findByNameContainingWithClub(@Param("name") String name);

    /**
     * ⭐ 클럽별 선수 조회 (클럽 상세 페이지용)
     */
    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.club WHERE p.club.id = :clubId ORDER BY p.ranking DESC")
    List<Player> findByClubId(@Param("clubId") Long clubId);

    /**
     * ✅ 유저 기반 선수 조회
     */
    Optional<Player> findByUser(User user);

    /**
     * ⭐⭐⭐ 추가: User의 username으로 Player 조회 ⭐⭐⭐
     */
    @Query("SELECT p FROM Player p WHERE p.user.username = :username")
    Optional<Player> findByUserUsername(@Param("username") String username);
}