package com.parkminje.pingpong.bracket;

import java.util.ArrayList;
import java.util.List;

/**
 * 리그전 경기 순서 (서클 방식 / 베르거 표).
 * 같은 선수가 연속으로 경기하지 않도록 라운드를 나눈다.
 */
public final class RoundRobin {

    public record Pairing(int round, int a, int b) {
    }

    private RoundRobin() {
    }

    /** 참가자 인덱스 0..n-1 의 모든 대진을 라운드 순으로 반환 */
    public static List<Pairing> schedule(int n) {
        List<Pairing> result = new ArrayList<>();
        if (n < 2) {
            return result;
        }
        int size = n % 2 == 0 ? n : n + 1;   // 홀수면 휴식(가상 참가자) 추가
        int[] ring = new int[size];
        for (int i = 0; i < size; i++) {
            ring[i] = i;
        }
        for (int round = 1; round < size; round++) {
            for (int i = 0; i < size / 2; i++) {
                int a = ring[i];
                int b = ring[size - 1 - i];
                if (a < n && b < n) {
                    result.add(round % 2 == 0 ? new Pairing(round, b, a) : new Pairing(round, Math.min(a, b), Math.max(a, b)));
                }
            }
            // 0번 고정, 나머지 시계 방향 회전
            int last = ring[size - 1];
            System.arraycopy(ring, 1, ring, 2, size - 2);
            ring[1] = last;
        }
        return result;
    }
}
