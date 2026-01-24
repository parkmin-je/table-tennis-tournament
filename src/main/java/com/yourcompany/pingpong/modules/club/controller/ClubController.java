package com.yourcompany.pingpong.modules.club.controller;

import com.yourcompany.pingpong.domain.Club;
import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.modules.club.service.ClubService;
import com.yourcompany.pingpong.modules.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/club")
public class ClubController {

    private final ClubService clubService;
    private final PlayerService playerService;

    /**
     * ✅ 클럽 목록 (선수 포함)
     */
    @GetMapping("/list")
    public String getAllClubsWithPlayers(Model model) {
        log.info("CONTROLLER INFO: Accessing club list page");
        model.addAttribute("clubs", clubService.getAllClubsWithPlayers());
        return "club/list";
    }

    /**
     * ⭐ 클럽 상세 페이지 (소속 선수 목록 포함)
     */
    @GetMapping("/detail/{id}")
    public String clubDetail(@PathVariable Long id, Model model) {
        log.info("CONTROLLER INFO: Accessing club detail page for ID: {}", id);

        Club club = clubService.getClub(id);
        List<Player> players = playerService.findByClubId(id);

        model.addAttribute("club", club);
        model.addAttribute("players", players);

        return "club/detail";
    }

    /**
     * ✅ 클럽 생성 폼 페이지
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        log.info("CONTROLLER INFO: Accessing club create form");
        model.addAttribute("club", new Club());
        return "club/create";
    }

    /**
     * ✅ 클럽 생성 처리
     */
    @PostMapping("/create")
    public String saveClub(@ModelAttribute Club club, RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Creating new club: {}", club.getName());

        try {
            Club savedClub = clubService.saveClub(club);
            redirectAttributes.addFlashAttribute("message", "클럽이 성공적으로 등록되었습니다.");
            return "redirect:/club/detail/" + savedClub.getId();
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to create club", e);
            redirectAttributes.addFlashAttribute("error", "클럽 등록에 실패했습니다.");
            return "redirect:/club/create";
        }
    }

    /**
     * ⭐ 클럽 수정 폼 페이지
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        log.info("CONTROLLER INFO: Accessing club edit form for ID: {}", id);

        Club club = clubService.getClub(id);
        model.addAttribute("club", club);

        return "club/edit";
    }

    /**
     * ⭐ 클럽 수정 처리
     */
    @PostMapping("/edit/{id}")
    public String updateClub(@PathVariable Long id,
                             @ModelAttribute Club club,
                             RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Updating club with ID: {}", id);

        try {
            club.setId(id);
            clubService.updateClub(club);
            redirectAttributes.addFlashAttribute("message", "클럽 정보가 성공적으로 수정되었습니다.");
            return "redirect:/club/detail/" + id;
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to update club", e);
            redirectAttributes.addFlashAttribute("error", "클럽 수정에 실패했습니다.");
            return "redirect:/club/edit/" + id;
        }
    }

    /**
     * ✅ 클럽 삭제
     */
    @PostMapping("/delete/{id}")
    public String deleteClub(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Deleting club with ID: {}", id);

        try {
            clubService.deleteClub(id);
            redirectAttributes.addFlashAttribute("message", "클럽이 성공적으로 삭제되었습니다.");
            return "redirect:/club/list";
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to delete club", e);
            redirectAttributes.addFlashAttribute("error", "클럽 삭제에 실패했습니다.");
            return "redirect:/club/list";
        }
    }
}