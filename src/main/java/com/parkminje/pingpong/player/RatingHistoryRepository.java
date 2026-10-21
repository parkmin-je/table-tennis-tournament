package com.parkminje.pingpong.player;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {

    @EntityGraph(attributePaths = "tournament")
    List<RatingHistory> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    @EntityGraph(attributePaths = "player")
    List<RatingHistory> findByTournamentId(Long tournamentId);
}
