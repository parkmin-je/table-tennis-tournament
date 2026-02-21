package com.yourcompany.pingpong.modules.calendar.controller;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.modules.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final TournamentService tournamentService;

    @GetMapping
    public String calendar() {
        return "calendar/calendar";
    }

    @GetMapping("/events")
    @ResponseBody
    public List<Map<String, Object>> getEvents() {
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return tournaments.stream().map(t -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", t.getId());
            event.put("title", t.getTitle());
            event.put("start", t.getStartDate().format(formatter));
            event.put("end", t.getEndDate().format(formatter));
            event.put("url", "/tournament/detail/" + t.getId());

            // 상태별 색상
            switch (t.getStatus()) {
                case RECRUITING -> event.put("backgroundColor", "#28a745");
                case IN_PROGRESS -> event.put("backgroundColor", "#ffc107");
                case COMPLETED -> event.put("backgroundColor", "#6c757d");
                default -> event.put("backgroundColor", "#007bff");
            }

            return event;
        }).collect(Collectors.toList());
    }
}