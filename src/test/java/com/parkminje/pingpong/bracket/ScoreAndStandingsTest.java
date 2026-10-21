package com.parkminje.pingpong.bracket;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreAndStandingsTest {

    @Test
    void 게임별_점수_해석() {
        GameScore s = GameScore.parse("11-9, 8-11 11:5 12-10", 5);
        assertThat(s.games1()).isEqualTo(3);
        assertThat(s.games2()).isEqualTo(1);
        assertThat(s.winnerSide()).isEqualTo(1);
        assertThat(s.points1()).isEqualTo(42);
        assertThat(s.format()).isEqualTo("11-9,8-11,11-5,12-10");
    }

    @Test
    void 잘못된_게임_점수는_거부() {
        assertThatThrownBy(() -> GameScore.parse("11-10,11-5,11-3", 5)).hasMessageContaining("1게임");
        assertThatThrownBy(() -> GameScore.parse("12-9,11-5,11-3", 5)).hasMessageContaining("1게임");
        assertThatThrownBy(() -> GameScore.parse("13-10,11-5,11-3", 5)).hasMessageContaining("1게임");
        assertThatThrownBy(() -> GameScore.parse("11-9,11-5", 5)).hasMessageContaining("3게임을 먼저");
        assertThatThrownBy(() -> GameScore.parse("11-9,11-5,11-3,11-2", 5)).hasMessageContaining("이미 결정");
        assertThatThrownBy(() -> GameScore.parse("abc", 5)).isInstanceOf(GameScore.ScoreException.class);
        assertThat(GameScore.parse("14-12,11-9", 3).games1()).isEqualTo(2);
    }

    @Test
    void 게임수_간편_입력() {
        assertThat(GameScore.ofGameCount(3, 1, 5).winnerSide()).isEqualTo(1);
        assertThat(GameScore.ofGameCount(0, 2, 3).winnerSide()).isEqualTo(2);
        assertThatThrownBy(() -> GameScore.ofGameCount(3, 3, 5)).isInstanceOf(GameScore.ScoreException.class);
        assertThatThrownBy(() -> GameScore.ofGameCount(2, 1, 5)).isInstanceOf(GameScore.ScoreException.class);
        assertThatThrownBy(() -> GameScore.ofGameCount(4, 0, 5)).isInstanceOf(GameScore.ScoreException.class);
    }

    @Test
    void 승점_순위() {
        // A 2승, B 1승1패, C 2패
        List<String> seed = List.of("C", "B", "A");
        List<Standings.Result<String>> rs = List.of(
                win("A", "B", 3, 1), win("A", "C", 3, 0), win("B", "C", 3, 2));
        List<Standings.Row<String>> rows = Standings.compute(seed, rs);
        assertThat(rows).extracting(Standings.Row::entry).containsExactly("A", "B", "C");
        assertThat(rows.get(0).matchPoints()).isEqualTo(4);
        assertThat(rows.get(2).matchPoints()).isEqualTo(2);
    }

    @Test
    void 세명_물고_물리면_게임_득실률로() {
        // A>B 3:0, B>C 3:1, C>A 3:2 → 모두 승점 3
        // 게임 득실: A 5:3, B 3:4, C 4:5
        List<Standings.Result<String>> rs = List.of(
                win("A", "B", 3, 0), win("B", "C", 3, 1), win("C", "A", 3, 2));
        List<Standings.Row<String>> rows = Standings.compute(List.of("A", "B", "C"), rs);
        assertThat(rows).extracting(Standings.Row::entry).containsExactly("A", "C", "B");
    }

    @Test
    void 두명_동률은_승자승() {
        // 4명 리그: A 3승, B·C 1승2패... B>C 이므로 B가 위
        List<Standings.Result<String>> rs = new ArrayList<>(List.of(
                win("A", "B", 3, 0), win("A", "C", 3, 0), win("A", "D", 3, 0),
                win("B", "C", 3, 2), win("D", "B", 3, 0), win("C", "D", 3, 0)));
        // B: 1승(C) 2패, C: 1승(D) 2패, D: 1승(B) 2패 → 3명 동률, 서로 물고 물림
        // 동률자끼리: B-C 3:2, D-B 3:0, C-D 3:0 → B 3:5, C 5:3, D 3:3 → C, D, B
        List<Standings.Row<String>> rows = Standings.compute(List.of("A", "B", "C", "D"), rs);
        assertThat(rows).extracting(Standings.Row::entry).containsExactly("A", "C", "D", "B");
    }

    @Test
    void 기권패는_승점_0() {
        List<Standings.Result<String>> rs = List.of(
                new Standings.Result<>("A", "B", "A", 0, 0, 0, 0, true),
                win("B", "C", 3, 0), win("C", "A", 3, 0));
        List<Standings.Row<String>> rows = Standings.compute(List.of("A", "B", "C"), rs);
        Standings.Row<String> b = rows.stream().filter(r -> r.entry().equals("B")).findFirst().orElseThrow();
        assertThat(b.matchPoints()).isEqualTo(2); // 기권패 0 + 승 2
        assertThat(b.played()).isEqualTo(1);
    }

    @Test
    void 엘로_변동() {
        assertThat(Elo.delta(1500, 1500, true, 0)).isEqualTo(16);
        assertThat(Elo.delta(1500, 1500, false, 0)).isEqualTo(-16);
        assertThat(Elo.delta(1800, 1400, true, 50)).isLessThan(3);
        assertThat(Elo.delta(1400, 1800, true, 50)).isGreaterThan(13);
    }

    private static Standings.Result<String> win(String w, String l, int gw, int gl) {
        return new Standings.Result<>(w, l, w, gw, gl, gw * 11, gl * 11, false);
    }
}
