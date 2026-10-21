package com.parkminje.pingpong.bootstrap;

import com.parkminje.pingpong.board.BoardCategory;
import com.parkminje.pingpong.board.PostService;
import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.club.ClubService;
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
import com.parkminje.pingpong.user.UserRepository;
import com.parkminje.pingpong.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 로컬 개발용 샘플 데이터 (app.sample-data=true 일 때, DB 가 비어 있으면 1회).
 * 실제 서비스 로직(신청 → 대진 → 경기 진행)을 그대로 호출해 만들기 때문에 기동 자체가 통합 점검 역할도 한다.
 */
@Slf4j
@Order(2)
@Component
@ConditionalOnProperty(prefix = "app", name = "sample-data", havingValue = "true")
@RequiredArgsConstructor
public class SampleDataLoader implements ApplicationRunner {

    private static final String[] NAMES = {
            "김민준", "이서연", "박지호", "최수아", "정우진", "강하은", "조현우", "윤지민", "장도윤", "임서윤",
            "한지훈", "오예린", "서준혁", "신다은", "권태양", "황보라", "안시우", "송유나", "류건우", "홍채원"};

    private final UserRepository userRepository;
    private final UserService userService;
    private final ClubService clubService;
    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final EntryService entryService;
    private final DrawService drawService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final PostService postService;

    private final Random random = new Random(20261021);

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("샘플 데이터 생성 시작");
        User created = userService.signUp("admin", "admin1234!", "admin@example.com", "관리자");
        userService.changeRole(created.getId(), created.getId(), Role.ADMIN);
        User admin = userService.get(created.getId());
        User staffCreated = userService.signUp("staff", "staff1234!", "staff@example.com", "운영요원");
        userService.changeRole(admin.getId(), staffCreated.getId(), Role.STAFF);
        User staff = userService.get(staffCreated.getId());

