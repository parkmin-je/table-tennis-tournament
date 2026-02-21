package com.yourcompany.pingpong.modules.match.dto;

import com.yourcompany.pingpong.domain.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchUpdateMessage {
    
    private MessageType type; // 메시지 유형
    private Long matchId;
    private Long tournamentId;
    private String roundName;
    private Integer matchNumber;
    private MatchStatus status;
    private Integer tableNumber;
    
    private String player1Name;
    private String player2Name;
    private Integer score1;
    private Integer score2;
    private String winner;
    
    private LocalDateTime timestamp;
    private String message; // 사용자에게 보여줄 메시지
    
    public enum MessageType {
        MATCH_STARTED,      // 경기 시작
        MATCH_COMPLETED,    // 경기 완료
        MATCH_SCHEDULED,    // 새 경기 생성
        TABLE_UPDATED,      // 탁구대 상태 변경
        BRACKET_UPDATED     // 대진표 업데이트
    }
}