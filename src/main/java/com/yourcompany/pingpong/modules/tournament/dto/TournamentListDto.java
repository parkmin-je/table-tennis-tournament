package com.yourcompany.pingpong.modules.tournament.dto;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.domain.TournamentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 대회 목록 조회용 DTO
 * 
 * <p>대회 목록 페이지에서 사용되며, 참가자 수를 포함합니다.</p>
 */
@Getter
@Setter
public class TournamentListDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private TournamentStatus status;
    private TournamentType type;
    private String creatorName;
    private int participationCount;

    /**
     * Tournament 엔티티를 DTO로 변환
     * 
     * @param tournament 대회 엔티티
     */
    public TournamentListDto(Tournament tournament) {
        this.id = tournament.getId();
        this.title = tournament.getTitle();
        this.description = tournament.getDescription();
        this.startDate = tournament.getStartDate();
        this.endDate = tournament.getEndDate();
        this.status = tournament.getStatus();
        this.type = tournament.getType();
        this.creatorName = tournament.getCreator() != null ? 
            (tournament.getCreator().getName() != null ? 
                tournament.getCreator().getName() : 
                tournament.getCreator().getUsername()) : "알 수 없음";
        this.participationCount = 0; // Service에서 별도로 설정
    }
}
