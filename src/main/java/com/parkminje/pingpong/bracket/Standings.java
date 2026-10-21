package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * 조별리그 순위 산정 (ITTF 규정 3.7.5 방식).
 * <ol>
 *   <li>승점: 승 2점, 패 1점, 기권패(부전패) 0점</li>
 *   <li>승점이 같으면 동률자끼리의 경기만으로 다시 승점 → 게임 득실률 → 점수 득실률 비교</li>
 *   <li>일부만 갈리면 남은 동률자끼리 1단계부터 다시 비교</li>
 *   <li>그래도 같으면 시드(사전 순서)</li>
 * </ol>
 */
public final class Standings {

    /** 경기 결과 1건. walkover=true 이면 패자는 경기를 치르지 않은 것으로 본다. */
    public record Result<T>(T a, T b, T winner, int gamesA, int gamesB, int pointsA, int pointsB, boolean walkover) {
    }

    public static final class Row<T> {
        private final T entry;
        private int played;
        private int wins;
        private int losses;
        private int matchPoints;
        private int gamesWon;
        private int gamesLost;
        private int pointsWon;
        private int pointsLost;
        private int rank;

        Row(T entry) {
            this.entry = entry;
        }

        public T entry() { return entry; }
        public int played() { return played; }
        public int wins() { return wins; }
        public int losses() { return losses; }
        public int matchPoints() { return matchPoints; }
        public int gamesWon() { return gamesWon; }
        public int gamesLost() { return gamesLost; }
        public int pointsWon() { return pointsWon; }
        public int pointsLost() { return pointsLost; }
        public int rank() { return rank; }
    }

    private Standings() {
    }

    /**
     * @param seedOrder 시드 순(최종 동률 시 우선)으로 정렬된 조 참가자
     * @param results   완료된 경기 결과
     */
    public static <T> List<Row<T>> compute(List<T> seedOrder, List<Result<T>> results) {
        Map<T, Row<T>> rows = new LinkedHashMap<>();
        for (T e : seedOrder) {
            rows.put(e, new Row<>(e));
        }
        for (Result<T> r : results) {
            accumulate(rows.get(r.a()), rows.get(r.b()), r);
        }
        List<T> ordered = resolve(new ArrayList<>(seedOrder), results, 0, seedOrder);
        List<Row<T>> out = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Row<T> row = rows.get(ordered.get(i));
            row.rank = i + 1;
            out.add(row);
        }
        return out;
    }

    private static <T> void accumulate(Row<T> ra, Row<T> rb, Result<T> r) {
        if (ra == null || rb == null) {
            return;
        }
        boolean aWins = r.winner().equals(r.a());
        Row<T> w = aWins ? ra : rb;
        Row<T> l = aWins ? rb : ra;
        w.wins++;
        w.played++;
        w.matchPoints += 2;
        l.losses++;
        if (!r.walkover()) {
            l.played++;
            l.matchPoints += 1;
        }
        ra.gamesWon += r.gamesA();
        ra.gamesLost += r.gamesB();
        rb.gamesWon += r.gamesB();
        rb.gamesLost += r.gamesA();
        ra.pointsWon += r.pointsA();
        ra.pointsLost += r.pointsB();
        rb.pointsWon += r.pointsB();
        rb.pointsLost += r.pointsA();
    }

    private static <T> List<T> resolve(List<T> tied, List<Result<T>> all, int level, List<T> seedOrder) {
        if (tied.size() <= 1) {
            return tied;
        }
        if (level >= 3) {
            List<T> bySeed = new ArrayList<>(tied);
            bySeed.sort(Comparator.comparingInt(seedOrder::indexOf));
            return bySeed;
        }
        // 첫 단계(level 0, 전체 대상)는 전체 경기, 이후에는 동률자끼리의 경기만 사용
        Set<T> members = new HashSet<>(tied);
        List<Result<T>> among = all.stream()
                .filter(r -> members.contains(r.a()) && members.contains(r.b()))
                .toList();
        Map<T, Row<T>> mini = new LinkedHashMap<>();
        for (T e : tied) {
            mini.put(e, new Row<>(e));
        }
        for (Result<T> r : among) {
            accumulate(mini.get(r.a()), mini.get(r.b()), r);
        }

        ToDoubleFunction<T> key = switch (level) {
            case 0 -> e -> mini.get(e).matchPoints;
            case 1 -> e -> ratio(mini.get(e).gamesWon, mini.get(e).gamesLost);
            default -> e -> ratio(mini.get(e).pointsWon, mini.get(e).pointsLost);
        };

        // 키 내림차순으로 묶기
        Map<Double, List<T>> buckets = new java.util.TreeMap<>(Comparator.reverseOrder());
        for (T e : tied) {
            buckets.computeIfAbsent(key.applyAsDouble(e), k -> new ArrayList<>()).add(e);
        }

        List<T> result = new ArrayList<>();
        for (List<T> bucket : buckets.values()) {
            if (bucket.size() == 1) {
                result.addAll(bucket);
            } else if (bucket.size() == tied.size()) {
                result.addAll(resolve(bucket, all, level + 1, seedOrder));
            } else {
                result.addAll(resolve(bucket, all, 0, seedOrder));
            }
        }
        return result;
    }

    private static double ratio(int won, int lost) {
        if (lost == 0) {
            return won == 0 ? 0 : 1_000_000_000d + won;
        }
        return (double) won / lost;
    }
}
