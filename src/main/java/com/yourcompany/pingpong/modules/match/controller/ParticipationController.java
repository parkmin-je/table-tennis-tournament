package com.yourcompany.pingpong.modules.match.controller;

import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.match.service.ParticipationService;
import com.yourcompany.pingpong.modules.tournament.service.TournamentService;
import com.yourcompany.pingpong.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/participation")
public class ParticipationController {

    private final ParticipationService participationService;
    private final TournamentService tournamentService;
    private final UserService userService;

    /* -----------------------------------------
        참가 신청
    ----------------------------------------- */
    @PostMapping("/apply/{tournamentId}")
    public String apply(@PathVariable("tournamentId") Long tournamentId,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("message", "로그인이 필요합니다.");
            redirectAttributes.addFlashAttribute("alertType", "warning");
            return "redirect:/user/login";
        }

        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername());
            participationService.applyParticipation(currentUser, tournamentService.getTournamentById(tournamentId));

            redirectAttributes.addFlashAttribute("message", "대회 참가 신청이 완료되었습니다!");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        } catch (Exception e) {
            log.error("PARTICIPATION ERROR: Failed to apply", e);
            redirectAttributes.addFlashAttribute("message", "참가 신청 중 알 수 없는 오류가 발생했습니다: " + e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }

        return "redirect:/tournament/detail/" + tournamentId;
    }

    /* -----------------------------------------
        ⭐ 참가 취소 (NEW)
    ----------------------------------------- */
    @PostMapping("/cancel/{participationId}")
    public String cancelParticipation(
            @PathVariable("participationId") Long participationId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("CANCEL: Request to cancel participation ID {}", participationId);

        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("message", "로그인이 필요합니다.");
            redirectAttributes.addFlashAttribute("alertType", "warning");
            return "redirect:/user/login";
        }

        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername());
            participationService.cancelParticipation(participationId, currentUser);

            redirectAttributes.addFlashAttribute("message", "참가 신청이 취소되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            log.info("CANCEL SUCCESS: Participation {} cancelled by user {}", participationId, currentUser.getUsername());
        } catch (IllegalStateException e) {
            log.warn("CANCEL FAILED: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        } catch (Exception e) {
            log.error("CANCEL ERROR: Unexpected error", e);
            redirectAttributes.addFlashAttribute("message", "참가 취소 중 오류가 발생했습니다: " + e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }

        return "redirect:/user/profile";
    }
}