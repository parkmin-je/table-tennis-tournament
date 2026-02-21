package com.yourcompany.pingpong.domain;

// 경기의 현재 상태를 정의하는 Enum
public enum MatchStatus {
    SCHEDULED("예정"),       // 경기가 예정되어 있으나 아직 시작되지 않음 (탁구대 미배정 또는 대기)
    IN_PROGRESS("진행중"),   // 경기가 탁구대에 배정되어 진행 중
    COMPLETED("완료"),       // 경기가 종료되어 결과가 입력됨
    CANCELLED("취소됨");     // 경기가 취소됨 (특별한 상황에 대비)

    private final String description;

    MatchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}