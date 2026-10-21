package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 탁구 경기 점수 규칙 (ITTF 기준).
 * <ul>
 *   <li>한 게임은 11점 선취, 10:10 이후에는 2점 차</li>
 *   <li>bestOf 게임 중 과반(예: 5게임제 → 3게임)을 먼저 이기면 승리</li>
 * </ul>
 * 게임별 점수 없이 게임 수(3:1)만 입력하는 간편 입력도 허용한다.
 */
public record GameScore(int games1, int games2, List<int[]> games) {

    public GameScore {
        games = List.copyOf(games);
    }

    public int winnerSide() {
        return games1 > games2 ? 1 : 2;
    }

    public int points1() {
        return games.stream().mapToInt(g -> g[0]).sum();
    }

    public int points2() {
        return games.stream().mapToInt(g -> g[1]).sum();
    }

    public boolean hasGameDetail() {
        return !games.isEmpty();
    }

    /** "11-9,8-11,11-5" 형태 (게임 상세가 없으면 null) */
    public String format() {
        if (games.isEmpty()) {
            return null;
        }
        return games.stream().map(g -> g[0] + "-" + g[1]).collect(Collectors.joining(","));
    }

    public static int gamesToWin(int bestOf) {
        return bestOf / 2 + 1;
    }

    /**
     * 게임별 점수 문자열을 해석·검증한다.
     *
     * @param text "11-9, 8-11, 11-5" (구분자: 쉼표/공백, 점수 구분: - 또는 :)
     */
    public static GameScore parse(String text, int bestOf) {
        validateBestOf(bestOf);
        if (text == null || text.isBlank()) {
            throw new ScoreException("게임 점수를 입력하세요.");
        }
        List<int[]> games = new ArrayList<>();
        for (String token : text.trim().split("[,\\s/]+")) {
            if (token.isBlank()) {
                continue;
            }
            String[] parts = token.split("[-:]");
            if (parts.length != 2) {
                throw new ScoreException("점수 형식이 올바르지 않습니다: " + token + " (예: 11-9)");
            }
            try {
                games.add(new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())});
            } catch (NumberFormatException e) {
                throw new ScoreException("점수는 숫자로 입력하세요: " + token);
            }
        }
        return ofGames(games, bestOf);
    }

    public static GameScore ofGames(List<int[]> games, int bestOf) {
        validateBestOf(bestOf);
        int need = gamesToWin(bestOf);
        int w1 = 0;
        int w2 = 0;
        for (int i = 0; i < games.size(); i++) {
            if (w1 == need || w2 == need) {
                throw new ScoreException("승부가 이미 결정된 뒤의 게임 점수가 있습니다. (" + (i + 1) + "게임)");
            }
            int[] g = games.get(i);
            validateGame(g[0], g[1], i + 1);
            if (g[0] > g[1]) {
                w1++;
            } else {
                w2++;
            }
        }
        if (w1 != need && w2 != need) {
            throw new ScoreException(bestOf + "게임제는 " + need + "게임을 먼저 이겨야 경기가 끝납니다. (현재 " + w1 + ":" + w2 + ")");
        }
        return new GameScore(w1, w2, games);
    }

    /** 게임 수만으로 결과 입력 (예: 3:1) */
    public static GameScore ofGameCount(int games1, int games2, int bestOf) {
        validateBestOf(bestOf);
        int need = gamesToWin(bestOf);
        if (games1 < 0 || games2 < 0) {
            throw new ScoreException("게임 수는 0 이상이어야 합니다.");
        }
        boolean valid = (games1 == need && games2 < need) || (games2 == need && games1 < need);
        if (!valid) {
            throw new ScoreException(bestOf + "게임제의 결과는 한쪽이 " + need + "게임을 이겨야 합니다. (입력: " + games1 + ":" + games2 + ")");
        }
        return new GameScore(games1, games2, List.of());
    }

    static void validateGame(int a, int b, int gameNo) {
        if (a < 0 || b < 0) {
            throw new ScoreException(gameNo + "게임: 점수는 0 이상이어야 합니다.");
        }
        int hi = Math.max(a, b);
        int lo = Math.min(a, b);
        boolean normal = hi == 11 && lo <= 9;
        boolean deuce = lo >= 10 && hi - lo == 2;
        if (!normal && !deuce) {
            throw new ScoreException(gameNo + "게임 " + a + "-" + b + ": 11점 선취(10:10 이후 2점 차) 규칙에 맞지 않습니다.");
        }
    }

    private static void validateBestOf(int bestOf) {
        if (bestOf < 1 || bestOf % 2 == 0 || bestOf > 9) {
            throw new IllegalArgumentException("게임 수는 1~9 사이의 홀수여야 합니다: " + bestOf);
        }
    }

    public static List<int[]> parseStored(String stored) {
        List<int[]> games = new ArrayList<>();
        if (stored == null || stored.isBlank()) {
            return games;
        }
        for (String token : stored.split(",")) {
            String[] p = token.split("-");
            games.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])});
        }
        return games;
    }

    public static class ScoreException extends RuntimeException {
        public ScoreException(String message) {
            super(message);
        }
    }
}
