package com.parkminje.pingpong.live;

import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryStatus;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchStage;
import com.parkminje.pingpong.match.StandingsService;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentStatus;

import java.util.List;

/**
 * 대회 화면용 읽기 모델 (상세·운영 콘솔·현황판 공용).
 */
public record TournamentBoard(
        Tournament tournament,
        List<Entry> entries,
        List<StandingsService.GroupTable> groups,
        List<Round> rounds,
        List<Table> tables,
        List<Match> queue,
        List<Match> recent,
        List<Match> matches,
        int playableTotal,
        int playableDone
) {

    public record Round(int roundNo, String label, List<Match> matches) {
    }

    public record Table(int number, Match match) {
        public boolean isFree() {
            return match == null;
        }
    }

    public List<Entry> activeEntries() {
        return entries.stream().filter(e -> e.getStatus().isActive()).toList();
    }

    public long countByStatus(EntryStatus status) {
        return entries.stream().filter(e -> e.getStatus() == status).count();
    }

    public long appliedCount() {
        return countByStatus(EntryStatus.APPLIED);
    }

    public long confirmedCount() {
        return countByStatus(EntryStatus.CONFIRMED);
    }

    public long checkedInCount() {
        return entries.stream().filter(e -> e.getStatus() == EntryStatus.CONFIRMED && e.isCheckedIn()).count();
    }

    public long paidCount() {
        return entries.stream().filter(e -> e.getStatus().isActive() && e.isPaid()).count();
    }

    public boolean hasDraw() {
        return !matches.isEmpty();
    }

    public boolean hasGroupStage() {
        return !groups.isEmpty();
    }

    public boolean hasKnockout() {
        return !rounds.isEmpty();
    }

    public boolean isGroupStageFinished() {
        return hasGroupStage() && groups.stream().allMatch(StandingsService.GroupTable::isFinished);
    }

    public boolean canCreateKnockout() {
        return tournament.hasGroupStage() && tournament.getStatus() == TournamentStatus.IN_PROGRESS
                && isGroupStageFinished() && !hasKnockout();
    }

    public boolean isLive() {
        return tournament.getStatus() == TournamentStatus.IN_PROGRESS;
    }

    public long liveCount() {
        return tables.stream().filter(t -> !t.isFree()).count();
    }

    public int progressPercent() {
        return playableTotal == 0 ? 0 : (int) Math.round(playableDone * 100.0 / playableTotal);
    }

    public List<Match> groupMatchesOf(Long groupId) {
        return matches.stream()
                .filter(m -> m.getStage() == MatchStage.GROUP && m.getGroup() != null && m.getGroup().getId().equals(groupId))
                .toList();
    }

    /** 결승 경기 (본선이 있을 때) */
    public Match finalMatch() {
        return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1).matches().get(0);
    }

    /** 공동 3위 (준결승 패자) */
    public List<Entry> thirdPlaces() {
        if (rounds.size() < 2) {
            return List.of();
        }
        return rounds.get(rounds.size() - 2).matches().stream()
                .map(Match::getLoser)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
