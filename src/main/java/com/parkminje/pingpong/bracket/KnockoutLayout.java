package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 싱글 엘리미네이션 대진 배치 규칙.
 * <p>
 * 표준 시드 배치(1번 시드와 2번 시드가 결승에서만 만남)를 사용하며,
 * 부전승(BYE)은 항상 상위 시드에게 돌아가므로 BYE끼리 붙는 경기는 생기지 않는다.
 */
public final class KnockoutLayout {

    private KnockoutLayout() {
    }

    /** n명 이상을 수용하는 가장 작은 2의 거듭제곱 (최소 2) */
    public static int bracketSize(int entrants) {
        if (entrants < 2) {
            throw new IllegalArgumentException("본선은 최소 2명이 필요합니다.");
        }
        int size = 2;
        while (size < entrants) {
            size <<= 1;
        }
        return size;
    }

    public static int rounds(int bracketSize) {
        return Integer.numberOfTrailingZeros(bracketSize);
    }

    /**
     * 슬롯(0-based) 별 시드 번호(1-based). 예: 8강 → [1, 8, 4, 5, 2, 7, 3, 6]
     */
    public static int[] seedBySlot(int bracketSize) {
        if (Integer.bitCount(bracketSize) != 1 || bracketSize < 2) {
            throw new IllegalArgumentException("대진 크기는 2의 거듭제곱이어야 합니다: " + bracketSize);
        }
        List<Integer> order = new ArrayList<>(List.of(1, 2));
        while (order.size() < bracketSize) {
            int n = order.size() * 2;
            List<Integer> next = new ArrayList<>(n);
            for (int seed : order) {
                next.add(seed);
                next.add(n + 1 - seed);
            }
            order = next;
        }
        return order.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 시드 순으로 정렬된 참가자 목록을 1라운드 슬롯에 배치한다. 빈 슬롯(null)은 BYE.
     */
    public static <T> List<T> placeBySeed(List<T> seeded) {
        int size = bracketSize(seeded.size());
        int[] seeds = seedBySlot(size);
        List<T> slots = new ArrayList<>(Collections.nCopies(size, null));
        for (int slot = 0; slot < size; slot++) {
            int seed = seeds[slot];
            if (seed <= seeded.size()) {
                slots.set(slot, seeded.get(seed - 1));
            }
        }
        return slots;
    }

    /**
     * 두 1라운드 슬롯의 참가자가 (둘 다 이긴다고 가정할 때) 만나게 되는 라운드 (1-based).
     */
    public static int meetingRound(int slotA, int slotB) {
        if (slotA == slotB) {
            throw new IllegalArgumentException("같은 슬롯입니다.");
        }
        return 32 - Integer.numberOfLeadingZeros(slotA ^ slotB);
    }

    /** 해당 라운드에 남은 인원 기준 라운드 이름 (예: 16 → "16강", 2 → "결승") */
    public static String roundLabel(int playersInRound) {
        if (playersInRound <= 2) {
            return "결승";
        }
        if (playersInRound == 4) {
            return "준결승";
        }
        return playersInRound + "강";
    }
}
