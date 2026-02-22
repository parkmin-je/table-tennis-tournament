package com.yourcompany.pingpong.modules.player.dto;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.Player;
import lombok.Getter;

import java.util.List;

/**
 * 선수 전적 통계 DTO
 */
@Getter
public class PlayerStatsDto {

    private final Long playerId;
    private final String playerName;
    private final String clubName;
    private final Integer ranking;

    private final int totalMatches;
    private final int wins;
    private final int losses;
    private final double winRate;

    private final int totalPointsScored;
    private final int totalPointsConceded;
    private final double avgPointsPerMatch;

    private final List<RecentMatch> recentMatches;

    public PlayerStatsDto(Player player, List<Match> completedMatches) {
        this.playerId  = player.getId();
        this.playerName = player.getName();
        this.clubName   = player.getClub() != null ? player.getClub().getName() : null;
        this.ranking    = player.getRanking();

        int wins = 0, losses = 0, scored = 0, conceded = 0;

        for (Match m : completedMatches) {
            boolean isPlayer1 = m.getPlayer1() != null && m.getPlayer1().getId().equals(player.getId());
            int myScore    = isPlayer1 ? (m.getScore1() != null ? m.getScore1() : 0)
                                       : (m.getScore2() != null ? m.getScore2() : 0);
            int theirScore = isPlayer1 ? (m.getScore2() != null ? m.getScore2() : 0)
                                       : (m.getScore1() != null ? m.getScore1() : 0);
            scored    += myScore;
            conceded  += theirScore;
            if (myScore > theirScore) wins++;
            else                      losses++;
        }

        this.totalMatches = completedMatches.size();
        this.wins         = wins;
        this.losses       = losses;
        this.winRate      = totalMatches > 0 ? Math.round((wins * 100.0 / totalMatches) * 10) / 10.0 : 0;
        this.totalPointsScored   = scored;
        this.totalPointsConceded = conceded;
        this.avgPointsPerMatch   = totalMatches > 0
                ? Math.round((scored * 10.0 / totalMatches)) / 10.0 : 0;

        this.recentMatches = completedMatches.stream()
                .limit(10)
                .map(m -> new RecentMatch(m, player.getId()))
                .toList();
    }

    @Getter
    public static class RecentMatch {
        private final Long matchId;
        private final String tournamentTitle;
        private final String roundName;
        private final String opponentName;
        private final Integer myScore;
        private final Integer opponentScore;
        private final boolean win;
        private final String resultLabel;

        public RecentMatch(Match m, Long playerId) {
            this.matchId = m.getId();
            this.tournamentTitle = m.getTournament() != null ? m.getTournament().getTitle() : "-";
            this.roundName       = m.getRoundName();

            boolean isP1 = m.getPlayer1() != null && m.getPlayer1().getId().equals(playerId);
            this.myScore       = isP1 ? m.getScore1() : m.getScore2();
            this.opponentScore = isP1 ? m.getScore2() : m.getScore1();
            this.opponentName  = isP1
                    ? (m.getPlayer2() != null ? m.getPlayer2().getName() : "상대 없음")
                    : (m.getPlayer1() != null ? m.getPlayer1().getName() : "상대 없음");

            int my = myScore != null ? myScore : 0;
            int op = opponentScore != null ? opponentScore : 0;
            this.win         = my > op;
            this.resultLabel = this.win ? "승" : "패";
        }
    }
}
