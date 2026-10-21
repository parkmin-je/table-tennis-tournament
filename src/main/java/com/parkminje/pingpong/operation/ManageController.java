package com.parkminje.pingpong.operation;

import com.parkminje.pingpong.club.ClubService;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryService;
import com.parkminje.pingpong.entry.EntryStatus;
import com.parkminje.pingpong.live.TournamentBoard;
import com.parkminje.pingpong.live.TournamentBoardService;
import com.parkminje.pingpong.match.DrawService;
import com.parkminje.pingpong.match.Match;
import com.parkminje.pingpong.match.MatchRepository;
import com.parkminje.pingpong.match.MatchService;
import com.parkminje.pingpong.match.ResultInput;
import com.parkminje.pingpong.player.Gender;
import com.parkminje.pingpong.player.PlayerRepository;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.tournament.TournamentService;
import com.parkminje.pingpong.tournament.TournamentStatus;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 운영진 대회 운영 콘솔 (엔트리 관리 → 대진 → 테이블 배정 → 결과 입력).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manage")
public class ManageController {

    private final TournamentService tournamentService;
    private final TournamentBoardService boardService;
    private final EntryService entryService;
    private final DrawService drawService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final WalkInService walkInService;
    private final PlayerRepository playerRepository;
    private final ClubService clubService;

    @GetMapping("/tournaments/{id}")
    public String console(@PathVariable Long id, @RequestParam(defaultValue = "entries") String tab, Model model) {
        TournamentBoard board = boardService.load(id);
        model.addAttribute("board", board);
        model.addAttribute("t", board.tournament());
        model.addAttribute("tab", tab);
        model.addAttribute("drawable", drawService.drawableEntries(id).size());
        model.addAttribute("players", playerRepository.findAllByOrderByNameAsc());
        model.addAttribute("clubs", clubService.all());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("entryStatuses", EntryStatus.values());
        return "manage/console";
    }

    // ── 대회 상태 ──────────────────────────────────────────

    @PostMapping("/tournaments/{id}/status")
    public String status(@PathVariable Long id, @RequestParam TournamentStatus status, RedirectAttributes ra) {
        tournamentService.transition(id, status);
        ra.addFlashAttribute("success", "대회 상태를 '" + status.getLabel() + "'(으)로 변경했습니다.");
        return console(id, "entries");
    }

    // ── 엔트리 ─────────────────────────────────────────────

    @PostMapping("/tournaments/{id}/entries")
    public String register(@PathVariable Long id, @RequestParam Long playerId, @RequestParam(required = false) Long partnerId,
                           @RequestParam(defaultValue = "false") boolean ignoreRestriction,
                           @LoginUser User staff, RedirectAttributes ra) {
        Entry e = entryService.register(id, playerId, partnerId, staff, ignoreRestriction);
        ra.addFlashAttribute("success", e.getDisplayName() + " 엔트리를 등록했습니다.");
        return console(id, "entries");
    }

    /** 현장 접수: 선수 등록 + 엔트리 등록을 한 번에 (단식) */
    @PostMapping("/tournaments/{id}/entries/quick")
    public String quickRegister(@PathVariable Long id, @RequestParam String name, @RequestParam Gender gender,
                                @RequestParam(required = false) Integer division, @RequestParam(required = false) Long clubId,
                                @RequestParam(required = false) String phone,
                                @RequestParam(defaultValue = "false") boolean ignoreRestriction,
                                @LoginUser User staff, RedirectAttributes ra) {
        if (tournamentService.get(id).isDoubles()) {
            throw new BusinessException("복식은 선수를 먼저 등록한 뒤 '기존 선수로 등록'을 사용하세요.");
        }
        Entry e = walkInService.register(id, new PlayerService.PlayerInput(name, gender, null, division, phone, clubId),
                staff, ignoreRestriction);
        ra.addFlashAttribute("success", e.getPlayer().getName() + " 선수를 등록하고 엔트리에 추가했습니다.");
        return console(id, "entries");
    }

    @PostMapping("/entries/{entryId}/status")
    public String entryStatus(@PathVariable Long entryId, @RequestParam EntryStatus status, RedirectAttributes ra) {
        Long tid = tournamentOf(entryId);
        entryService.changeStatus(entryId, status);
        ra.addFlashAttribute("success", "엔트리 상태를 '" + status.getLabel() + "'(으)로 변경했습니다.");
        return console(tid, "entries");
    }

    @PostMapping("/tournaments/{id}/entries/confirm-all")
    public String confirmAll(@PathVariable Long id, RedirectAttributes ra) {
        int n = entryService.confirmAll(id);
        ra.addFlashAttribute("success", n + "건을 확정했습니다.");
        return console(id, "entries");
    }

    @PostMapping("/entries/{entryId}/check-in")
    public String checkIn(@PathVariable Long entryId, @RequestParam boolean value) {
        Long tid = tournamentOf(entryId);
        entryService.setCheckIn(entryId, value);
        return console(tid, "entries");
    }

    @PostMapping("/entries/{entryId}/paid")
    public String paid(@PathVariable Long entryId, @RequestParam boolean value) {
        Long tid = tournamentOf(entryId);
        entryService.setPaid(entryId, value);
        return console(tid, "entries");
    }

    @PostMapping("/entries/{entryId}/seed")
    public String seed(@PathVariable Long entryId, @RequestParam(required = false) Integer seed,
                       @RequestParam(required = false) String memo, RedirectAttributes ra) {
        Long tid = tournamentOf(entryId);
        entryService.setSeedAndMemo(entryId, seed, memo);
        ra.addFlashAttribute("success", "시드/메모를 저장했습니다.");
        return console(tid, "entries");
    }

