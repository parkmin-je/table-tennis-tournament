// ===== TournamentStatus.java =====
package com.yourcompany.pingpong.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TournamentStatus {
    READY("준비중", "대회 준비 단계입니다"),
    RECRUITING("접수중", "참가 신청을 받고 있습니다"),
    IN_PROGRESS("진행중", "대회가 진행 중입니다"),
    MAIN_READY("본선 준비 완료", "예선이 끝나고 본선을 시작할 준비가 완료되었습니다"), // ⭐ 추가
    COMPLETED("종료", "대회가 종료되었습니다");

    private final String description;
    private final String detail;
}
