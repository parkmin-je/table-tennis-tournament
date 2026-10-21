package com.parkminje.pingpong.tournament;

import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.entry.EntryService;
import com.parkminje.pingpong.live.TournamentBoard;
import com.parkminje.pingpong.live.TournamentBoardService;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.player.RatingHistoryRepository;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class TournamentController {

    private static final Set<String> FRAGMENTS = Set.of("stats", "tables", "queue", "recent", "groups", "bracket", "board", "progress");

    private final TournamentService tournamentService;
    private final TournamentBoardService boardService;
    private final EntryService entryService;
    private final PlayerService playerService;
    private final RatingHistoryRepository ratingHistoryRepository;
    private final com.parkminje.pingpong.player.PlayerRepository playerRepository;

    @GetMapping("/tournaments")
    public String list(@RequestParam(required = false) TournamentStatus status,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", tournamentService.searchPublic(status, q, page));
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("statuses", new TournamentStatus[]{TournamentStatus.RECRUITING, TournamentStatus.CLOSED,
                TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED});
        return "tournament/list";
    }

    @GetMapping("/tournament/{id}")
    public String detail(@PathVariable Long id, @LoginUser User user, Model model) {
        TournamentBoard board = publicBoard(id, user);
        model.addAttribute("board", board);
        model.addAttribute("t", board.tournament());
        Optional<Player> myPlayer = user == null ? Optional.empty() : playerService.findByUser(user.getId());
        model.addAttribute("myPlayer", myPlayer.orElse(null));
        Entry myEntry = myPlayer.flatMap(p -> board.entries().stream()
                .filter(e -> e.includes(p) && (e.getStatus().isActive() || e.isWithdrawn())).findFirst()).orElse(null);
        model.addAttribute("myEntry", myEntry);
        boolean open = board.tournament().isRegistrationOpen(LocalDateTime.now());
        model.addAttribute("registrationOpen", open);
        if (open && board.tournament().isDoubles() && myPlayer.isPresent()) {
            model.addAttribute("partners", playerRepository.findAllByOrderByNameAsc().stream()
                    .filter(p -> !p.getId().equals(myPlayer.get().getId())).toList());
        }
        if (board.tournament().isRatingApplied()) {
            model.addAttribute("ratingChanges", ratingHistoryRepository.findByTournamentId(id).stream()
                    .sorted((a, b) -> Integer.compare(b.getDelta(), a.getDelta())).toList());
        }
        return "tournament/detail";
    }

    @GetMapping("/tournament/{id}/bracket")
    public String bracket(@PathVariable Long id, @LoginUser User user, Model model) {
        TournamentBoard board = publicBoard(id, user);
        model.addAttribute("board", board);
        model.addAttribute("t", board.tournament());
        return "tournament/bracket";
    }

    /** 체육관 대형 화면용 현황판 */
    @GetMapping("/tournament/{id}/board")
    public String tvBoard(@PathVariable Long id, @LoginUser User user, Model model) {
        TournamentBoard board = publicBoard(id, user);
        model.addAttribute("board", board);
        model.addAttribute("t", board.tournament());
        return "tournament/board";
    }

    /** 실시간 갱신용 서버 렌더링 조각 */
    @GetMapping("/tournament/{id}/fragment/{name}")
    public String fragment(@PathVariable Long id, @PathVariable String name, @LoginUser User user, Model model) {
        if (!FRAGMENTS.contains(name)) {
            throw new NotFoundException("없는 화면 조각입니다.");
        }
        TournamentBoard board = publicBoard(id, user);
        model.addAttribute("board", board);
        model.addAttribute("t", board.tournament());
        return "tournament/fragments :: " + name;
    }

    @PostMapping("/tournament/{id}/apply")
    public String apply(@PathVariable Long id, @LoginUser User user,
                        @RequestParam(required = false) Long partnerId, RedirectAttributes ra) {
        entryService.apply(id, user, partnerId);
        ra.addFlashAttribute("success", "참가 신청이 접수되었습니다.");
        return "redirect:/tournament/" + id;
    }

    @PostMapping("/entries/{entryId}/cancel")
    public String cancel(@PathVariable Long entryId, @LoginUser User user, RedirectAttributes ra) {
        Entry entry = entryService.find(entryId);
        Long tournamentId = entry.getTournament().getId();
        entryService.cancelByUser(entryId, user);
        ra.addFlashAttribute("success", "참가 신청을 취소했습니다.");
        return "redirect:/tournament/" + tournamentId;
    }

    /** 준비중(비공개) 대회는 운영진만 볼 수 있다 */
    private TournamentBoard publicBoard(Long id, User user) {
        TournamentBoard board = boardService.load(id);
        if (board.tournament().getStatus() == TournamentStatus.DRAFT && (user == null || !user.isStaff())) {
            throw NotFoundException.of("대회", id);
        }
        return board;
    }
}
