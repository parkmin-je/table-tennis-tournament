package com.yourcompany.pingpong.modules.match.repository;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.MatchStatus;
import com.yourcompany.pingpong.domain.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // 기존의 Match 엔티티 관계 로딩 방식
    List<Match> findByTournamentIdOrderByRoundNameAsc(Long tournamentId);

    List<Match> findByTournamentIdAndRoundNameOrderByMatchNumberAsc(Long tournamentId, String roundName);

    // ⭐⭐⭐ TournamentController detail 페이지용: Player까지 Fetch Join ⭐⭐⭐
    @Query("SELECT DISTINCT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "LEFT JOIN FETCH m.matchGroup " +
            "WHERE m.tournament.id = :tournamentId " +
            "ORDER BY m.roundName ASC, m.matchNumber ASC")
    List<Match> findByTournamentIdWithPlayers(@Param("tournamentId") Long tournamentId);

    // ⭐⭐⭐ 핵심 수정: findByTournamentIdAndStatusOrderByMatchTimeAsc 메서드에 Fetch Join 추가! ⭐⭐⭐
    // player1, player2, tournament 엔티티를 함께 EAGER 로딩하여 LazyInitializationException 방지
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 p1 " +
            "LEFT JOIN FETCH m.player2 p2 " +
            "LEFT JOIN FETCH m.tournament t " +
            "LEFT JOIN FETCH m.matchGroup mg " +
            "WHERE m.tournament.id = :tournamentId AND m.status = :status " +
            "ORDER BY m.matchTime ASC")
    List<Match> findByTournamentIdAndStatusOrderByMatchTimeAsc(@Param("tournamentId") Long tournamentId,
                                                               @Param("status") MatchStatus status);

    // ⭐ 추가: 특정 대회의 모든 경기 조회 (TournamentController의 bracket, statistics용)
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "LEFT JOIN FETCH m.matchGroup " +
            "WHERE m.tournament.id = :tournamentId " +
            "ORDER BY m.roundName ASC, m.matchNumber ASC, m.id ASC")
    List<Match> findByTournamentId(@Param("tournamentId") Long tournamentId);

    Optional<Match> findByTableNumberAndStatus(Integer tableNumber, MatchStatus status);

    // ⭐ 기존: 모든 Tournament의 오늘 경기
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "WHERE m.matchTime BETWEEN :startOfDay AND :endOfDay " +
            "ORDER BY m.matchTime ASC")
    List<Match> findMatchesForToday(@Param("startOfDay") LocalDateTime startOfDay,
                                    @Param("endOfDay") LocalDateTime endOfDay);

    // ⭐ 추가: 특정 상태의 Tournament의 오늘 경기
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament t " +
            "WHERE m.matchTime BETWEEN :startOfDay AND :endOfDay " +
            "AND t.status = :tournamentStatus " +
            "ORDER BY m.matchTime ASC")
    List<Match> findMatchesForToday(@Param("startOfDay") LocalDateTime startOfDay,
                                    @Param("endOfDay") LocalDateTime endOfDay,
                                    @Param("tournamentStatus") TournamentStatus tournamentStatus);

    // ⭐ 라이브 스코어보드용: 현재 진행 중인 모든 경기
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "WHERE m.status = com.yourcompany.pingpong.domain.MatchStatus.IN_PROGRESS " +
            "ORDER BY m.tableNumber ASC NULLS LAST, m.id ASC")
    List<Match> findAllInProgress();

    // ⭐ 선수 전적 통계용: 특정 선수의 완료된 경기
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "WHERE (m.player1.id = :playerId OR m.player2.id = :playerId) " +
            "AND m.status = com.yourcompany.pingpong.domain.MatchStatus.COMPLETED " +
            "ORDER BY m.updatedAt DESC")
    List<Match> findCompletedMatchesByPlayerId(@Param("playerId") Long playerId);

    // ⭐ 경기 관리 패널용: 특정 대회 SCHEDULED + IN_PROGRESS 경기
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.tournament " +
            "LEFT JOIN FETCH m.matchGroup " +
            "WHERE m.tournament.id = :tournamentId " +
            "AND m.status IN (com.yourcompany.pingpong.domain.MatchStatus.SCHEDULED, " +
            "                 com.yourcompany.pingpong.domain.MatchStatus.IN_PROGRESS) " +
            "ORDER BY m.status DESC, m.roundName ASC, m.matchNumber ASC")
    List<Match> findActiveMatchesByTournamentId(@Param("tournamentId") Long tournamentId);
}