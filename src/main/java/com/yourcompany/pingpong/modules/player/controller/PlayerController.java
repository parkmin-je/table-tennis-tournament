package com.yourcompany.pingpong.modules.player.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yourcompany.pingpong.domain.Club;
import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.modules.club.service.ClubService;
import com.yourcompany.pingpong.modules.player.dto.PlayerStatsDto;
import com.yourcompany.pingpong.modules.player.service.PlayerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;
    private final ClubService clubService;

    /**
     * ✅ 선수 목록
     */
    @GetMapping("/list")
    public String list(Model model) {
        log.info("CONTROLLER INFO: Accessing player list page");
        model.addAttribute("players", playerService.findAllWithClub());
        model.addAttribute("clubs", clubService.getAllClubs());
        return "player/list";
    }

    /**
     * ✅ 선수 등록 폼
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        log.info("CONTROLLER INFO: Accessing player create form");
        model.addAttribute("player", new Player());
        model.addAttribute("clubs", clubService.getAllClubs());
        return "player/create";
    }

    /**
     * ✅ 등록 처리
     */
    @PostMapping("/create")
    public String savePlayer(@ModelAttribute Player player,
                             @RequestParam(value = "clubId", required = false) Long clubId,
                             RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Creating new player: {}", player.getName());

        try {
            if (clubId != null) {
                Club club = clubService.getClub(clubId);
                player.setClub(club);
            }
            Player savedPlayer = playerService.save(player);
            redirectAttributes.addFlashAttribute("message", "선수가 성공적으로 등록되었습니다.");
            return "redirect:/player/detail/" + savedPlayer.getId();
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to create player", e);
            redirectAttributes.addFlashAttribute("error", "선수 등록에 실패했습니다.");
            return "redirect:/player/create";
        }
    }

    /**
     * ⭐ 선수 상세보기 - /player/detail/{id} 경로 추가
     */
    @GetMapping("/detail/{id}")
    public String detailById(@PathVariable Long id, Model model) {
        log.info("CONTROLLER INFO: Accessing player detail page for ID: {}", id);
        model.addAttribute("player", playerService.getPlayerWithClub(id));
        return "player/detail";
    }

    /**
     * ✅ 선수 상세보기 - 기존 /{id} 경로 유지
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        log.info("CONTROLLER INFO: Accessing player detail page for ID: {}", id);
        model.addAttribute("player", playerService.getPlayerWithClub(id));
        return "player/detail";
    }

    /**
     * ✅ 선수 수정 폼
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        log.info("CONTROLLER INFO: Accessing player edit form for ID: {}", id);
        model.addAttribute("player", playerService.getPlayerWithClub(id));
        model.addAttribute("clubs", clubService.getAllClubs());
        return "player/edit";
    }

    /**
     * ✅ 선수 수정 처리
     */
    @PostMapping("/edit/{id}")
    public String updatePlayer(@PathVariable Long id,
                               @ModelAttribute Player updatedPlayer,
                               @RequestParam(value = "clubId", required = false) Long clubId,
                               RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Updating player with ID: {}", id);

        try {
            if (clubId != null) {
                Club club = clubService.getClub(clubId);
                updatedPlayer.setClub(club);
            }
            playerService.update(id, updatedPlayer);
            redirectAttributes.addFlashAttribute("message", "선수 정보가 성공적으로 수정되었습니다.");
            return "redirect:/player/detail/" + id;
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to update player", e);
            redirectAttributes.addFlashAttribute("error", "선수 수정에 실패했습니다.");
            return "redirect:/player/edit/" + id;
        }
    }

    /**
     * ✅ 선수 삭제
     */
    @PostMapping("/delete/{id}")
    public String deletePlayer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("CONTROLLER INFO: Deleting player with ID: {}", id);

        try {
            playerService.delete(id);
            redirectAttributes.addFlashAttribute("message", "선수가 성공적으로 삭제되었습니다.");
            return "redirect:/player/list";
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Failed to delete player", e);
            redirectAttributes.addFlashAttribute("error", "선수 삭제에 실패했습니다.");
            return "redirect:/player/detail/" + id;
        }
    }

    /**
     * ⭐ 선수 전적 통계 API (JSON)
     */
    @GetMapping("/stats/{id}")
    @ResponseBody
    public ResponseEntity<PlayerStatsDto> getStats(@PathVariable Long id) {
        log.info("CONTROLLER INFO: Fetching player stats for ID: {}", id);
        PlayerStatsDto stats = playerService.getPlayerStats(id);
        return ResponseEntity.ok(stats);
    }

    /**
     * ⭐ 선수 랭킹 페이지
     */
    @GetMapping("/ranking")
    public String ranking(Model model) {
        log.info("CONTROLLER INFO: Accessing player ranking page");

        List<Player> rankedPlayers = playerService.findAllWithClub().stream()
                .filter(p -> p.getRanking() != null)
                .sorted(Comparator.comparing(Player::getRanking))
                .limit(100)
                .collect(Collectors.toList());

        model.addAttribute("players", rankedPlayers);
        return "player/ranking";
    }
}