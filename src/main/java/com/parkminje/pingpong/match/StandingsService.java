package com.parkminje.pingpong.match;

import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.bracket.Standings;
import com.parkminje.pingpong.entry.Entry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 조별 순위표 계산 (DB 조회 없이 이미 불러온 경기 목록으로 계산) */
@Service
public class StandingsService {

    public record GroupTable(MatchGroup group, List<Standings.Row<Entry>> rows, int completed, int total) {
        public boolean isFinished() {
            return total > 0 && completed == total;
        }
    }

    /**
     * @param entries 이 대회의 조 배정된 엔트리
     * @param matches 이 대회의 전체 경기
     */
    public List<GroupTable> compute(List<MatchGroup> groups, List<Entry> entries, List<Match> matches) {
        Map<Long, List<Entry>> entriesByGroup = entries.stream()
                .filter(e -> e.getGroup() != null)
                .collect(Collectors.groupingBy(e -> e.getGroup().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Match>> matchesByGroup = matches.stream()
                .filter(m -> m.getStage() == MatchStage.GROUP && m.getGroup() != null)
                .collect(Collectors.groupingBy(m -> m.getGroup().getId()));

        List<GroupTable> tables = new ArrayList<>();
        for (MatchGroup group : groups) {
            List<Entry> members = new ArrayList<>(entriesByGroup.getOrDefault(group.getId(), List.of()));
            members.sort(seedComparator());
            List<Match> groupMatches = matchesByGroup.getOrDefault(group.getId(), List.of());
            tables.add(new GroupTable(group, Standings.compute(members, toResults(groupMatches)),
                    (int) groupMatches.stream().filter(Match::isCompleted).count(), groupMatches.size()));
        }
        return tables;
    }

    /** 동률 최종 판단용 시드 순서: 지정 시드 → 레이팅 → 접수 순 */
    public static Comparator<Entry> seedComparator() {
        return Comparator.comparing(Entry::getSeed, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Entry::getStrength, Comparator.reverseOrder())
                .thenComparing(Entry::getId);
    }

    private static List<Standings.Result<Entry>> toResults(List<Match> matches) {
        List<Standings.Result<Entry>> results = new ArrayList<>();
        for (Match m : matches) {
            if (!m.isCompleted() || !m.hasBothEntries() || m.getWinner() == null) {
                continue;
            }
            boolean unplayed = m.getResultType() != null && m.getResultType().isUnplayed();
            int g1 = unplayed || m.getGames1() == null ? 0 : m.getGames1();
            int g2 = unplayed || m.getGames2() == null ? 0 : m.getGames2();
            int p1 = 0;
            int p2 = 0;
            if (!unplayed) {
                for (int[] game : GameScore.parseStored(m.getGameScores())) {
                    p1 += game[0];
                    p2 += game[1];
                }
            }
            Entry winner = m.isWinner(m.getEntry1()) ? m.getEntry1() : m.getEntry2();
            results.add(new Standings.Result<>(m.getEntry1(), m.getEntry2(), winner, g1, g2, p1, p2, unplayed));
        }
        return results;
    }
}
