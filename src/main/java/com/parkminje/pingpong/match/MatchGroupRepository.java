package com.parkminje.pingpong.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchGroupRepository extends JpaRepository<MatchGroup, Long> {

    List<MatchGroup> findByTournamentIdOrderByOrdinalAsc(Long tournamentId);

    void deleteByTournamentId(Long tournamentId);
}
