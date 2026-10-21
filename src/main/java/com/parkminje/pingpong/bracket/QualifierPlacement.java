package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 조별리그 통과자를 본선 대진에 배치한다.
 * <ul>
 *   <li>조 1위 → 상위 시드, 조 2위 → 그다음 시드 (같은 순위끼리는 조 순서)</li>
 *   <li>같은 조 출신끼리는 가능한 한 늦게(이상적으로는 결승에서) 만나도록 같은 순위끼리 자리를 교환</li>
 * </ul>
 */
public final class QualifierPlacement {

    public record Qualifier<T>(T entry, int groupIndex, int place) {
    }

    private QualifierPlacement() {
    }

    public static <T> List<T> place(List<Qualifier<T>> qualifiers) {
        List<Qualifier<T>> seeded = new ArrayList<>(qualifiers);
        seeded.sort(Comparator.comparingInt((Qualifier<T> q) -> q.place())
                .thenComparingInt(Qualifier::groupIndex));

        List<Qualifier<T>> slots = KnockoutLayout.placeBySeed(seeded);
        int rounds = KnockoutLayout.rounds(slots.size());

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int a = 0; a < slots.size(); a++) {
                for (int b = a + 1; b < slots.size(); b++) {
                    Qualifier<T> qa = slots.get(a);
                    Qualifier<T> qb = slots.get(b);
                    if (qa == null || qb == null || qa.place() == 1 || qa.place() != qb.place()) {
                        continue;
                    }
                    long before = cost(slots, rounds);
                    slots.set(a, qb);
                    slots.set(b, qa);
                    if (cost(slots, rounds) < before) {
                        improved = true;
                    } else {
                        slots.set(a, qa);
                        slots.set(b, qb);
                    }
                }
            }
        }
        return slots.stream().map(q -> q == null ? null : q.entry()).toList();
    }

    /** 같은 조 출신이 일찍 만날수록 큰 비용 */
    static <T> long cost(List<Qualifier<T>> slots, int rounds) {
        long cost = 0;
        for (int a = 0; a < slots.size(); a++) {
            for (int b = a + 1; b < slots.size(); b++) {
                Qualifier<T> qa = slots.get(a);
                Qualifier<T> qb = slots.get(b);
                if (qa != null && qb != null && qa.groupIndex() == qb.groupIndex()) {
                    int meet = KnockoutLayout.meetingRound(a, b);
                    cost += 1L << (2 * (rounds - meet));
                }
            }
        }
        return cost;
    }

    /** 테스트·검증용: 같은 조끼리 1라운드에서 만나는 쌍이 있는지 */
    public static <T> boolean hasFirstRoundClash(List<T> slots, java.util.function.Function<T, Integer> groupOf) {
        for (int i = 0; i + 1 < slots.size(); i += 2) {
            T a = slots.get(i);
            T b = slots.get(i + 1);
            if (a != null && b != null && Objects.equals(groupOf.apply(a), groupOf.apply(b))) {
                return true;
            }
        }
        return false;
    }
}
