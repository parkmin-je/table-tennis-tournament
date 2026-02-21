package com.yourcompany.pingpong.modules.tournament.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // ⭐⭐⭐ 관리자용: 전체 대회 목록 (Creator JOIN FETCH) ⭐⭐⭐
    @Query("SELECT DISTINCT t FROM Tournament t " +
            "LEFT JOIN FETCH t.creator " +
            "ORDER BY t.createdAt DESC")
    List<Tournament> findAllWithCreator();

    @Query("SELECT DISTINCT t FROM Tournament t " +
            "LEFT JOIN FETCH t.participations " +
            "LEFT JOIN FETCH t.creator " +
            "ORDER BY t.createdAt DESC")
    List<Tournament> findAllWithParticipations();

    @Query("SELECT t FROM Tournament t " +
            "LEFT JOIN FETCH t.participations " +
            "WHERE t.id = :id")
    Optional<Tournament> findByIdWithParticipations(@Param("id") Long id);

    @Query("SELECT DISTINCT t FROM Tournament t " +
            "LEFT JOIN FETCH t.participations " +
            "LEFT JOIN FETCH t.creator " +
            "WHERE t.status IN :statuses " +
            "ORDER BY t.startDate ASC")
    List<Tournament> findOngoingTournamentsWithParticipations(@Param("statuses") List<TournamentStatus> statuses);
}