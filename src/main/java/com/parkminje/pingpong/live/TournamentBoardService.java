package com.parkminje.pingpong.live;

import com.parkminje.pingpong.bracket.KnockoutLayout;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryRepository;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchGroupRepository;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.match.MatchStage;
import com.parkminje.pingpong.match.ResultType;
import com.parkminje.pingpong.match.StandingsService;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentBoardService {

    private static final int QUEUE_SIZE = 12;
    private static final int RECENT_SIZE = 10;

    private final TournamentRepository tournamentRepository;
    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final MatchGroupRepository groupRepository;
    private final StandingsService standingsService;

    public TournamentBoard load(Long tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> NotFoundException.of("대회", tournamentId));
        List<Entry> entries = entryRepository.findByTournamentIdOrderByIdAsc(tournamentId);
        List<Match> matches = matchRepository.findByTournamentIdOrderByPlayOrderAsc(tournamentId);
        if (t.getChampion() != null) {
            Hibernate.initialize(t.getChampion());
            Hibernate.initialize(t.getChampion().getPlayer());
        }

        List<StandingsService.GroupTable> groups = standingsService.compute(
                groupRepository.findByTournamentIdOrderByOrdinalAsc(tournamentId), entries, matches);

        Map<Integer, List<Match>> byRound = matches.stream()
                .filter(Match::isKnockout)
                .collect(Collectors.groupingBy(Match::getRoundNo, TreeMap::new, Collectors.toList()));
        List<TournamentBoard.Round> rounds = new ArrayList<>();
        byRound.forEach((no, list) -> {
            list.sort(Comparator.comparingInt(Match::getBracketPos));
            rounds.add(new TournamentBoard.Round(no, KnockoutLayout.roundLabel(list.get(0).getBracketSize()), list));
        });

        List<TournamentBoard.Table> tables = new ArrayList<>();
        for (int n = 1; n <= t.getTableCount(); n++) {
            int table = n;
            Match live = matches.stream()
                    .filter(m -> m.isInProgress() && Integer.valueOf(table).equals(m.getTableNumber()))
                    .findFirst().orElse(null);
            tables.add(new TournamentBoard.Table(n, live));
        }

        Set<Long> busy = new HashSet<>();
        matches.stream().filter(Match::isInProgress).forEach(m -> {
            busy.add(m.getEntry1().getId());
            busy.add(m.getEntry2().getId());
        });
        List<Match> queue = matches.stream()
                .filter(m -> m.isScheduled() && m.hasBothEntries())
                .filter(m -> !busy.contains(m.getEntry1().getId()) && !busy.contains(m.getEntry2().getId()))
                .limit(QUEUE_SIZE)
                .toList();

        List<Match> recent = matches.stream()
                .filter(m -> m.isCompleted() && m.getResultType() != ResultType.BYE)
                .sorted(Comparator.comparing(Match::getFinishedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .limit(RECENT_SIZE)
                .toList();

        List<Match> playable = matches.stream().filter(m -> m.getResultType() != ResultType.BYE).toList();
        int done = (int) playable.stream().filter(Match::isCompleted).count();

        return new TournamentBoard(t, entries, groups, rounds, tables, queue, recent, matches, playable.size(), done);
    }

    /** 대기열 전체 (운영 콘솔용, 상대 미정 경기 포함) */
    public List<Match> pending(TournamentBoard board) {
        return board.matches().stream().filter(m -> !m.isCompleted()).toList();
    }

    public static boolean isGroup(Match m) {
        return m.getStage() == MatchStage.GROUP;
    }
}
