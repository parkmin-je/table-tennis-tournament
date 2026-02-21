package com.yourcompany.pingpong.common.controller;

import com.yourcompany.pingpong.domain.Player;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.modules.player.repository.PlayerRepository;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 전역 검색 API
 * GET /api/search?q={keyword}
 * → { tournaments: [...], players: [...] }
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;

    public SearchController(TournamentRepository tournamentRepository,
                            PlayerRepository playerRepository) {
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(name = "q", defaultValue = "") String q) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (q == null || q.isBlank()) {
            result.put("tournaments", Collections.emptyList());
            result.put("players", Collections.emptyList());
            return ResponseEntity.ok(result);
        }

        String keyword = q.trim();

        /* ── 대회 검색 ── */
        List<Map<String, Object>> tournaments = new ArrayList<>();
        tournamentRepository.findByTitleContaining(keyword)
                .stream().limit(10)
                .forEach(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",          t.getId());
                    m.put("title",       t.getTitle());
                    m.put("statusLabel", t.getStatus().getDescription());
                    m.put("typeLabel",   t.getType().getDescription());
                    tournaments.add(m);
                });

        /* ── 선수 검색 ── */
        List<Map<String, Object>> players = new ArrayList<>();
        playerRepository.findByNameContainingWithClub(keyword)
                .stream().limit(10)
                .forEach(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",       p.getId());
                    m.put("name",     p.getName());
                    m.put("ranking",  p.getRanking() != null ? p.getRanking() : 0);
                    m.put("clubName", p.getClub() != null ? p.getClub().getName() : null);
                    players.add(m);
                });

        result.put("tournaments", tournaments);
        result.put("players",    players);
        return ResponseEntity.ok(result);
    }
}
