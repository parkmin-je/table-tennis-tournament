package com.yourcompany.pingpong.modules.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchGroupBracketDTO {
    private Long id;
    private Long tournamentId;
    private String tournamentTitle;
    private String name; // 조 이름 (예: A조, B조)
}