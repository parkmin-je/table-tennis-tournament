package com.yourcompany.pingpong.modules.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 조 편성 정보를 담는 DTO
 * 대진표 페이지의 좌측 조 목록에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {

    private Long id;                    // 조 ID
    private String groupName;           // 조 이름 (예: "호성 5부", "A조", "B조")
    private Long tournamentId;          // 대회 ID
    private String tournamentTitle;     // 대회 제목
    private List<SeedDTO> seeds;        // 조에 속한 시드 목록
    private Integer totalSeeds;         // 총 시드 수
    private Integer completedMatches;   // 완료된 경기 수
    private Integer totalMatches;       // 총 경기 수
}