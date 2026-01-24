package com.yourcompany.pingpong.common.controller;

import com.yourcompany.pingpong.modules.match.service.MatchService;
import com.yourcompany.pingpong.modules.player.service.PlayerService;
import com.yourcompany.pingpong.modules.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private final PlayerService playerService;

    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("tournaments", tournamentService.getOngoingTournaments());
            model.addAttribute("matches", matchService.getTodayMatches());
            model.addAttribute("players", playerService.findAllWithClub());
            log.info("Index page loaded successfully");
            return "index";
        } catch (Exception e) {
            log.error("Error loading index page", e);
            model.addAttribute("error", "메인 페이지를 불러오는 중 오류가 발생했습니다.");
            return "error/500";
        }
    }
}