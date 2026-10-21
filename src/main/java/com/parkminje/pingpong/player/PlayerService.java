package com.parkminje.pingpong.player;

import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.club.ClubRepository;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryRepository;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    public record PlayerInput(String name, Gender gender, Integer birthYear, Integer division, String phone, Long clubId) {
    }

    /** 경기 한 줄 (선수 관점) */
    public record MatchLine(Match match, Entry mine, Entry opponent, boolean won) {
    }

    public record PlayerStats(int played, int wins, int losses, int gamesWon, int gamesLost, List<MatchLine> recent) {
        public int winRate() {
            return played == 0 ? 0 : (int) Math.round(wins * 100.0 / played);
        }
    }

    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final RatingHistoryRepository ratingHistoryRepository;

    public Player create(PlayerInput in) {
        validate(in);
        return playerRepository.save(new Player(in.name().trim(), in.gender(), in.birthYear(), in.division(),
                blank(in.phone()), club(in.clubId()), null));
    }

    public void update(Long id, PlayerInput in) {
        validate(in);
        Player p = get(id);
        p.update(in.name().trim(), in.gender(), in.birthYear(), in.division(), blank(in.phone()), club(in.clubId()));
    }

    /** 회원 본인 선수 프로필 등록/수정 (이름은 회원 이름을 따른다) */
    public Player saveMyProfile(User user, PlayerInput in) {
        PlayerInput withName = new PlayerInput(user.getName(), in.gender(), in.birthYear(), in.division(), in.phone(), in.clubId());
        validate(withName);
        Optional<Player> existing = playerRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            Player p = existing.get();
            p.update(user.getName(), in.gender(), in.birthYear(), in.division(), blank(in.phone()), club(in.clubId()));
            return p;
        }
        return playerRepository.save(new Player(user.getName(), in.gender(), in.birthYear(), in.division(),
                blank(in.phone()), club(in.clubId()), user));
    }

    /** 회원 이름이 바뀌면 연결된 선수 이름도 맞춘다 */
    public void syncName(Long userId, String name) {
        playerRepository.findByUserId(userId).ifPresent(p ->
                p.update(name, p.getGender(), p.getBirthYear(), p.getDivision(), p.getPhone(), p.getClub()));
    }

    public void delete(Long id) {
        Player p = get(id);
        if (!entryRepository.findByPlayerInvolved(id).isEmpty()) {
            throw new BusinessException("대회 참가 기록이 있는 선수는 삭제할 수 없습니다.");
        }
        playerRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public Player get(Long id) {
        return playerRepository.findWithClubById(id).orElseThrow(() -> NotFoundException.of("선수", id));
    }

    @Transactional(readOnly = true)
    public Optional<Player> findByUser(Long userId) {
        return playerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<Player> search(String q, Long clubId, Integer division, Gender gender, int page) {
        return playerRepository.search(blank(q), clubId, division, gender,
                PageRequest.of(Math.max(page, 0), 24, org.springframework.data.domain.Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Page<Player> ranking(int page) {
        return playerRepository.ranking(PageRequest.of(Math.max(page, 0), 50));
    }

    @Transactional(readOnly = true)
    public List<Player> topRanked(int n) {
        return playerRepository.ranking(PageRequest.of(0, n)).getContent();
    }

    @Transactional(readOnly = true)
    public List<Player> clubMembers(Long clubId) {
        return playerRepository.findByClubIdOrderByRatingDesc(clubId);
    }

    @Transactional(readOnly = true)
    public List<RatingHistory> ratingHistory(Long playerId) {
        return ratingHistoryRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }

    @Transactional(readOnly = true)
    public List<Entry> entries(Long playerId) {
        return entryRepository.findByPlayerInvolved(playerId);
    }

    @Transactional(readOnly = true)
    public PlayerStats stats(Long playerId) {
        Player me = get(playerId);
        List<Match> matches = matchRepository.findCompletedByPlayer(playerId, PageRequest.of(0, 300));
        int wins = 0;
        int losses = 0;
        int gw = 0;
        int gl = 0;
        List<MatchLine> lines = new java.util.ArrayList<>();
        for (Match m : matches) {
            Entry mine = m.getEntry1().includes(me) ? m.getEntry1() : m.getEntry2();
            Entry opp = m.opponentOf(mine);
            boolean won = m.isWinner(mine);
            boolean unplayed = m.getResultType() != null && m.getResultType().isUnplayed();
            if (!unplayed) {
                if (won) {
                    wins++;
                } else {
                    losses++;
                }
                boolean first = mine.equals(m.getEntry1());
                gw += nz(first ? m.getGames1() : m.getGames2());
                gl += nz(first ? m.getGames2() : m.getGames1());
            }
            if (lines.size() < 20) {
                lines.add(new MatchLine(m, mine, opp, won));
            }
        }
        return new PlayerStats(wins + losses, wins, losses, gw, gl, lines);
    }

    private void validate(PlayerInput in) {
        if (in.name() == null || in.name().isBlank()) {
            throw new BusinessException("이름을 입력하세요.");
        }
        if (in.gender() == null) {
            throw new BusinessException("성별을 선택하세요.");
        }
        if (in.division() != null && (in.division() < 1 || in.division() > 9)) {
            throw new BusinessException("부수는 1~9부 사이로 입력하세요.");
        }
        int year = java.time.Year.now().getValue();
        if (in.birthYear() != null && (in.birthYear() < year - 100 || in.birthYear() > year - 5)) {
            throw new BusinessException("출생연도가 올바르지 않습니다.");
        }
    }

    private Club club(Long clubId) {
        return clubId == null ? null : clubRepository.findById(clubId).orElseThrow(() -> NotFoundException.of("클럽", clubId));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
