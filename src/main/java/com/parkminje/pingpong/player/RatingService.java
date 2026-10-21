package com.parkminje.pingpong.player;

import com.parkminje.pingpong.bracket.Elo;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.tournament.Tournament;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대회가 끝나면 단식 경기 결과로 Elo 레이팅을 갱신한다.
 * 부전승·BYE 는 제외하고, 경기 종료 시각 순으로 차례대로 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private final MatchRepository matchRepository;
    private final RatingHistoryRepository historyRepository;

    private static final class Tally {
        final Player player;
        final int before;
        int matches;
        int wins;

        Tally(Player player) {
            this.player = player;
            this.before = player.getRating();
        }
    }

    public void apply(Tournament t) {
        if (t.isRatingApplied()) {
            return;
        }
        t.markRatingApplied();
        if (t.isDoubles()) {
            return;
        }
        List<Match> matches = matchRepository.findByTournamentIdOrderByPlayOrderAsc(t.getId()).stream()
                .filter(m -> m.isCompleted() && m.hasBothEntries() && m.getWinner() != null)
                .filter(m -> m.getResultType() != null && !m.getResultType().isUnplayed())
                .sorted(Comparator.comparing(Match::getFinishedAt, Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .toList();

        Map<Long, Tally> tallies = new LinkedHashMap<>();
        for (Match m : matches) {
            Player p1 = m.getEntry1().getPlayer();
            Player p2 = m.getEntry2().getPlayer();
            Tally t1 = tallies.computeIfAbsent(p1.getId(), id -> new Tally(p1));
            Tally t2 = tallies.computeIfAbsent(p2.getId(), id -> new Tally(p2));
            boolean p1Won = m.winnerSide() == 1;
            int r1 = p1.getRating();
            int r2 = p2.getRating();
            int d1 = Elo.delta(r1, r2, p1Won, p1.getRatedMatches() + t1.matches);
            int d2 = Elo.delta(r2, r1, !p1Won, p2.getRatedMatches() + t2.matches);
            p1.applyRating(r1 + d1, 0);
            p2.applyRating(r2 + d2, 0);
            t1.matches++;
            t2.matches++;
            if (p1Won) {
                t1.wins++;
            } else {
                t2.wins++;
            }
        }
        for (Tally tally : tallies.values()) {
            tally.player.applyRating(tally.player.getRating(), tally.matches);
            historyRepository.save(new RatingHistory(tally.player, t, tally.before, tally.player.getRating(), tally.matches, tally.wins));
        }
        log.info("레이팅 반영: tournament={} players={} matches={}", t.getId(), tallies.size(), matches.size());
    }

    /** 대회 결과 수정으로 재개될 때 이 대회에서 반영한 레이팅을 되돌린다 */
    public void revert(Tournament t) {
        if (!t.isRatingApplied()) {
            return;
        }
        List<RatingHistory> histories = historyRepository.findByTournamentId(t.getId());
        for (RatingHistory h : histories) {
            Player p = h.getPlayer();
            p.applyRating(p.getRating() - h.getDelta(), -h.getMatchCount());
        }
        historyRepository.deleteAll(histories);
        t.unmarkRatingApplied();
    }
}
