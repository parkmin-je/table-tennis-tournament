package com.yourcompany.pingpong.modules.match.controller;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.MatchStatus;
import com.yourcompany.pingpong.modules.match.repository.MatchRepository;
import com.yourcompany.pingpong.modules.match.service.BracketService;
import com.yourcompany.pingpong.modules.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/match")
public class MatchController {

    private final BracketService bracketService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;


    // 본선 대진표 페이지를 보여준다.
    @GetMapping("/mainBracket/{tournamentId}")
    public String mainBracket(@PathVariable("tournamentId") Long tournamentId, Model model) {
        log.info("CONTROLLER INFO: Request for main bracket page for Tournament ID: {}", tournamentId);
        model.addAttribute("tournamentId", tournamentId);
        return "match/main_bracket";
    }

    // 본선 대진표 데이터를 JSON 형태로 반환한다.
    @GetMapping("/mainBracketData/{tournamentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mainBracketData(@PathVariable("tournamentId") Long tournamentId) {
        log.info("CONTROLLER INFO: Request for main bracket JSON data for Tournament ID: {}", tournamentId);
        try {
            Map<String, Object> bracketData = bracketService.getBracketData(tournamentId);
            return ResponseEntity.ok(bracketData);
        } catch (IllegalArgumentException e) {
            log.error("CONTROLLER ERROR: Error fetching bracket data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Unexpected error fetching bracket data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "서버 오류가 발생했습니다."));
        }
    }

    // ⭐⭐⭐ 새로운 API 추가: 진행 중인 경기 목록을 JSON으로 반환! ⭐⭐⭐
    @GetMapping("/inProgress/{tournamentId}")
    @ResponseBody
    public ResponseEntity<List<Match>> getInProgressMatches(@PathVariable("tournamentId") Long tournamentId) {
        log.info("CONTROLLER INFO: Request for in-progress matches for Tournament ID: {}", tournamentId);
        try {
            List<Match> inProgressMatches = matchRepository.findByTournamentIdAndStatusOrderByMatchTimeAsc(tournamentId, MatchStatus.IN_PROGRESS);
            // JPA 지연로딩 프록시 문제 방지 위해 불필요한 필드를 DTO로 변환하거나 @JsonIgnore 사용 필요할 수도 있지만,
            // 일단 그대로 보내보고 필요시 수정.
            return ResponseEntity.ok(inProgressMatches);
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Error fetching in-progress matches for Tournament ID {}: {}", tournamentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
    
    // ⭐⭐ 새로운 API: 경기 시작 (탁구대 배정 및 상태 변경) ⭐⭐
    @PostMapping("/start/{matchId}")
    @ResponseBody
    public ResponseEntity<?> startMatch(@PathVariable("matchId") Long matchId,
                                       @RequestParam("tableNumber") Integer tableNumber) {
        log.info("CONTROLLER INFO: Request to start match ID {} on table number {}.", matchId, tableNumber);
        try {
            Match match = matchService.startMatch(matchId, tableNumber);
            return ResponseEntity.ok(Map.of("message", "경기가 시작되었습니다.", "match", match));
        } catch (IllegalArgumentException e) {
            log.warn("CONTROLLER WARN: Failed to start match: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
             log.warn("CONTROLLER WARN: Failed to start match due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Error starting match ID {}: {}", matchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "경기 시작 중 서버 오류 발생."));
        }
    }

    // ⭐⭐ 새로운 API: 경기 완료 (점수 입력 및 상태 변경, 다음 라운드 생성 시도) ⭐⭐
    @PostMapping("/complete/{matchId}")
    @ResponseBody
    public ResponseEntity<?> completeMatch(@PathVariable("matchId") Long matchId,
                                           @RequestParam("score1") Integer score1,
                                           @RequestParam("score2") Integer score2) {
        log.info("CONTROLLER INFO: Request to complete match ID {} with scores {}:{}.", matchId, score1, score2);
        try {
            Match match = matchService.completeMatch(matchId, score1, score2);
            return ResponseEntity.ok(Map.of("message", "경기가 완료되었습니다.", "match", match));
        } catch (IllegalArgumentException e) {
            log.warn("CONTROLLER WARN: Failed to complete match: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("CONTROLLER WARN: Failed to complete match due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("CONTROLLER ERROR: Error completing match ID {}: {}", matchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "경기 완료 중 서버 오류 발생."));
        }
    }
}