package com.parkminje.pingpong.home;

import com.parkminje.pingpong.player.PlayerRepository;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentRepository;
import com.parkminje.pingpong.tournament.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("home", homeService.load());
        return "index";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "calendar";
    }

    @GetMapping("/api/calendar")
    @ResponseBody
    public List<Map<String, Object>> events(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (Tournament t : tournamentRepository.findByStatusNotAndStartAtBetweenOrderByStartAtAsc(TournamentStatus.DRAFT, start, end)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", t.getId());
            e.put("title", t.getTitle());
            e.put("start", t.getStartAt().toString());
            e.put("end", t.getEndAt().toString());
            e.put("url", "/tournament/" + t.getId());
            e.put("classNames", List.of("ev-" + t.getStatus().getTone()));
            e.put("extendedProps", Map.of("status", t.getStatus().getLabel(), "venue", t.getVenue() == null ? "" : t.getVenue()));
            events.add(e);
        }
        return events;
    }

    @GetMapping("/api/search")
    @ResponseBody
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q) {
        String keyword = q.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        if (keyword.isEmpty() || keyword.length() > 50) {
            result.put("tournaments", List.of());
            result.put("players", List.of());
            return result;
        }
        result.put("tournaments", tournamentRepository.findTop10ByTitleContainingAndStatusNotOrderByStartAtDesc(keyword, TournamentStatus.DRAFT)
                .stream().map(t -> Map.of("id", t.getId(), "title", t.getTitle(), "status", t.getStatus().getLabel(),
                        "date", t.getStartAt().toLocalDate().toString())).toList());
        result.put("players", playerRepository.findTop10ByNameContainingOrderByRatingDesc(keyword).stream()
                .map(p -> Map.of("id", p.getId(), "name", p.getName(), "club", p.getClubName(),
                        "division", p.getDivisionLabel(), "rating", p.getRating())).toList());
        return result;
    }
}