    @PostMapping("/entries/{entryId}/withdraw")
    public String withdraw(@PathVariable Long entryId, RedirectAttributes ra) {
        Long tid = tournamentOf(entryId);
        entryService.withdraw(entryId);
        ra.addFlashAttribute("success", "기권 처리했습니다. 남은 경기는 상대 부전승으로 처리됩니다.");
        return console(tid, "entries");
    }

    // ── 대진 ───────────────────────────────────────────────

    @PostMapping("/tournaments/{id}/draw")
    public String draw(@PathVariable Long id, RedirectAttributes ra) {
        drawService.createInitialDraw(id);
        ra.addFlashAttribute("success", "대진을 생성했습니다. 참가자에게 알림이 발송되었습니다.");
        return console(id, "matches");
    }

    @PostMapping("/tournaments/{id}/draw/knockout")
    public String drawKnockout(@PathVariable Long id, RedirectAttributes ra) {
        drawService.createKnockoutFromGroups(id);
        ra.addFlashAttribute("success", "예선 결과로 본선 대진을 생성했습니다.");
        return console(id, "matches");
    }

    @PostMapping("/tournaments/{id}/draw/reset")
    public String resetDraw(@PathVariable Long id, RedirectAttributes ra) {
        drawService.resetDraw(id);
        ra.addFlashAttribute("success", "대진을 초기화했습니다. 대회는 '접수마감' 상태로 돌아갑니다.");
        return console(id, "entries");
    }

    @PostMapping("/tournaments/{id}/draw/reset-knockout")
    public String resetKnockout(@PathVariable Long id, RedirectAttributes ra) {
        drawService.resetKnockout(id);
        ra.addFlashAttribute("success", "본선 대진을 초기화했습니다.");
        return console(id, "matches");
    }

    @PostMapping("/tournaments/{id}/auto-assign")
    public String autoAssign(@PathVariable Long id, RedirectAttributes ra) {
        int n = matchService.autoAssign(id);
        ra.addFlashAttribute(n > 0 ? "success" : "info", n > 0 ? n + "경기를 빈 테이블에 배정하고 선수를 호출했습니다." : "배정할 수 있는 경기 또는 빈 테이블이 없습니다.");
        return console(id, "matches");
    }

    // ── 경기 ───────────────────────────────────────────────

    /** 심판/운영진용 점수 입력 화면 (모바일) */
    @GetMapping("/matches/{matchId}")
    public String matchPage(@PathVariable Long matchId, Model model) {
        Match m = matchRepository.findWithDetailsById(matchId).orElseThrow(() -> NotFoundException.of("경기", matchId));
        model.addAttribute("m", m);
        model.addAttribute("t", m.getTournament());
        model.addAttribute("freeTables", freeTables(m.getTournament().getId()));
        return "manage/match";
    }

    @PostMapping("/matches/{matchId}/start")
    public String start(@PathVariable Long matchId, @RequestParam int table,
                        @RequestParam(required = false) String returnTo, RedirectAttributes ra) {
        Match m = matchService.start(matchId, table);
        ra.addFlashAttribute("success", table + "번 테이블에 배정하고 선수를 호출했습니다.");
        return back(returnTo, m);
    }

    @PostMapping("/matches/{matchId}/cancel-start")
    public String cancelStart(@PathVariable Long matchId, @RequestParam(required = false) String returnTo, RedirectAttributes ra) {
        matchService.cancelStart(matchId);
        ra.addFlashAttribute("success", "테이블 배정을 취소했습니다.");
        return back(returnTo, load(matchId));
    }

    @PostMapping("/matches/{matchId}/result")
    public String result(@PathVariable Long matchId, @RequestParam ResultInput.Mode mode,
                         @RequestParam(required = false) String gameScores,
                         @RequestParam(required = false) Integer games1, @RequestParam(required = false) Integer games2,
                         @RequestParam(required = false) Integer winnerSide,
                         @RequestParam(defaultValue = "false") boolean correction,
                         @RequestParam(required = false) String returnTo, RedirectAttributes ra) {
        ResultInput input = new ResultInput(mode, gameScores, games1, games2, winnerSide);
        Match m = correction ? matchService.correct(matchId, input) : matchService.record(matchId, input);
        ra.addFlashAttribute("success", m.getRoundLabel() + " 결과 저장: " + m.getWinner().getDisplayName() + " 승 (" + m.getScoreLabel() + ")");
        return back(returnTo, m);
    }

    @PostMapping("/matches/{matchId}/reset")
    public String reset(@PathVariable Long matchId, @RequestParam(required = false) String returnTo, RedirectAttributes ra) {
        matchService.reset(matchId);
        ra.addFlashAttribute("success", "경기 결과를 취소했습니다.");
        return back(returnTo, load(matchId));
    }

    // ───────────────────────────────────────────────────────

    private java.util.List<Integer> freeTables(Long tournamentId) {
        TournamentBoard board = boardService.load(tournamentId);
        return board.tables().stream().filter(TournamentBoard.Table::isFree).map(TournamentBoard.Table::number).toList();
    }

    private Match load(Long matchId) {
        return matchRepository.findWithDetailsById(matchId).orElseThrow(() -> NotFoundException.of("경기", matchId));
    }

    private Long tournamentOf(Long entryId) {
        return entryService.find(entryId).getTournament().getId();
    }

    private static String console(Long tournamentId, String tab) {
        return "redirect:/manage/tournaments/" + tournamentId + "?tab=" + tab;
    }

    /** 내부 경로만 허용 */
    private static String back(String returnTo, Match m) {
        if (returnTo != null && returnTo.matches("^/(manage|tournament)/[\\w/?=&-]*$")) {
            return "redirect:" + returnTo;
        }
        return console(m.getTournament().getId(), "matches");
    }
}
