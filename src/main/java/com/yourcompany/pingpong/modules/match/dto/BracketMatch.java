package com.yourcompany.pingpong.modules.match.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BracketMatch {
    private String team1;
    private String team2;
    private Integer score1;
    private Integer score2;
    private String winner;
    private String roundName;
}
