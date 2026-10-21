package com.parkminje.pingpong.match;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchStage {
    GROUP("예선 리그"),
    KNOCKOUT("본선");

    private final String label;
}
