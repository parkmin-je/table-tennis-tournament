package com.parkminje.pingpong.tournament;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    /**
     * 대회 단위 직렬화 락. 대진 생성·경기 시작/종료처럼 같은 대회의 상태를 바꾸는 작업은
     * 모두 이 락을 잡고 진행해 동시 입력(여러 운영진·여러 서버)에도 대진이 꼬이지 않게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tournament t where t.id = :id")
    Optional<Tournament> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            select t from Tournament t
            where (:status is null or t.status = :status)
              and t.status <> com.parkminje.pingpong.tournament.TournamentStatus.DRAFT
              and (:q is null or t.title like concat('%', :q, '%') or t.venue like concat('%', :q, '%'))
            order by case when t.status = com.parkminje.pingpong.tournament.TournamentStatus.IN_PROGRESS then 0
                          when t.status = com.parkminje.pingpong.tournament.TournamentStatus.RECRUITING then 1
                          when t.status = com.parkminje.pingpong.tournament.TournamentStatus.CLOSED then 2
                          else 3 end,
                     t.startAt desc
            """,
            countQuery = """
            select count(t) from Tournament t
            where (:status is null or t.status = :status)
              and t.status <> com.parkminje.pingpong.tournament.TournamentStatus.DRAFT
              and (:q is null or t.title like concat('%', :q, '%') or t.venue like concat('%', :q, '%'))
            """)
    Page<Tournament> searchPublic(@Param("status") TournamentStatus status, @Param("q") String q, Pageable pageable);

    List<Tournament> findAllByOrderByStartAtDesc();

    List<Tournament> findByStatusInOrderByStartAtAsc(Collection<TournamentStatus> statuses);

    List<Tournament> findByStatusNotAndStartAtBetweenOrderByStartAtAsc(TournamentStatus status, LocalDateTime from, LocalDateTime to);

    List<Tournament> findTop10ByTitleContainingAndStatusNotOrderByStartAtDesc(String title, TournamentStatus status);

    long countByStatus(TournamentStatus status);
}
