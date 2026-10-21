package com.parkminje.pingpong.admin;

import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.entry.EntryRepository;
import com.parkminje.pingpong.player.PlayerRepository;
import com.parkminje.pingpong.security.SessionExpirer;
import com.parkminje.pingpong.tournament.TournamentRepository;
import com.parkminje.pingpong.tournament.TournamentStatus;
import com.parkminje.pingpong.user.Role;
import com.parkminje.pingpong.user.User;
import com.parkminje.pingpong.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final SessionExpirer sessionExpirer;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final EntryRepository entryRepository;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.count());
        model.addAttribute("playerCount", playerRepository.count());
        model.addAttribute("recruiting", tournamentRepository.countByStatus(TournamentStatus.RECRUITING));
        model.addAttribute("inProgress", tournamentRepository.countByStatus(TournamentStatus.IN_PROGRESS));
        model.addAttribute("completed", tournamentRepository.countByStatus(TournamentStatus.COMPLETED));
        model.addAttribute("tournaments", tournamentRepository.findAllByOrderByStartAtDesc().stream().limit(8).toList());
        model.addAttribute("recentEntries", entryRepository.findTop10ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", userService.search(q, page));
        model.addAttribute("q", q);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String role(@PathVariable Long id, @RequestParam Role role, @LoginUser User actor, RedirectAttributes ra) {
        userService.changeRole(actor.getId(), id, role);
        sessionExpirer.expireUser(id);
        ra.addFlashAttribute("success", "권한을 '" + role.getLabel() + "'(으)로 변경했습니다. 해당 회원은 다시 로그인해야 합니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/enabled")
    public String enabled(@PathVariable Long id, @RequestParam boolean value, @LoginUser User actor, RedirectAttributes ra) {
        userService.setEnabled(actor.getId(), id, value);
        if (!value) {
            sessionExpirer.expireUser(id);
        }
        ra.addFlashAttribute("success", value ? "계정을 활성화했습니다." : "계정을 비활성화했습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        String temp = userService.resetPassword(id);
        sessionExpirer.expireUser(id);
        ra.addFlashAttribute("success", "임시 비밀번호: " + temp + " (회원에게 전달 후 로그인하여 변경하도록 안내하세요)");
        return "redirect:/admin/users";
    }
}