        List<Club> clubs = List.of(
                clubService.create("안산 스핀", "경기 안산시 단원구", "평일 저녁 정기 연습, 초보 환영"),
                clubService.create("고잔 탁구회", "경기 안산시 단원구 고잔동", "주말 리그전 운영"),
                clubService.create("상록 핑퐁", "경기 안산시 상록구", "구청 체육관 화·목 저녁"),
                clubService.create("호수공원 TTC", "경기 안산시 단원구", null));

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            Gender gender = i % 2 == 0 ? Gender.MALE : Gender.FEMALE;
            int division = 2 + (i % 6);
            Long clubId = clubs.get(i % clubs.size()).getId();
            if (i < 8) {
                User u = userService.signUp("user" + (i + 1), "user1234!", "user" + (i + 1) + "@example.com", NAMES[i]);
                players.add(playerService.saveMyProfile(u, new PlayerService.PlayerInput(NAMES[i], gender, 1975 + i * 2, division, null, clubId)));
            } else {
                players.add(playerService.create(new PlayerService.PlayerInput(NAMES[i], gender, 1970 + i, division, null, clubId)));
            }
        }
        // 동명이인 (같은 이름, 다른 클럽) — 이름 대신 엔트리로 승자를 기록하는지 확인용
        players.add(playerService.create(new PlayerService.PlayerInput("김민준", Gender.MALE, 1990, 4, null, clubs.get(2).getId())));

        LocalDate today = LocalDate.now();

        // 1) 종료된 토너먼트 (레이팅 반영)
        Tournament spring = tournamentService.create(spec("봄맞이 오픈 토너먼트", today.minusDays(40), TournamentFormat.SINGLE_ELIMINATION,
                null, null, 4), admin);
        open(spring);
        players.subList(0, 11).forEach(p -> entryService.register(spring.getId(), p.getId(), null, staff, true));
        entryService.findForTournament(spring.getId()).forEach(e -> entryService.setCheckIn(e.getId(), true));
        drawService.createInitialDraw(spring.getId());
        playAll(spring.getId(), Integer.MAX_VALUE);

        // 2) 진행 중인 예선 리그 + 본선 (일부 경기 완료, 일부 진행 중)
        Tournament live = tournamentService.create(spec("안산시장배 생활체육 탁구대회", today, TournamentFormat.GROUP_KNOCKOUT,
                null, null, 8), admin);
        open(live);
        players.forEach(p -> entryService.register(live.getId(), p.getId(), null, staff, true));
        entryService.findForTournament(live.getId()).forEach(e -> entryService.setCheckIn(e.getId(), true));
        drawService.createInitialDraw(live.getId());
        playAll(live.getId(), 14);
        matchService.autoAssign(live.getId());

        // 3) 접수 중인 부수 제한 대회 (회원 신청 일부)
        Tournament autumn = tournamentService.create(spec("가을 정기 리그 (3~5부)", today.plusDays(21), TournamentFormat.GROUP_KNOCKOUT,
                3, 5, 6), admin);
        open(autumn);
        for (int i = 0; i < 8; i++) {
            Player p = players.get(i);
            if (p.getDivision() >= 3 && p.getDivision() <= 5) {
                entryService.apply(autumn.getId(), userRepository.findByUsername("user" + (i + 1)).orElseThrow(), null);
            }
        }

        // 4) 준비중(비공개) 복식 대회
        TournamentSpec doubles = new TournamentSpec("연말 혼합 복식 대회", "연말 친선 복식 대회입니다. 파트너와 함께 신청하세요.",
                "안산올림픽기념관 체육관", today.plusDays(60).atTime(9, 0), today.plusDays(60).atTime(17, 0),
                today.plusDays(50).atTime(23, 59), TournamentFormat.SINGLE_ELIMINATION, EntryType.DOUBLES, 3, 32,
                4, 2, 6, 20000, null, null, GenderRestriction.ANY, false, true);
        tournamentService.create(doubles, admin);

        postService.write(admin, BoardCategory.NOTICE, "대회 운영 안내 (꼭 읽어주세요)",
                "1. 참가 신청 전 '내 정보 > 선수 프로필'에서 성별·부수·클럽을 등록해 주세요.\n"
                        + "2. 대회 당일 접수처에서 체크인 후 대진이 발표됩니다.\n"
                        + "3. 경기 호출은 알림과 현황판으로 안내됩니다. 호출 후 5분 내 미출석 시 부전패 처리될 수 있습니다.");
        postService.write(staff, BoardCategory.NOTICE, "안산시장배 테이블 배치 안내", "1~4번 테이블은 A구역, 5~8번 테이블은 B구역입니다.");
        User u1 = userRepository.findByUsername("user1").orElseThrow();
        postService.write(u1, BoardCategory.FREE, "이번 주 토요일 연습 같이 하실 분", "고잔 체육관 오후 2시에 모입니다. 초보도 환영합니다!");

        log.info("샘플 데이터 생성 완료 — admin / admin1234!, staff / staff1234!, user1~8 / user1234!");
    }

    private TournamentSpec spec(String title, LocalDate day, TournamentFormat format, Integer divMin, Integer divMax, int tables) {
        LocalDateTime start = day.atTime(LocalTime.of(9, 0));
        return new TournamentSpec(title, title + " — 안산시 탁구협회 주최 생활체육 대회입니다.\n참가비는 현장에서 납부합니다.",
                "안산올림픽기념관 체육관", start, day.atTime(LocalTime.of(18, 0)), start.minusDays(3),
                format, EntryType.SINGLES, format == TournamentFormat.GROUP_KNOCKOUT ? 5 : 3, 32, 4, 2, tables, 10000,
                divMin, divMax, GenderRestriction.ANY, false, true);
    }

    private void open(Tournament t) {
        tournamentService.transition(t.getId(), TournamentStatus.RECRUITING);
    }

    /** 순서대로 최대 limit 경기를 진행 (테이블 배정 → 결과 입력) */
    private void playAll(Long tournamentId, int limit) {
        int played = 0;
        while (played < limit) {
            List<Match> ready = matchRepository.findByTournamentIdOrderByPlayOrderAsc(tournamentId).stream()
                    .filter(m -> m.isScheduled() && m.hasBothEntries()).toList();
            if (ready.isEmpty()) {
                if (tournamentService.get(tournamentId).hasGroupStage()
                        && matchRepository.countByTournamentIdAndStage(tournamentId, com.parkminje.pingpong.match.MatchStage.KNOCKOUT) == 0
                        && tournamentService.get(tournamentId).getStatus() == TournamentStatus.IN_PROGRESS) {
                    drawService.createKnockoutFromGroups(tournamentId);
                    continue;
                }
                return;
            }
            Match m = ready.get(0);
            matchService.start(m.getId(), 1);
            matchService.record(m.getId(), ResultInput.games(randomScore(m, tournamentService.get(tournamentId).getBestOf())));
            played++;
        }
    }

    /** 레이팅·부수가 높은 쪽이 이길 확률이 높은 그럴듯한 게임 점수 */
    private String randomScore(Match m, int bestOf) {
        Player a = m.getEntry1().getPlayer();
        Player b = m.getEntry2().getPlayer();
        double diff = (a.getRating() - b.getRating()) + (b.getDivision() - a.getDivision()) * 60.0;
        double p1 = 1 / (1 + Math.pow(10, -diff / 400.0));
        int need = GameScore.gamesToWin(bestOf);
        int w1 = 0;
        int w2 = 0;
        List<String> games = new ArrayList<>();
        while (w1 < need && w2 < need) {
            boolean first = random.nextDouble() < p1;
            int lose = random.nextInt(12);
            int win = 11;
            if (lose >= 10) {
                win = lose + 2;
            }
            games.add(first ? win + "-" + lose : lose + "-" + win);
            if (first) {
                w1++;
            } else {
                w2++;
            }
        }
        return String.join(",", games);
    }
}
