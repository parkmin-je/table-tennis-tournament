package com.yourcompany.pingpong.modules.match.dto;

import com.yourcompany.pingpong.modules.user.dto.PlayerSimpleDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipationBracketDTO {
    private Long id;
    private Long tournamentId;
    private String teamName;
    private String status;
    private Long matchGroupId;
    private PlayerSimpleDTO player; // PlayerSimpleDTO 사용
    private Long userId;
    private String userName;
}