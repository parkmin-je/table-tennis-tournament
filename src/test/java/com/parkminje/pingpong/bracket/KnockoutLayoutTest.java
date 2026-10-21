package com.parkminje.pingpong.bracket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnockoutLayoutTest {

    @Test
    void 표준_시드_배치() {
        assertThat(KnockoutLayout.seedBySlot(2)).containsExactly(1, 2);
        assertThat(KnockoutLayout.seedBySlot(4)).containsExactly(1, 4, 2, 3);
        assertThat(KnockoutLayout.seedBySlot(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }

    @Test
    void 대진_크기는_2의_거듭제곱() {
        assertThat(KnockoutLayout.bracketSize(2)).isEqualTo(2);
        assertThat(KnockoutLayout.bracketSize(3)).isEqualTo(4);
        assertThat(KnockoutLayout.bracketSize(5)).isEqualTo(8);
        assertThat(KnockoutLayout.bracketSize(33)).isEqualTo(64);
        assertThat(KnockoutLayout.bracketSize(64)).isEqualTo(64);
        assertThatThrownBy(() -> KnockoutLayout.bracketSize(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 6, 7, 9, 12, 17, 31, 33, 50, 64, 100})
    void BYE끼리_붙는_경기가_없고_BYE는_상위시드에게(int n) {
        List<Integer> seeded = IntStream.rangeClosed(1, n).boxed().toList();
        List<Integer> slots = KnockoutLayout.placeBySeed(seeded);

        Set<Integer> placed = new HashSet<>();
        List<Integer> byeReceivers = new ArrayList<>();
        for (int i = 0; i < slots.size(); i += 2) {
            Integer a = slots.get(i);
            Integer b = slots.get(i + 1);
            assertThat(a != null || b != null).as("BYE vs BYE at match %d", i / 2).isTrue();
            if (a == null) byeReceivers.add(b);
            if (b == null) byeReceivers.add(a);
            if (a != null) placed.add(a);
            if (b != null) placed.add(b);
        }
        assertThat(placed).hasSize(n);
        int byes = slots.size() - n;
        assertThat(byeReceivers).containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, byes).boxed().toList());
    }

    @Test
    void 일번과_이번_시드는_결승에서_만난다() {
        int[] seeds = KnockoutLayout.seedBySlot(16);
        int slot1 = indexOf(seeds, 1);
        int slot2 = indexOf(seeds, 2);
        assertThat(KnockoutLayout.meetingRound(slot1, slot2)).isEqualTo(4);
        // 1·4번 시드는 준결승
        assertThat(KnockoutLayout.meetingRound(slot1, indexOf(seeds, 4))).isEqualTo(3);
    }

    @Test
    void 라운드_이름() {
        assertThat(KnockoutLayout.roundLabel(2)).isEqualTo("결승");
        assertThat(KnockoutLayout.roundLabel(4)).isEqualTo("준결승");
        assertThat(KnockoutLayout.roundLabel(8)).isEqualTo("8강");
        assertThat(KnockoutLayout.roundLabel(64)).isEqualTo("64강");
    }

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }
}
