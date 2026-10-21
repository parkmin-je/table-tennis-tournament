package com.parkminje.pingpong.tournament;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TournamentStatus {
    DRAFT("준비중", "secondary"),
    RECRUITING("접수중", "success"),
    CLOSED("접수마감", "warning"),
    IN_PROGRESS("진행중", "live"),
    COMPLETED("종료", "dark"),
    CANCELLED("취소", "danger");

    private final String label;
    /** 화면 배지 스타일 키 */
    private final String tone;

    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED;
    }
}
