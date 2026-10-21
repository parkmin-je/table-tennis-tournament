package com.parkminje.pingpong;

import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryStatus;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchStage;
import com.parkminje.pingpong.match.ResultInput;
import com.parkminje.pingpong.match.ResultType;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.RatingHistoryRepository;
import com.parkminje.pingpong.support.IntegrationTest;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentFormat;
import com.parkminje.pingpong.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentFlowTest extends IntegrationTest {

    @Autowired
    RatingHistoryRepository ratingHistoryRepository;

    @Test
    void 토너먼트_5명_BYE_자동처리부터_우승과_레이팅까지() {
        // 동명이인 2명 포함
        Club a = newClub();
        Club b = newClub();
        List<Player> ps = List.of(newPlayer("김민준", 3, a), newPlayer("김민준", 3, b),
                newPlayer("이서연", 3, a), newPlayer("박지호", 3, b), newPlayer("최수아", 3, null));
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 4), ps);

        drawService.createInitialDraw(t.getId());

        List<Match> ms = matches(t.getId());
        assertThat(ms).hasSize(7); // 8강 4 + 준결승 2 + 결승 1
        List<Match> byes = ms.stream().filter(m -> m.getResultType() == ResultType.BYE).toList();
        assertThat(byes).hasSize(3);
        assertThat(tournamentService.get(t.getId()).getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);

        int played = playAll(t.getId());
        assertThat(played).isEqualTo(4); // 8강 1경기 + 준결승 2 + 결승 1

        Tournament done = tournamentService.get(t.getId());
        assertThat(done.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(done.isRatingApplied()).isTrue();
        assertThat(ratingHistoryRepository.findByTournamentId(t.getId())).isNotEmpty();

        // 모든 본선 승자는 그 경기 참가자 중 하나 (이름이 아니라 엔트리로 기록)
        for (Match m : matches(t.getId())) {
            assertThat(m.isCompleted()).isTrue();
            assertThat(m.involves(m.getWinner())).isTrue();
        }
        // 동명이인 두 명의 엔트리는 서로 다르게 유지된다
        assertThat(entryOf(t.getId(), ps.get(0)).getId()).isNotEqualTo(entryOf(t.getId(), ps.get(1)).getId());
    }

    @Test
    void 리그와_본선_전체_흐름_같은조는_첫판에서_안만난다() {
        Tournament t = withEntries(spec(TournamentFormat.GROUP_KNOCKOUT, 6), players(10));
        drawService.createInitialDraw(t.getId());

        List<Match> group = matches(t.getId());
        assertThat(group).allMatch(m -> m.getStage() == MatchStage.GROUP);
        assertThat(group).hasSize(6 + 3 + 3); // 4명조 1개, 3명조 2개

        assertThatThrownBy(() -> drawService.createKnockoutFromGroups(t.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("남아 있습니다");

        playAll(t.getId());
        drawService.createKnockoutFromGroups(t.getId());

        List<Match> firstRound = matches(t.getId()).stream()
                .filter(m -> m.getStage() == MatchStage.KNOCKOUT && m.getRoundNo() == 1)
                .sorted(Comparator.comparingInt(Match::getBracketPos)).toList();
        assertThat(firstRound).hasSize(4); // 6명 → 8강, BYE 2
        for (Match m : firstRound) {
            if (m.hasBothEntries()) {
                assertThat(m.getEntry1().getGroup().getId()).isNotEqualTo(m.getEntry2().getGroup().getId());
            }
        }

        playAll(t.getId());
        Tournament done = tournamentService.get(t.getId());
        assertThat(done.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(done.getChampion()).isNotNull();
    }

    @Test
    void 결과_수정시_다음경기_자리가_바뀌고_다음경기가_시작되면_수정불가() {
        List<Player> ps = players(4);
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 4), ps);
        drawService.createInitialDraw(t.getId());

        Match semi = matches(t.getId()).stream().filter(m -> m.getRoundNo() == 1).findFirst().orElseThrow();
        matchService.start(semi.getId(), 1);
        matchService.record(semi.getId(), ResultInput.games("11-3,11-3,11-3"));
        Match after = matchRepository.findWithDetailsById(semi.getId()).orElseThrow();
        Entry firstWinner = after.getWinner();
        Entry loser = after.getLoser();

        Match fin = matchRepository.findWithDetailsById(after.getNextMatch().getId()).orElseThrow();
        assertThat(fin.entryAt(after.getNextSlot())).isEqualTo(firstWinner);

        // 승자 뒤집기 (0:3)
        matchService.correct(semi.getId(), ResultInput.games("3-11,3-11,3-11"));
        fin = matchRepository.findWithDetailsById(fin.getId()).orElseThrow();
        assertThat(fin.entryAt(after.getNextSlot())).isEqualTo(loser);

        // 다른 준결승도 끝내고 결승 시작 → 준결승 수정 불가
        Match other = matches(t.getId()).stream().filter(m -> m.getRoundNo() == 1 && !m.getId().equals(semi.getId())).findFirst().orElseThrow();
        matchService.start(other.getId(), 2);
        matchService.record(other.getId(), ResultInput.count(3, 1));
        matchService.start(fin.getId(), 1);
        assertThatThrownBy(() -> matchService.correct(semi.getId(), ResultInput.games("11-3,11-3,11-3")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("다음 경기");
    }

    @Test
    void 결승_결과를_수정하면_우승자와_레이팅이_다시_계산된다() {
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 2), players(2));
        drawService.createInitialDraw(t.getId());
        Match fin = matches(t.getId()).get(0);
        matchService.start(fin.getId(), 1);
        matchService.record(fin.getId(), ResultInput.count(3, 0));
        Tournament done = tournamentService.get(t.getId());
        int ratingAfterFirst = entryOf(t.getId(), fin.getEntry1().getPlayer()).getPlayer().getRating();
        assertThat(done.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(ratingAfterFirst).isGreaterThan(1500);

        matchService.correct(fin.getId(), ResultInput.count(1, 3));
        Tournament corrected = tournamentService.get(t.getId());
        assertThat(corrected.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        Match fixedFinal = matchRepository.findWithDetailsById(fin.getId()).orElseThrow();
        assertThat(corrected.getChampion().getId()).isEqualTo(fixedFinal.getEntry2().getId());
        assertThat(playerService.get(fin.getEntry1().getPlayer().getId()).getRating()).isLessThan(1500);
        assertThat(ratingHistoryRepository.findByTournamentId(t.getId())).hasSize(2);
    }

    @Test
    void 대회중_기권하면_남은_경기는_상대_부전승() {
        List<Player> ps = players(4);
        Tournament t = withEntries(spec(TournamentFormat.GROUP_KNOCKOUT, 4), ps);
        drawService.createInitialDraw(t.getId());
        Entry quitter = entryOf(t.getId(), ps.get(0));

        entryService.withdraw(quitter.getId());

        List<Match> theirs = matches(t.getId()).stream().filter(m -> m.involves(quitter)).toList();
        assertThat(theirs).hasSize(3).allMatch(m -> m.isCompleted() && m.getResultType() == ResultType.WALKOVER && !m.isWinner(quitter));
        assertThat(entryService.find(quitter.getId()).getStatus()).isEqualTo(EntryStatus.WITHDRAWN);
    }

    @Test
    void 같은_테이블_중복배정과_경기중_선수_중복배정을_막는다() {
        Tournament t = withEntries(spec(TournamentFormat.GROUP_KNOCKOUT, 3), players(4));
        drawService.createInitialDraw(t.getId());
        List<Match> ms = matches(t.getId());
        Match m1 = ms.get(0);
        Match m2 = ms.get(1); // 같은 라운드 다른 선수
        matchService.start(m1.getId(), 1);
        assertThatThrownBy(() -> matchService.start(m2.getId(), 1)).hasMessageContaining("1번 테이블");
        assertThatThrownBy(() -> matchService.start(m2.getId(), 9)).hasMessageContaining("1~3");

        Match sameEntry = ms.stream().filter(m -> !m.getId().equals(m1.getId()) && (m.involves(m1.getEntry1()))).findFirst().orElseThrow();
        assertThatThrownBy(() -> matchService.start(sameEntry.getId(), 2)).hasMessageContaining("경기 중");

        int assigned = matchService.autoAssign(t.getId());
        assertThat(assigned).isEqualTo(1); // 4명 중 2명이 경기 중 → 남은 2명 경기 1개
    }

    @Test
    void 잘못된_점수는_거부되고_상태는_그대로() {
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 2), players(2));
        drawService.createInitialDraw(t.getId());
        Match m = matches(t.getId()).get(0);
        assertThatThrownBy(() -> matchService.record(m.getId(), ResultInput.count(3, 0)))
                .hasMessageContaining("시작");
        matchService.start(m.getId(), 1);
        assertThatThrownBy(() -> matchService.record(m.getId(), ResultInput.games("11-10,11-3,11-3")))
                .hasMessageContaining("1게임");
        assertThat(matchRepository.findById(m.getId()).orElseThrow().isInProgress()).isTrue();
    }

    @Test
    void 아무경기도_안했으면_대진_초기화_가능_시작후에는_불가() {
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 2), players(6));
        drawService.createInitialDraw(t.getId());
        drawService.resetDraw(t.getId());
        assertThat(matches(t.getId())).isEmpty();
        assertThat(tournamentService.get(t.getId()).getStatus()).isEqualTo(TournamentStatus.CLOSED);

        drawService.createInitialDraw(t.getId());
        Match m = matches(t.getId()).stream().filter(x -> x.isScheduled() && x.hasBothEntries()).findFirst().orElseThrow();
        matchService.start(m.getId(), 1);
        assertThatThrownBy(() -> drawService.resetDraw(t.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    void 체크인_필수_대회는_체크인한_엔트리만_대진에_들어간다() {
        var spec = spec(TournamentFormat.SINGLE_ELIMINATION, 2);
        spec = new com.parkminje.pingpong.tournament.TournamentSpec(spec.title(), null, null, spec.startAt(), spec.endAt(),
                spec.registrationDeadline(), spec.format(), spec.entryType(), 3, null, 4, 2, 2, 0, null, null,
                spec.genderRestriction(), false, true);
        List<Player> ps = players(5);
        Tournament t = withEntries(spec, ps);
        for (int i = 0; i < 3; i++) {
            entryService.setCheckIn(entryOf(t.getId(), ps.get(i)).getId(), true);
        }
        drawService.createInitialDraw(t.getId());
        long distinctEntries = matches(t.getId()).stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getEntry1(), m.getEntry2()))
                .filter(Objects::nonNull).map(Entry::getId).distinct().count();
        assertThat(distinctEntries).isEqualTo(3);
    }
}
