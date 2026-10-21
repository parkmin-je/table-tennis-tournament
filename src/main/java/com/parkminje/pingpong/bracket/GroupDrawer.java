package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 조 편성 (스네이크 시딩 + 같은 클럽 분산).
 * <p>
 * 강한 순서로 정렬된 참가자를 A→B→C→C→B→A … 순으로 배정한다.
 * 같은 포트(같은 줄) 안에서 클럽이 겹치면, 겹침이 줄어드는 경우에만 포트 내 다른 참가자와 자리를 맞바꾼다.
 * 결과적으로 각 조 인원 차이는 최대 1명이다.
 */
public final class GroupDrawer {

    private GroupDrawer() {
    }

    public static int groupCount(int entrants, int groupSize) {
        if (groupSize < 2) {
            throw new IllegalArgumentException("조 인원은 2명 이상이어야 합니다.");
        }
        int groups = (int) Math.ceil(entrants / (double) groupSize);
        // 한 조에 2명 미만이 생기지 않도록 조 수를 줄인다
        while (groups > 1 && entrants / groups < 2) {
            groups--;
        }
        return Math.max(1, groups);
    }

    /**
     * @param strongestFirst 강한 순으로 정렬된 참가자
     * @param groups         조 수
     * @param clubOf         클럽 식별자 (없으면 null)
     */
    public static <T> List<List<T>> draw(List<T> strongestFirst, int groups, Function<T, Object> clubOf) {
        if (groups < 1) {
            throw new IllegalArgumentException("조 수는 1 이상이어야 합니다.");
        }
        List<List<T>> result = new ArrayList<>();
        for (int g = 0; g < groups; g++) {
            result.add(new ArrayList<>());
        }

        for (int start = 0, pot = 0; start < strongestFirst.size(); start += groups, pot++) {
            List<T> potEntries = new ArrayList<>(strongestFirst.subList(start, Math.min(start + groups, strongestFirst.size())));
            List<Integer> targets = new ArrayList<>();
            for (int i = 0; i < potEntries.size(); i++) {
                targets.add(pot % 2 == 0 ? i : groups - 1 - i);
            }
            reduceClubClashes(result, potEntries, targets, clubOf);
            for (int i = 0; i < potEntries.size(); i++) {
                result.get(targets.get(i)).add(potEntries.get(i));
            }
        }
        return result;
    }

    private static <T> void reduceClubClashes(List<List<T>> groups, List<T> pot, List<Integer> targets,
                                              Function<T, Object> clubOf) {
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < pot.size(); i++) {
                for (int j = i + 1; j < pot.size(); j++) {
                    int before = clash(groups, pot.get(i), targets.get(i), clubOf)
                            + clash(groups, pot.get(j), targets.get(j), clubOf);
                    int after = clash(groups, pot.get(i), targets.get(j), clubOf)
                            + clash(groups, pot.get(j), targets.get(i), clubOf);
                    if (after < before) {
                        Integer tmp = targets.get(i);
                        targets.set(i, targets.get(j));
                        targets.set(j, tmp);
                        improved = true;
                    }
                }
            }
        }
    }

    private static <T> int clash(List<List<T>> groups, T entry, int group, Function<T, Object> clubOf) {
        Object club = clubOf.apply(entry);
        if (club == null) {
            return 0;
        }
        int count = 0;
        for (T other : groups.get(group)) {
            if (Objects.equals(club, clubOf.apply(other))) {
                count++;
            }
        }
        return count;
    }

    /** 0 → "A조", 25 → "Z조", 26 → "AA조" */
    public static String groupName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb + "조";
    }
}
