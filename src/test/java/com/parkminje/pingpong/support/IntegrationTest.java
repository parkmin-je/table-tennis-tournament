package com.parkminje.pingpong.support;

import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.club.ClubService;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryService;
import com.parkminje.pingpong.match.DrawService;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.match.MatchService;
import com.parkminje.pingpong.match.ResultInput;
import com.parkminje.pingpong.player.Gender;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.tournament.EntryType;
import com.parkminje.pingpong.tournament.GenderRestriction;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentFormat;
import com.parkminje.pingpong.tournament.TournamentService;
import com.parkminje.pingpong.tournament.TournamentSpec;
import com.parkminje.pingpong.tournament.TournamentStatus;
import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import com.parkminje.pingpong.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** 통합 테스트 공통 (H2 메모리 DB, 테스트마다 새 데이터로 독립) */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected UserService userService;
    @Autowired protected PlayerService playerService;
    @Autowired protected ClubService clubService;
    @Autowired protected TournamentService tournamentService;
    @Autowired protected EntryService entryService;
    @Autowired protected DrawService drawService;
    @Autowired protected MatchService matchService;
    @Autowired protected MatchRepository matchRepository;

    protected User newUser(Role role) {
        int n = SEQ.incrementAndGet();
        User u = userService.signUp("u" + n + "_" + UUID.randomUUID().toString().substring(0, 6), "pass1234!", "u" + n + "_" + UUID.randomUUID() + "@t.io", "회원" + n);
        if (role != Role.USER) {
            User admin = adminUser();
            userService.changeRole(admin.getId(), u.getId(), role);
        }
        return userService.get(u.getId());
    }

    private User admin;

    protected User adminUser() {
        if (admin == null) {
            int n = SEQ.incrementAndGet();
            User u = userService.signUp("admin" + n + "_" + UUID.randomUUID().toString().substring(0, 6), "pass1234!", "a" + n + "_" + UUID.randomUUID() + "@t.io", "관리자");
            userService.changeRole(u.getId(), u.getId(), Role.ADMIN);
            admin = userService.get(u.getId());
        }
        return admin;
    }

    protected Club newClub() {
        return clubService.create("클럽" + SEQ.incrementAndGet() + UUID.randomUUID().toString().substring(0, 4), "안산", null);
    }

    protected Player newPlayer(String name, Integer division, Club club) {
        return playerService.create(new PlayerService.PlayerInput(name, Gender.MALE, 1990, division, null, club == null ? null : club.getId()));
    }

    protected List<Player> players(int n) {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(newPlayer("선수" + SEQ.incrementAndGet(), 3, null));
        }
        return list;
    }

    protected TournamentSpec spec(TournamentFormat format, int tables) {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withNano(0);
        return new TournamentSpec("테스트 대회 " + SEQ.incrementAndGet(), null, "체육관", start, start.plusHours(8),
                start.minusDays(1), format, EntryType.SINGLES, 5, null, 4, 2, tables, 0, null, null,
                GenderRestriction.ANY, false, false);
    }

    protected Tournament recruiting(TournamentSpec spec) {
        Tournament t = tournamentService.create(spec, adminUser());
        tournamentService.transition(t.getId(), TournamentStatus.RECRUITING);
        return tournamentService.get(t.getId());
    }

    protected Tournament withEntries(TournamentSpec spec, List<Player> players) {
        Tournament t = recruiting(spec);
        for (Player p : players) {
            entryService.register(t.getId(), p.getId(), null, adminUser(), true);
        }
        return t;
    }

    protected List<Match> matches(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByPlayOrderAsc(tournamentId);
    }

    /** 대기 경기 중 첫 경기 진행. 진행한 경기가 없으면 false */
    protected boolean playNext(Long tournamentId, String scores) {
        for (Match m : matches(tournamentId)) {
            if (m.isScheduled() && m.hasBothEntries()) {
                matchService.start(m.getId(), 1);
                matchService.record(m.getId(), ResultInput.games(scores));
                return true;
            }
        }
        return false;
    }

    /** entry1 이 항상 3:0 으로 이기도록 모두 진행 */
    protected int playAll(Long tournamentId) {
        int n = 0;
        while (playNext(tournamentId, "11-5,11-6,11-7")) {
            n++;
        }
        return n;
    }

    protected Entry entryOf(Long tournamentId, Player p) {
        return entryService.findForTournament(tournamentId).stream().filter(e -> e.getPlayer().getId().equals(p.getId())).findFirst().orElseThrow();
    }
}
