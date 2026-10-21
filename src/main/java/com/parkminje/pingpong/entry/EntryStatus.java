package com.parkminje.pingpong.entry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntryStatus {
    APPLIED("신청", "warning"),
    CONFIRMED("확정", "success"),
    WITHDRAWN("기권/취소", "secondary"),
    REJECTED("반려", "danger");

    private final String label;
    private final String tone;

    /** 정원 계산에 포함되는 상태 */
    public boolean isActive() {
        return this == APPLIED || this == CONFIRMED;
    }
}
