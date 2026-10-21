package com.parkminje.pingpong.match;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @EntityGraph(attributePaths = {"group", "entry1", "entry1.player", "entry1.player.club", "entry1.partner", "entry1.partner.club", "entry2", "entry2.player", "entry2.player.club", "entry2.partner", "entry2.partner.club", "winner"})
    List<Match> findByTournamentIdOrderByPlayOrderAsc(Long tournamentId);

    @EntityGraph(attributePaths = {"tournament", "group", "entry1", "entry1.player", "entry1.player.club", "entry1.partner", "entry1.partner.club", "entry2", "entry2.player", "entry2.player.club", "entry2.partner", "entry2.partner.club", "winner", "nextMatch"})
    Optional<Match> findWithDetailsById(Long id);

    @Query("select m.tournament.id from Match m where m.id = :id")
    Optional<Long> findTournamentIdById(@Param("id") Long id);

    List<Match> findByTournamentIdAndStage(Long tournamentId, MatchStage stage);

    long countByTournamentId(Long tournamentId);

    long countByTournamentIdAndStage(Long tournamentId, MatchStage stage);

    long countByTournamentIdAndStatus(Long tournamentId, MatchStatus status);

    long countByTournamentIdAndStageAndStatusNot(Long tournamentId, MatchStage stage, MatchStatus status);

    @Query("""
            select count(m) > 0 from Match m
            where m.tournament.id = :tournamentId and m.stage = :stage
              and (m.status = com.parkminje.pingpong.match.MatchStatus.IN_PROGRESS
                   or (m.status = com.parkminje.pingpong.match.MatchStatus.COMPLETED
                       and m.resultType <> com.parkminje.pingpong.match.ResultType.BYE))
            """)
    boolean anyPlayed(@Param("tournamentId") Long tournamentId, @Param("stage") MatchStage stage);

    @EntityGraph(attributePaths = {"tournament", "group", "entry1", "entry1.player", "entry1.player.club", "entry1.partner", "entry1.partner.club", "entry2", "entry2.player", "entry2.player.club", "entry2.partner", "entry2.partner.club"})
    List<Match> findByStatusOrderByStartedAtAsc(MatchStatus status);

    /** 선수가 출전한 완료 경기 (최신순) */
    @EntityGraph(attributePaths = {"tournament", "group", "entry1", "entry1.player", "entry1.player.club", "entry1.partner", "entry1.partner.club", "entry2", "entry2.player", "entry2.player.club", "entry2.partner", "entry2.partner.club", "winner"})
    @Query("""
            select m from Match m
            join m.entry1 e1 join m.entry2 e2
            left join e1.partner p1 left join e2.partner p2
            where m.status = com.parkminje.pingpong.match.MatchStatus.COMPLETED
              and (m.resultType is null or m.resultType <> com.parkminje.pingpong.match.ResultType.BYE)
              and (e1.player.id = :playerId or p1.id = :playerId or e2.player.id = :playerId or p2.id = :playerId)
            order by m.finishedAt desc
            """)
    List<Match> findCompletedByPlayer(@Param("playerId") Long playerId, Pageable pageable);

    @Query("select m.tournament.id, count(m) from Match m where m.status = :status and m.tournament.id in :ids group by m.tournament.id")
    List<Object[]> countByTournamentsAndStatus(@Param("ids") Collection<Long> ids, @Param("status") MatchStatus status);

    @Query("select m.tournament.id, count(m) from Match m where m.tournament.id in :ids group by m.tournament.id")
    List<Object[]> countByTournaments(@Param("ids") Collection<Long> ids);

    void deleteByTournamentIdAndStage(Long tournamentId, MatchStage stage);
}
