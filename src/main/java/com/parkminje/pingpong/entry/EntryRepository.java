package com.parkminje.pingpong.entry;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    @EntityGraph(attributePaths = {"player", "player.club", "partner", "partner.club", "group"})
    List<Entry> findByTournamentIdOrderByIdAsc(Long tournamentId);

    @EntityGraph(attributePaths = {"player", "player.club", "partner", "partner.club", "group"})
    List<Entry> findByTournamentIdAndStatusInOrderByIdAsc(Long tournamentId, Collection<EntryStatus> statuses);

    java.util.Optional<Entry> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    long countByTournamentIdAndStatusIn(Long tournamentId, Collection<EntryStatus> statuses);

    /** 해당 대회에서 이 선수가 (본인 또는 파트너로) 활성 엔트리에 들어가 있는지 */
    @Query("""
            select count(e) > 0 from Entry e left join e.partner pt
            where e.tournament.id = :tournamentId
              and e.status in (com.parkminje.pingpong.entry.EntryStatus.APPLIED, com.parkminje.pingpong.entry.EntryStatus.CONFIRMED)
              and (e.player.id = :playerId or pt.id = :playerId)
            """)
    boolean isPlayerEntered(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @EntityGraph(attributePaths = {"tournament", "player", "partner"})
    @Query("""
            select e from Entry e left join e.partner pt
            where e.player.id = :playerId or pt.id = :playerId
            order by e.tournament.startAt desc
            """)
    List<Entry> findByPlayerInvolved(@Param("playerId") Long playerId);

    @EntityGraph(attributePaths = {"tournament", "player", "partner"})
    List<Entry> findTop10ByOrderByCreatedAtDesc();

    @Query("select e.tournament.id, count(e) from Entry e where e.status in :statuses group by e.tournament.id")
    List<Object[]> countByTournament(@Param("statuses") Collection<EntryStatus> statuses);

    void deleteByTournamentId(Long tournamentId);
}
