package com.yourcompany.pingpong.common.service;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.modules.match.dto.MatchUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 대회의 모든 구독자에게 메시지 전송
     */
    public void sendToTournament(Long tournamentId, MatchUpdateMessage message) {
        String destination = "/topic/tournament/" + tournamentId;
        log.info("WebSocket 메시지 전송 → {}: {}", destination, message.getType());
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * 경기 시작 알림
     */
    public void notifyMatchStarted(Match match) {
        MatchUpdateMessage message = MatchUpdateMessage.builder()
                .type(MatchUpdateMessage.MessageType.MATCH_STARTED)
                .matchId(match.getId())
                .tournamentId(match.getTournament().getId())
                .roundName(match.getRoundName())
                .matchNumber(match.getMatchNumber())
                .status(match.getStatus())
                .tableNumber(match.getTableNumber())
                .player1Name(match.getPlayer1() != null ? match.getPlayer1().getName() : "BYE")
                .player2Name(match.getPlayer2() != null ? match.getPlayer2().getName() : "BYE")
                .timestamp(LocalDateTime.now())
                .message(String.format("🔴 %s %d경기가 %d번 탁구대에서 시작되었습니다!", 
                        match.getRoundName(), match.getMatchNumber(), match.getTableNumber()))
                .build();
        
        sendToTournament(match.getTournament().getId(), message);
    }

    /**
     * 경기 완료 알림
     */
    public void notifyMatchCompleted(Match match) {
        MatchUpdateMessage message = MatchUpdateMessage.builder()
                .type(MatchUpdateMessage.MessageType.MATCH_COMPLETED)
                .matchId(match.getId())
                .tournamentId(match.getTournament().getId())
                .roundName(match.getRoundName())
                .matchNumber(match.getMatchNumber())
                .status(match.getStatus())
                .player1Name(match.getPlayer1() != null ? match.getPlayer1().getName() : "BYE")
                .player2Name(match.getPlayer2() != null ? match.getPlayer2().getName() : "BYE")
                .score1(match.getScore1())
                .score2(match.getScore2())
                .winner(match.getWinner())
                .timestamp(LocalDateTime.now())
                .message(String.format("✅ %s %d경기 종료! 승자: %s (%d:%d)", 
                        match.getRoundName(), match.getMatchNumber(), 
                        match.getWinner(), match.getScore1(), match.getScore2()))
                .build();
        
        sendToTournament(match.getTournament().getId(), message);
    }

    /**
     * 대진표 업데이트 알림 (다음 라운드 생성 시)
     */
    public void notifyBracketUpdated(Long tournamentId, String roundName) {
        MatchUpdateMessage message = MatchUpdateMessage.builder()
                .type(MatchUpdateMessage.MessageType.BRACKET_UPDATED)
                .tournamentId(tournamentId)
                .roundName(roundName)
                .timestamp(LocalDateTime.now())
                .message(String.format("🎯 %s 대진표가 업데이트되었습니다!", roundName))
                .build();
        
        sendToTournament(tournamentId, message);
    }

    /**
     * 탁구대 상태 업데이트
     */
    public void notifyTableUpdated(Long tournamentId) {
        MatchUpdateMessage message = MatchUpdateMessage.builder()
                .type(MatchUpdateMessage.MessageType.TABLE_UPDATED)
                .tournamentId(tournamentId)
                .timestamp(LocalDateTime.now())
                .message("🏓 탁구대 상태가 업데이트되었습니다.")
                .build();
        
        sendToTournament(tournamentId, message);
    }
}