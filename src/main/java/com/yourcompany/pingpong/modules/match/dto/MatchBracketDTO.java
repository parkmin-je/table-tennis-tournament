package com.yourcompany.pingpong.modules.match.dto;

import com.yourcompany.pingpong.modules.user.dto.PlayerSimpleDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchBracketDTO {
    private Long id;
    private Long tournamentId;
    private Long matchGroupId;
    private Integer matchNumber;
    private String status;
    private String roundName;
    private PlayerSimpleDTO player1; // PlayerSimpleDTO 사용
    private PlayerSimpleDTO player2; // PlayerSimpleDTO 사용
    private Integer score1;
    private Integer score2;
    private String winner;
}