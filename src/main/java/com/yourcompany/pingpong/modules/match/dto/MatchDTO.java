package com.yourcompany.pingpong.modules.match.dto;

import com.yourcompany.pingpong.domain.Match;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경기 정보를 담는 DTO
 * 대진표 렌더링에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {

    private Long id;                // 경기 ID
    private Long tournamentId;      // 대회 ID
    private Long matchGroupId;      // 조 ID (예선의 경우)
    private String roundName;       // 라운드 이름 (16강, 8강, 4강, 준결승, 결승)
    private Integer matchNumber;    // 경기 번호
    private String status;          // 경기 상태

    private String player1Name;     // 선수1 이름
    private String player2Name;     // 선수2 이름
    private Long player1Id;         // 선수1 ID
    private Long player2Id;         // 선수2 ID

    private Integer score1;         // 선수1 점수
    private Integer score2;         // 선수2 점수
    private String winner;          // 승자

    private Integer tableNumber;    // 탁구대 번호

    /**
     * Match 엔티티에서 DTO로 변환
     */
    public static MatchDTO fromEntity(Match match) {
        return MatchDTO.builder()
                .id(match.getId())
                .tournamentId(match.getTournament() != null ? match.getTournament().getId() : null)
                .matchGroupId(match.getMatchGroup() != null ? match.getMatchGroup().getId() : null)
                .roundName(match.getRoundName())
                .matchNumber(match.getMatchNumber())
                .status(match.getStatus().name())
                .player1Name(match.getPlayer1() != null ? match.getPlayer1().getName() : "BYE")
                .player2Name(match.getPlayer2() != null ? match.getPlayer2().getName() : "BYE")
                .player1Id(match.getPlayer1() != null ? match.getPlayer1().getId() : null)
                .player2Id(match.getPlayer2() != null ? match.getPlayer2().getId() : null)
                .score1(match.getScore1())
                .score2(match.getScore2())
                .winner(match.getWinner())
                .tableNumber(match.getTableNumber())
                .build();
    }
}