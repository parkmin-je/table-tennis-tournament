package com.yourcompany.pingpong.modules.club.repository;

import com.yourcompany.pingpong.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    /** ✅ Club + Player 함께 로딩 (N+1 방지) */
    @Query("SELECT DISTINCT c FROM Club c LEFT JOIN FETCH c.players")
    List<Club> findAllWithPlayers();
}
