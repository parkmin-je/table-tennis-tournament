package com.parkminje.pingpong.club;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubService {

    public record ClubSummary(Club club, long members, int avgRating) {
    }

    private final ClubRepository clubRepository;
    private final PlayerRepository playerRepository;

    public Club create(String name, String region, String description) {
        String n = name.trim();
        if (clubRepository.existsByName(n)) {
            throw new BusinessException("같은 이름의 클럽이 이미 있습니다.");
        }
        return clubRepository.save(new Club(n, blank(region), blank(description)));
    }

    public void update(Long id, String name, String region, String description) {
        Club club = get(id);
        String n = name.trim();
        if (clubRepository.existsByNameAndIdNot(n, id)) {
            throw new BusinessException("같은 이름의 클럽이 이미 있습니다.");
        }
        club.update(n, blank(region), blank(description));
    }

    public void delete(Long id) {
        Club club = get(id);
        long members = playerRepository.countByClubId(id);
        if (members > 0) {
            throw new BusinessException("소속 선수가 " + members + "명 있는 클럽은 삭제할 수 없습니다. 먼저 선수 소속을 변경하세요.");
        }
        clubRepository.delete(club);
    }

    @Transactional(readOnly = true)
    public Club get(Long id) {
        return clubRepository.findById(id).orElseThrow(() -> NotFoundException.of("클럽", id));
    }

    @Transactional(readOnly = true)
    public List<Club> all() {
        return clubRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ClubSummary> summaries() {
        Map<Long, Object[]> stats = new HashMap<>();
        for (Object[] row : clubRepository.memberStats()) {
            stats.put((Long) row[0], row);
        }
        return clubRepository.findAllByOrderByNameAsc().stream().map(c -> {
            Object[] row = stats.get(c.getId());
            long members = row == null ? 0 : ((Number) row[1]).longValue();
            int avg = row == null || row[2] == null ? 0 : (int) Math.round(((Number) row[2]).doubleValue());
            return new ClubSummary(c, members, avg);
        }).toList();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
