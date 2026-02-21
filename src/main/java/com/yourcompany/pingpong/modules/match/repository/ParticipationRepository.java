package com.yourcompany.pingpong.modules.match.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yourcompany.pingpong.domain.Participation;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.MatchGroup;
import com.yourcompany.pingpong.domain.User;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    // ⭐⭐⭐ 관리자 대시보드용: User, Tournament JOIN FETCH (최신 10개) ⭐⭐⭐
    @Query("SELECT p FROM Participation p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.tournament " +
            "ORDER BY p.registeredAt DESC")
    List<Participation> findTop10ByOrderByRegisteredAtDesc();

    // ⭐⭐⭐ 참가 신청 관리 목록용: User, Tournament JOIN FETCH (전체) ⭐⭐⭐
    @Query("SELECT p FROM Participation p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.tournament " +
            "ORDER BY p.registeredAt DESC")
    List<Participation> findAllWithUserAndTournament();

    @Query("SELECT p FROM Participation p " +
            "LEFT JOIN FETCH p.player " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.matchGroup.id IN :groupIds")
    List<Participation> findByMatchGroupIdInWithPlayerAndUser(@Param("groupIds") List<Long> groupIds);

    @Query("SELECT p FROM Participation p " +
            "LEFT JOIN FETCH p.tournament " +
            "WHERE p.user.username = :username")
    List<Participation> findByUserUsername(@Param("username") String username);

    List<Participation> findByTournament(Tournament tournament);

    List<Participation> findByMatchGroup(MatchGroup matchGroup);

    boolean existsByTournamentAndUser(Tournament tournament, User user);
}