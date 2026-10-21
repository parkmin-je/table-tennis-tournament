package com.parkminje.pingpong.club;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<Club> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** [clubId, 선수 수, 평균 레이팅] */
    @Query("select p.club.id, count(p), avg(p.rating) from Player p where p.club is not null group by p.club.id")
    List<Object[]> memberStats();
}
