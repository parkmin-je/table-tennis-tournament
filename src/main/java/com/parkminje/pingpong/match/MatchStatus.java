package com.parkminje.pingpong.match;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchStatus {
    SCHEDULED("대기", "secondary"),
    IN_PROGRESS("진행중", "live"),
    COMPLETED("종료", "dark");

    private final String label;
    private final String tone;
}
