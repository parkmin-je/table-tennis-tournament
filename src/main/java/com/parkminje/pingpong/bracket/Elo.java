package com.parkminje.pingpong.bracket;

/**
 * Elo 레이팅 계산.
 * 신규 선수(레이팅 경기 30회 미만)는 K=32 로 빠르게 수렴시키고, 이후에는 K=16.
 */
public final class Elo {

    public static final int INITIAL = 1500;
    private static final int PROVISIONAL_MATCHES = 30;

    private Elo() {
    }

    public static double expected(int rating, int opponent) {
        return 1.0 / (1.0 + Math.pow(10, (opponent - rating) / 400.0));
    }

    public static int k(int ratedMatches) {
        return ratedMatches < PROVISIONAL_MATCHES ? 32 : 16;
    }

    /** 경기 후 변동량 (승자는 +, 패자는 -) */
    public static int delta(int rating, int opponent, boolean won, int ratedMatches) {
        double score = won ? 1.0 : 0.0;
        return (int) Math.round(k(ratedMatches) * (score - expected(rating, opponent)));
    }
}
