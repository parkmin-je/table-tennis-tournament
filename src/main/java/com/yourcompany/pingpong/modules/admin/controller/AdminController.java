package com.yourcompany.pingpong.modules.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yourcompany.pingpong.domain.Participation;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.modules.match.repository.ParticipationRepository;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;
import com.yourcompany.pingpong.modules.match.service.ParticipationService;
import com.yourcompany.pingpong.modules.player.service.PlayerService;
import com.yourcompany.pingpong.modules.tournament.service.TournamentService;
import com.yourcompany.pingpong.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TournamentService tournamentService;
    private final TournamentRepository tournamentRepository;
    private final ParticipationService participationService;
    private final UserService userService;
    private final PlayerService playerService;
    private final ParticipationRepository participationRepository;

    // 관리자 대시보드
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // ⭐ 전체 통계
        long totalTournaments = tournamentService.getAllTournaments().size();
        long totalUsers = userService.countAllUsers();
        long totalPlayers = playerService.countAllPlayers();

        // ⭐ 최근 대회
        List<Tournament> recentTournaments = tournamentService.getAllTournaments().stream()
                .sorted(Comparator.comparing(Tournament::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // ⭐⭐⭐ 최근 참가 신청 (User와 Tournament를 FETCH JOIN으로 즉시 로딩) ⭐⭐⭐
        List<Participation> recentParticipations = participationRepository
                .findTop10ByOrderByRegisteredAtDesc();

        model.addAttribute("totalTournaments", totalTournaments);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPlayers", totalPlayers);
        model.addAttribute("ongoingTournaments", tournamentService.getOngoingTournaments().size());
        model.addAttribute("recentTournaments", recentTournaments);
        model.addAttribute("recentParticipations", recentParticipations);

        return "admin/dashboard";
    }

    // ⭐⭐⭐ 참가 신청 관리 목록 페이지 ⭐⭐⭐
    @GetMapping("/participationList")
    public String participationList(Model model) {
        model.addAttribute("participations", participationService.findAll());
        model.addAttribute("stats", participationService.getStatusStats());
        return "admin/participationList";
    }

    // 참가 신청 상태 업데이트 (POST 요청)
    @PostMapping("/updateStatus")
    public String updateParticipationStatus(@RequestParam("participationId") Long participationId,
                                            @RequestParam("newStatus") String newStatus,
                                            RedirectAttributes redirectAttributes) {
        participationService.updateStatus(participationId, newStatus);
        redirectAttributes.addFlashAttribute("message", participationId + "번 참가신청 상태가 " + newStatus + "로 변경되었습니다.");
        redirectAttributes.addFlashAttribute("alertType", "success");
        return "redirect:/admin/participationList";
    }

    // ⭐⭐⭐ 관리자용 대회 목록 ⭐⭐⭐
    @GetMapping("/tournaments")
    public String adminTournaments(Model model) {
        model.addAttribute("tournaments", tournamentService.getAllTournamentsForAdmin());
        return "admin/tournaments";
    }

    // 관리자용 대회 삭제
    @PostMapping("/tournament/delete/{id}")
    public String deleteTournamentAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        tournamentService.deleteTournament(id);
        redirectAttributes.addFlashAttribute("message", id + "번 대회가 삭제되었습니다.");
        redirectAttributes.addFlashAttribute("alertType", "success");
        return "redirect:/admin/tournaments";
    }

    // ⭐⭐⭐ NEW: 대회 상태 변경 (드롭다운에서 바로 변경) ⭐⭐⭐
    @PostMapping("/tournament/updateStatus/{id}")
    public String updateTournamentStatus(@PathVariable("id") Long id,
                                         @RequestParam("status") TournamentStatus status,
                                         RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating tournament status - ID: {}, New Status: {}", id, status);

            Tournament tournament = tournamentService.getTournamentById(id);
            TournamentStatus oldStatus = tournament.getStatus();

            tournament.setStatus(status);
            tournament.setUpdatedAt(LocalDateTime.now());
            tournamentRepository.save(tournament);

            log.info("Tournament status updated successfully - ID: {}, {} → {}",
                    id, oldStatus.getDescription(), status.getDescription());

            redirectAttributes.addFlashAttribute("message",
                    "대회 상태가 '" + status.getDescription() + "'로 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

        } catch (Exception e) {
            log.error("Error updating tournament status - ID: {}", id, e);
            redirectAttributes.addFlashAttribute("message",
                    "상태 변경 중 오류가 발생했습니다: " + e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }

        return "redirect:/admin/tournaments";
    }
}