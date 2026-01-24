package com.yourcompany.pingpong.modules.match.repository;

import com.yourcompany.pingpong.domain.MatchGroup;
import com.yourcompany.pingpong.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchGroupRepository extends JpaRepository<MatchGroup, Long> {
    // 기본 메서드 유지
    List<MatchGroup> findByTournament(Tournament tournament);
    boolean existsByTournament(Tournament tournament);

    // ⭐⭐ 새로운 Fetch Join 쿼리 추가! MatchGroup과 Tournament를 한 번에 가져옴 ⭐⭐
    @Query("SELECT mg FROM MatchGroup mg JOIN FETCH mg.tournament t WHERE t = :tournament")
    List<MatchGroup> findByTournamentWithTournament(@Param("tournament") Tournament tournament);

    // ID 기반 조회시 Fetch Join (필요하다면)
    @Query("SELECT mg FROM MatchGroup mg JOIN FETCH mg.tournament t WHERE mg.id IN :groupIds")
    List<MatchGroup> findByIdInWithTournament(@Param("groupIds") List<Long> groupIds);
}