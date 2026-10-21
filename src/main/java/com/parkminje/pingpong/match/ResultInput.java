package com.parkminje.pingpong.match;

import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.common.BusinessException;

/**
 * 결과 입력값.
 * <ul>
 *   <li>GAMES: 게임별 점수 "11-9,8-11,11-5,11-7"</li>
 *   <li>COUNT: 게임 수만 (3:1)</li>
 *   <li>RETIRED: 경기 도중 기권 — 승자 지정, 그때까지의 게임 수는 선택</li>
 *   <li>WALKOVER: 부전승 — 승자 지정</li>
 * </ul>
 */
public record ResultInput(Mode mode, String gameScores, Integer games1, Integer games2, Integer winnerSide) {

    public enum Mode {
        GAMES, COUNT, RETIRED, WALKOVER
    }

    public static ResultInput games(String scores) {
        return new ResultInput(Mode.GAMES, scores, null, null, null);
    }

    public static ResultInput count(int g1, int g2) {
        return new ResultInput(Mode.COUNT, null, g1, g2, null);
    }

    public static ResultInput walkover(int winnerSide) {
        return new ResultInput(Mode.WALKOVER, null, null, null, winnerSide);
    }

    public static ResultInput retired(int winnerSide, Integer g1, Integer g2) {
        return new ResultInput(Mode.RETIRED, null, g1, g2, winnerSide);
    }

    public ResultType resultType() {
        return switch (mode) {
            case GAMES, COUNT -> ResultType.NORMAL;
            case RETIRED -> ResultType.RETIRED;
            case WALKOVER -> ResultType.WALKOVER;
        };
    }

    /** 점수 검증 후 GameScore (부전승은 null) */
    public GameScore toScore(int bestOf) {
        return switch (mode) {
            case GAMES -> GameScore.parse(gameScores, bestOf);
            case COUNT -> GameScore.ofGameCount(require(games1), require(games2), bestOf);
            case RETIRED -> {
                int g1 = games1 == null ? 0 : games1;
                int g2 = games2 == null ? 0 : games2;
                int need = GameScore.gamesToWin(bestOf);
                if (g1 < 0 || g2 < 0 || g1 >= need || g2 >= need) {
                    throw new BusinessException("기권 시점의 게임 수가 올바르지 않습니다.");
                }
                yield new GameScore(g1, g2, java.util.List.of());
            }
            case WALKOVER -> null;
        };
    }

    public int resolveWinnerSide(GameScore score) {
        if (mode == Mode.RETIRED || mode == Mode.WALKOVER) {
            if (winnerSide == null || (winnerSide != 1 && winnerSide != 2)) {
                throw new BusinessException("승자를 선택하세요.");
            }
            return winnerSide;
        }
        return score.winnerSide();
    }

    private static int require(Integer v) {
        if (v == null) {
            throw new BusinessException("게임 수를 입력하세요.");
        }
        return v;
    }
}
