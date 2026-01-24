package com.yourcompany.pingpong.modules.tournament.dto;

import com.yourcompany.pingpong.domain.Tournament;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TournamentSimpleDTO {
    private Long id;
    private String title;
    private String status;
    private String type;

    // ⭐⭐⭐ Enum을 String으로 변환! ⭐⭐⭐
    public TournamentSimpleDTO(Tournament tournament) {
        this.id = tournament.getId();
        this.title = tournament.getTitle();
        
        // TournamentStatus Enum → String 변환
        this.status = tournament.getStatus().getDescription();  // "진행중", "모집중"
        
        // ⭐ TournamentType Enum → String 변환
        this.type = tournament.getType().getDescription();  // "단식", "복식"
    }
}