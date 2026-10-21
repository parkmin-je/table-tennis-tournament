package com.parkminje.pingpong.bracket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAndQualifierTest {

    record P(int strength, String club) {
    }

    @ParameterizedTest
    @CsvSource({"10,4,3", "12,4,3", "5,4,2", "3,4,1", "7,3,3", "16,4,4", "2,4,1"})
    void 조_수_계산(int n, int groupSize, int expected) {
        assertThat(GroupDrawer.groupCount(n, groupSize)).isEqualTo(expected);
    }

    @Test
    void 스네이크_시딩으로_조_인원_차이는_최대_1() {
        List<P> players = IntStream.rangeClosed(1, 10).mapToObj(i -> new P(i, null)).toList();
        List<List<P>> groups = GroupDrawer.draw(players, 3, P::club);

        assertThat(groups).extracting(List::size).containsExactly(3, 3, 4);
        // 1번 포트: 1,2,3 / 2번 포트(역순): 6→A, 5→B, 4→C
        assertThat(groups.get(0).get(0).strength()).isEqualTo(1);
        assertThat(groups.get(2).get(0).strength()).isEqualTo(3);
        assertThat(groups.get(2).get(1).strength()).isEqualTo(4);
        assertThat(groups.get(0).get(1).strength()).isEqualTo(6);
    }

    @Test
    void 같은_클럽은_가능하면_다른_조로() {
        // 스네이크 그대로면 3번(수원)이 B조(수원), 4번(안산)이 A조(안산)로 겹친다
        List<P> players = List.of(
                new P(1, "안산"), new P(2, "수원"),
                new P(3, "수원"), new P(4, "안산"));
        List<List<P>> groups = GroupDrawer.draw(players, 2, P::club);
        for (List<P> g : groups) {
            assertThat(g.stream().map(P::club).distinct().count()).isEqualTo(g.size());
        }
    }

    @Test
    void 조_이름() {
        assertThat(GroupDrawer.groupName(0)).isEqualTo("A조");
        assertThat(GroupDrawer.groupName(25)).isEqualTo("Z조");
        assertThat(GroupDrawer.groupName(26)).isEqualTo("AA조");
    }

    @ParameterizedTest
    @CsvSource({"2", "3", "4", "5", "8"})
    void 리그전은_모든_쌍이_정확히_한번_그리고_한_라운드에_한경기(int n) {
        List<RoundRobin.Pairing> schedule = RoundRobin.schedule(n);
        assertThat(schedule).hasSize(n * (n - 1) / 2);
        Set<String> pairs = new HashSet<>();
        for (RoundRobin.Pairing p : schedule) {
            pairs.add(Math.min(p.a(), p.b()) + "-" + Math.max(p.a(), p.b()));
        }
        assertThat(pairs).hasSize(schedule.size());
        schedule.stream().collect(java.util.stream.Collectors.groupingBy(RoundRobin.Pairing::round))
                .values().forEach(round -> {
                    Set<Integer> seen = new HashSet<>();
                    round.forEach(p -> {
                        assertThat(seen.add(p.a())).isTrue();
                        assertThat(seen.add(p.b())).isTrue();
                    });
                });
    }

    @ParameterizedTest
    @CsvSource({"2,2", "3,2", "4,2", "5,2", "6,2", "8,2", "3,1", "16,2", "4,3"})
    void 같은_조_출신은_1라운드에서_만나지_않는다(int groups, int perGroup) {
        List<QualifierPlacement.Qualifier<String>> qualifiers = new ArrayList<>();
        for (int g = 0; g < groups; g++) {
            for (int place = 1; place <= perGroup; place++) {
                qualifiers.add(new QualifierPlacement.Qualifier<>(g + ":" + place, g, place));
            }
        }
        List<String> slots = QualifierPlacement.place(qualifiers);

        assertThat(slots.stream().filter(java.util.Objects::nonNull)).hasSize(groups * perGroup);
        if (groups > 1) {
            assertThat(QualifierPlacement.hasFirstRoundClash(slots, s -> Integer.parseInt(s.split(":")[0]))).isFalse();
        }
        // 조 1위는 모두 상위 시드 자리
        int[] seeds = KnockoutLayout.seedBySlot(slots.size());
        for (int slot = 0; slot < slots.size(); slot++) {
            String s = slots.get(slot);
            if (s != null && s.endsWith(":1")) {
                assertThat(seeds[slot]).isLessThanOrEqualTo(groups);
            }
        }
    }

    @Test
    void 두개조_1위2위는_반대편_블록으로_결승에서만_재대결() {
        List<QualifierPlacement.Qualifier<String>> q = List.of(
                new QualifierPlacement.Qualifier<>("A1", 0, 1), new QualifierPlacement.Qualifier<>("A2", 0, 2),
                new QualifierPlacement.Qualifier<>("B1", 1, 1), new QualifierPlacement.Qualifier<>("B2", 1, 2));
        List<String> slots = QualifierPlacement.place(q);
        assertThat(KnockoutLayout.meetingRound(slots.indexOf("A1"), slots.indexOf("A2"))).isEqualTo(2);
        assertThat(KnockoutLayout.meetingRound(slots.indexOf("B1"), slots.indexOf("B2"))).isEqualTo(2);
    }
}
