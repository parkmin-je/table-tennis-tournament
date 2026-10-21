package com.parkminje.pingpong;

import com.parkminje.pingpong.board.BoardCategory;
import com.parkminje.pingpong.board.Comment;
import com.parkminje.pingpong.board.Post;
import com.parkminje.pingpong.board.PostService;
import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.player.Gender;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.security.UserPrincipal;
import com.parkminje.pingpong.support.IntegrationTest;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentFormat;
import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 모든 화면이 실제 데이터로 렌더링되는지와 권한 차단을 확인 */
@AutoConfigureMockMvc
class WebPagesTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired PostService postService;

    User member;
    User staff;
    User admin;
    Player memberPlayer;
    Tournament groupLive;
    Tournament finished;
    Tournament recruitingDoubles;
    Club club;
    Post post;

    @BeforeEach
    void seed() {
        member = newUser(Role.USER);
        staff = newUser(Role.STAFF);
        admin = adminUser();
        club = newClub();
        memberPlayer = playerService.saveMyProfile(member, new PlayerService.PlayerInput(null, Gender.MALE, 1990, 4, "010", club.getId()));

        // <script> 이름: 화면에서 이스케이프되는지 확인
        List<Player> ps = new ArrayList<>(List.of(newPlayer("<script>alert(1)</script>", 3, club)));
        ps.add(memberPlayer);
        ps.addAll(players(6));

        groupLive = withEntries(spec(TournamentFormat.GROUP_KNOCKOUT, 4), ps);
        drawService.createInitialDraw(groupLive.getId());
        playNext(groupLive.getId(), "11-9,9-11,11-7,11-5");
        matchService.autoAssign(groupLive.getId());

        finished = withEntries(spec(TournamentFormat.SINGLE_ELIMINATION, 2), players(5));
        drawService.createInitialDraw(finished.getId());
        playAll(finished.getId());

        var s = spec(TournamentFormat.SINGLE_ELIMINATION, 2);
        recruitingDoubles = recruiting(new com.parkminje.pingpong.tournament.TournamentSpec(s.title(), "복식", null, s.startAt(), s.endAt(),
                s.registrationDeadline(), s.format(), com.parkminje.pingpong.tournament.EntryType.DOUBLES, 3, null, 4, 2, 2, 0,
                null, null, s.genderRestriction(), false, false));

        post = postService.write(member, BoardCategory.FREE, "<b>제목</b>", "내용\n줄바꿈");
        postService.comment(post.getId(), member, "댓글");
    }

    private RequestPostProcessor as(User u) {
        return user(new UserPrincipal(userService.get(u.getId())));
    }

    @Test
    void 공개_페이지는_비로그인으로_모두_렌더링된다() throws Exception {
        for (String url : List.of("/", "/tournaments", "/tournaments?status=IN_PROGRESS&q=테스트",
                "/tournament/" + groupLive.getId(), "/tournament/" + groupLive.getId() + "/bracket",
                "/tournament/" + groupLive.getId() + "/board",
                "/tournament/" + finished.getId(), "/tournament/" + finished.getId() + "/bracket",
                "/tournament/" + recruitingDoubles.getId(),
                "/players", "/players?q=선수&division=3", "/player/" + memberPlayer.getId(), "/ranking",
                "/clubs", "/club/" + club.getId(), "/board", "/board?category=FREE", "/board/" + post.getId(),
                "/calendar", "/user/login", "/user/signup")) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
        for (String f : List.of("stats", "tables", "queue", "recent", "groups", "bracket", "board", "progress")) {
            mvc.perform(get("/tournament/" + groupLive.getId() + "/fragment/" + f)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/search").param("q", "선수")).andExpect(status().isOk());
        mvc.perform(get("/api/calendar").param("start", "2020-01-01T00:00:00").param("end", "2040-01-01T00:00:00")).andExpect(status().isOk());
    }

    @Test
    void 선수_이름과_게시글은_HTML_이스케이프된다() throws Exception {
        mvc.perform(get("/tournament/" + groupLive.getId()))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
        mvc.perform(get("/tournament/" + groupLive.getId() + "/fragment/groups"))
                .andExpect(content().string(not(containsString("<script>alert(1)"))));
        mvc.perform(get("/board/" + post.getId()))
                .andExpect(content().string(not(containsString("<b>제목</b>"))));
    }

    @Test
    void 보안_헤더와_CSP() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void 회원_페이지() throws Exception {
        for (String url : List.of("/me", "/me/password", "/me/player", "/notifications", "/board/write")) {
            mvc.perform(get(url).with(as(member))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/notifications/unread-count").with(as(member))).andExpect(status().isOk());
    }

    @Test
    void 운영진_관리자_페이지() throws Exception {
        for (String url : List.of("/manage/tournaments/" + groupLive.getId(),
                "/manage/tournaments/" + groupLive.getId() + "?tab=matches",
                "/manage/tournaments/" + finished.getId() + "?tab=draw",
                "/manage/tournaments/" + recruitingDoubles.getId(),
                "/manage/players/new", "/manage/players/" + memberPlayer.getId() + "/edit",
                "/manage/clubs/new", "/manage/clubs/" + club.getId() + "/edit",
                "/manage/tournaments/" + groupLive.getId() + "/export/entries.csv",
                "/manage/tournaments/" + groupLive.getId() + "/export/results.csv")) {
            mvc.perform(get(url).with(as(staff))).andExpect(status().isOk());
        }
        Match any = matches(groupLive.getId()).get(0);
        mvc.perform(get("/manage/matches/" + any.getId()).with(as(staff))).andExpect(status().isOk());
        for (String url : List.of("/admin", "/admin/users", "/admin/tournaments", "/admin/tournaments/new",
                "/admin/tournaments/" + groupLive.getId() + "/edit")) {
            mvc.perform(get(url).with(as(admin))).andExpect(status().isOk());
        }
    }

    @Test
    void 권한_없는_접근은_차단된다() throws Exception {
        mvc.perform(get("/manage/tournaments/" + groupLive.getId())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/user/login"));
        mvc.perform(get("/manage/tournaments/" + groupLive.getId()).with(as(member))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(as(staff))).andExpect(status().isForbidden());
        mvc.perform(post("/manage/tournaments/" + groupLive.getId() + "/draw/reset").with(as(member)).with(csrf()))
                .andExpect(status().isForbidden());
        // CSRF 토큰 없는 POST 거부
        mvc.perform(post("/tournament/" + recruitingDoubles.getId() + "/apply").with(as(member))).andExpect(status().isForbidden());
        // API 는 비로그인 시 401
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
    }

    @Test
    void 남의_댓글은_삭제할_수_없다() throws Exception {
        User other = newUser(Role.USER);
        Comment c = postService.comments(post.getId()).get(0);
        mvc.perform(post("/board/comments/" + c.getId() + "/delete").with(as(other)).with(csrf())
                        .header("Referer", "http://localhost/board/" + post.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("flashError"));
        assertThat(postService.comments(post.getId())).hasSize(1);
    }

    @Test
    void 화면에서_경기_시작과_결과_입력() throws Exception {
        Match next = matches(groupLive.getId()).stream().filter(m -> m.isInProgress()).findFirst().orElseThrow();
        mvc.perform(post("/manage/matches/" + next.getId() + "/result").with(as(staff)).with(csrf())
                        .param("mode", "GAMES").param("gameScores", "11-3,11-4,11-5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        assertThat(matchRepository.findById(next.getId()).orElseThrow().isCompleted()).isTrue();

        // 잘못된 점수는 이전 화면으로 돌아가며 오류 메시지
        Match another = matches(groupLive.getId()).stream().filter(m -> m.isInProgress()).findFirst().orElseThrow();
        mvc.perform(post("/manage/matches/" + another.getId() + "/result").with(as(staff)).with(csrf())
                        .header("Referer", "http://localhost/manage/tournaments/" + groupLive.getId())
                        .param("mode", "GAMES").param("gameScores", "11-10,11-4,11-5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("flashError", containsString("1게임")));
    }

    @Test
    void 회원_참가신청_화면_흐름() throws Exception {
        Tournament open = recruiting(spec(TournamentFormat.SINGLE_ELIMINATION, 2));
        mvc.perform(post("/tournament/" + open.getId() + "/apply").with(as(member)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        mvc.perform(get("/tournament/" + open.getId()).with(as(member)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("신청 취소")));
    }
}
