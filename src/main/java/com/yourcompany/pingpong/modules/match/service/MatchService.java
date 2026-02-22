package com.yourcompany.pingpong.modules.match.service;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.MatchStatus;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.modules.match.repository.MatchRepository;
import com.yourcompany.pingpong.common.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final BracketService bracketService;
    private final WebSocketService webSocketService;

    public Match startMatch(Long matchId, Integer tableNumber) {
        log.info("SERVICE INFO: Request to start match ID {} on table number {}.", matchId, tableNumber);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다. ID=" + matchId));

        if (match.getStatus() != MatchStatus.SCHEDULED) {
            log.warn("SERVICE WARN: Match ID {} is not in SCHEDULED status. Current status: {}. Cannot start.",
                    matchId, match.getStatus());
            throw new IllegalStateException("예정된 경기만 시작할 수 있습니다.");
        }

        Optional<Match> existingMatchOnTable = matchRepository.findByTableNumberAndStatus(tableNumber, MatchStatus.IN_PROGRESS);
        if (existingMatchOnTable.isPresent()) {
            throw new IllegalStateException("탁구대 " + tableNumber + "번은 이미 다른 경기가 진행 중입니다.");
        }

        match.setTableNumber(tableNumber);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setUpdatedAt(LocalDateTime.now());
        Match updatedMatch = matchRepository.save(match);

        webSocketService.notifyMatchStarted(updatedMatch);

        log.info("SERVICE INFO: Match ID {} started on table {}. Status changed to IN_PROGRESS.", matchId, tableNumber);
        return updatedMatch;
    }

    public Match completeMatch(Long matchId, Integer score1, Integer score2) {
        log.info("SERVICE INFO: Request to complete match ID {} with scores {}:{}.", matchId, score1, score2);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다. ID=" + matchId));

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            log.warn("SERVICE WARN: Match ID {} is not in IN_PROGRESS status. Current status: {}. Cannot complete.",
                    matchId, match.getStatus());
            throw new IllegalStateException("진행 중인 경기만 완료할 수 있습니다.");
        }

        if (score1 == null || score2 == null) {
            throw new IllegalArgumentException("점수를 모두 입력해야 합니다.");
        }

        if (score1.equals(score2)) {
            throw new IllegalArgumentException("무승부는 현재 지원되지 않습니다. 승패를 결정해 주세요.");
        }

        String winnerName;
        if (score1 > score2) {
            winnerName = match.getPlayer1() != null ? match.getPlayer1().getName() : "Unknown Winner P1";
        } else {
            winnerName = match.getPlayer2() != null ? match.getPlayer2().getName() : "Unknown Winner P2";
        }

        match.setScore1(score1);
        match.setScore2(score2);
        match.setWinner(winnerName);
        match.setStatus(MatchStatus.COMPLETED);
        match.setTableNumber(null);
        match.setUpdatedAt(LocalDateTime.now());

        Match completedMatch = matchRepository.save(match);

        webSocketService.notifyMatchCompleted(completedMatch);

        log.info("SERVICE INFO: Match ID {} completed. Winner: {}, Score: {}:{}. Status changed to COMPLETED.",
                matchId, winnerName, score1, score2);

        bracketService.autoAdvanceWinner(completedMatch);

        return completedMatch;
    }

    @Transactional(readOnly = true)
    public Match findMatchByTableNumberAndStatus(Integer tableNumber) {
        return matchRepository.findByTableNumberAndStatus(tableNumber, MatchStatus.IN_PROGRESS).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Match> getTodayMatches() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        log.info("SERVICE INFO: Fetching today's matches from {} to {}.", start, end);

        List<Match> todayMatches = matchRepository.findMatchesForToday(
                start, end, TournamentStatus.IN_PROGRESS
        );

        log.debug("SERVICE DEBUG: Found {} matches for today.", todayMatches.size());
        return todayMatches;
    }

    /**
     * 특정 대회의 모든 경기 조회
     */
    @Transactional(readOnly = true)
    public List<Match> getMatchesByTournamentId(Long tournamentId) {
        log.info("SERVICE INFO: Fetching matches for tournament ID: {}", tournamentId);
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);
        log.debug("SERVICE DEBUG: Found {} matches for tournament ID: {}", matches.size(), tournamentId);
        return matches;
    }

    /**
     * ⭐ 경기 결과 수정 (관리자 전용 - 완료된 경기 점수 수정)
     */
    public Match editMatch(Long matchId, Integer score1, Integer score2) {
        log.info("SERVICE INFO: Admin editing match ID {} → {}:{}", matchId, score1, score2);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다. ID=" + matchId));

        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new IllegalStateException("완료된 경기만 수정할 수 있습니다. 현재 상태: " + match.getStatus());
        }
        if (score1 == null || score2 == null) {
            throw new IllegalArgumentException("점수를 모두 입력해야 합니다.");
        }
        if (score1.equals(score2)) {
            throw new IllegalArgumentException("무승부는 허용되지 않습니다.");
        }

        String winnerName;
        if (score1 > score2) {
            winnerName = match.getPlayer1() != null ? match.getPlayer1().getName() : "Unknown";
        } else {
            winnerName = match.getPlayer2() != null ? match.getPlayer2().getName() : "Unknown";
        }

        match.setScore1(score1);
        match.setScore2(score2);
        match.setWinner(winnerName);
        match.setUpdatedAt(LocalDateTime.now());

        Match updated = matchRepository.save(match);
        webSocketService.notifyMatchCompleted(updated);
        log.info("SERVICE INFO: Match ID {} result updated. Winner: {}, Score: {}:{}", matchId, winnerName, score1, score2);
        return updated;
    }
}