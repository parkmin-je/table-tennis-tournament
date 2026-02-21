package com.yourcompany.pingpong.modules.tournament.dto;

import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.domain.TournamentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TournamentDto {

    @NotBlank(message = "대회명은 필수 항목입니다.")
    private String title;

    @NotBlank(message = "설명은 필수 항목입니다.")
    private String description;

    @NotNull(message = "시작일시는 필수 항목입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일시는 필수 항목입니다.")
    private LocalDateTime endDate;

    @NotNull(message = "경기 유형은 필수 항목입니다.")
    private TournamentType type;

    @NotNull(message = "대회 상태는 필수 항목입니다.")
    private TournamentStatus status;

    // ⭐⭐⭐ 주최 단체명 필드 추가 ⭐⭐⭐
    @NotBlank(message = "주최 단체명은 필수 항목입니다.")
    private String name;
}