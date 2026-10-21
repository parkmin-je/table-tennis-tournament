package com.parkminje.pingpong.match;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultType {
    NORMAL("정상"),
    WALKOVER("부전승"),
    RETIRED("기권승"),
    BYE("BYE");

    private final String label;

    /** 레이팅·득실 계산에서 제외할 결과 */
    public boolean isUnplayed() {
        return this == WALKOVER || this == BYE;
    }
}
