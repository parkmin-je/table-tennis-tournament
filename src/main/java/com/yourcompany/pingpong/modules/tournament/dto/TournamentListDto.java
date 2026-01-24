// ===== TournamentListDto.java (새 파일) =====
package com.yourcompany.pingpong.modules.tournament.dto;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.domain.TournamentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private int participationCount; // ✅ 참가자 수를 미리 계산

    // ✅ Tournament 엔티티에서 DTO로 변환하는 생성자
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
        // ✅ participations 컬렉션을 직접 접근하지 않고 Repository에서 COUNT 쿼리로 가져옴
        this.participationCount = 0; // Service에서 설정
    }
}

