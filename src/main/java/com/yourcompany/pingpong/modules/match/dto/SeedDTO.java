package com.yourcompany.pingpong.modules.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시드 정보를 담는 DTO
 * 조 편성에서 각 선수의 시드 번호와 정보를 표시
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedDTO {

    private Long participationId;   // 참가 ID
    private Integer seedNumber;     // 시드 번호 (1, 2, 3, ...)
    private Long playerId;          // 선수 ID
    private String playerName;      // 선수 이름
    private String clubName;        // 소속 클럽
    private Integer ranking;        // 랭킹
    private Boolean isWinner;       // 조 진출 여부 (본선 진출자)
    private Integer wins;           // 승수
    private Integer losses;         // 패수
}