package com.parkminje.pingpong;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.ForbiddenException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryStatus;
import com.parkminje.pingpong.player.Gender;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.support.IntegrationTest;
import com.parkminje.pingpong.tournament.EntryType;
import com.parkminje.pingpong.tournament.GenderRestriction;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentFormat;
import com.parkminje.pingpong.tournament.TournamentSpec;
import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntryRulesTest extends IntegrationTest {

    private TournamentSpec restricted(int max, Integer divMin, Integer divMax, GenderRestriction gender, EntryType type) {
        TournamentSpec s = spec(TournamentFormat.SINGLE_ELIMINATION, 2);
        return new TournamentSpec(s.title(), null, null, s.startAt(), s.endAt(), s.registrationDeadline(), s.format(),
                type, 3, max, 4, 2, 2, 0, divMin, divMax, gender, false, false);
    }

    private User userWithPlayer(Gender gender, Integer division) {
        User u = newUser(Role.USER);
        playerService.saveMyProfile(u, new PlayerService.PlayerInput(null, gender, 1990, division, null, null));
        return u;
    }

    @Test
    void 선수_프로필이_없으면_신청_불가() {
        Tournament t = recruiting(spec(TournamentFormat.SINGLE_ELIMINATION, 2));
        User u = newUser(Role.USER);
        assertThatThrownBy(() -> entryService.apply(t.getId(), u, null)).hasMessageContaining("선수 프로필");
    }

    @Test
    void 부수_성별_정원_중복_규칙() {
        Tournament t = recruiting(restricted(2, 3, 5, GenderRestriction.FEMALE, EntryType.SINGLES));
        assertThatThrownBy(() -> entryService.apply(t.getId(), userWithPlayer(Gender.MALE, 4), null)).hasMessageContaining("여자부");
        assertThatThrownBy(() -> entryService.apply(t.getId(), userWithPlayer(Gender.FEMALE, 2), null)).hasMessageContaining("참가 부수");
        assertThatThrownBy(() -> entryService.apply(t.getId(), userWithPlayer(Gender.FEMALE, null), null)).hasMessageContaining("참가 부수");

        User ok = userWithPlayer(Gender.FEMALE, 3);
        Entry e = entryService.apply(t.getId(), ok, null);
        assertThat(e.getStatus()).isEqualTo(EntryStatus.APPLIED);
        assertThatThrownBy(() -> entryService.apply(t.getId(), ok, null)).hasMessageContaining("이미");

        entryService.apply(t.getId(), userWithPlayer(Gender.FEMALE, 5), null);
        assertThatThrownBy(() -> entryService.apply(t.getId(), userWithPlayer(Gender.FEMALE, 4), null)).hasMessageContaining("정원");
    }

    @Test
    void 취소_후_재신청하면_기존_엔트리를_되살린다() {
        Tournament t = recruiting(spec(TournamentFormat.SINGLE_ELIMINATION, 2));
        User u = userWithPlayer(Gender.MALE, 4);
        Entry e = entryService.apply(t.getId(), u, null);
        entryService.cancelByUser(e.getId(), u);
        assertThat(entryService.find(e.getId()).getStatus()).isEqualTo(EntryStatus.WITHDRAWN);

        Entry again = entryService.apply(t.getId(), u, null);
        assertThat(again.getId()).isEqualTo(e.getId());
        assertThat(entryService.find(e.getId()).getStatus()).isEqualTo(EntryStatus.APPLIED);
    }

    @Test
    void 남의_신청은_취소할_수_없다() {
        Tournament t = recruiting(spec(TournamentFormat.SINGLE_ELIMINATION, 2));
        Entry e = entryService.apply(t.getId(), userWithPlayer(Gender.MALE, 4), null);
        User other = newUser(Role.USER);
        assertThatThrownBy(() -> entryService.cancelByUser(e.getId(), other)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 복식은_파트너가_필요하고_파트너도_중복_참가_불가() {
        Tournament t = recruiting(restricted(10, null, null, GenderRestriction.ANY, EntryType.DOUBLES));
        User u = userWithPlayer(Gender.MALE, 4);
        var partner = newPlayer("파트너", 4, null);
        assertThatThrownBy(() -> entryService.apply(t.getId(), u, null)).hasMessageContaining("파트너");
        Entry e = entryService.apply(t.getId(), u, partner.getId());
        assertThat(e.getDisplayName()).contains("파트너");

        User u2 = userWithPlayer(Gender.MALE, 4);
        assertThatThrownBy(() -> entryService.apply(t.getId(), u2, partner.getId())).hasMessageContaining("이미");
    }

    @Test
    void 대진_생성_후에는_엔트리_추가와_확정변경_불가() {
        Tournament t = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 2), players(3));
        drawService.createInitialDraw(t.getId());
        var late = newPlayer("지각생", 3, null);
        assertThatThrownBy(() -> entryService.register(t.getId(), late.getId(), null, adminUser(), true))
                .isInstanceOf(BusinessException.class);
        Entry any = entryService.findForTournament(t.getId()).get(0);
        assertThatThrownBy(() -> entryService.changeStatus(any.getId(), EntryStatus.REJECTED)).isInstanceOf(BusinessException.class);
    }
}
