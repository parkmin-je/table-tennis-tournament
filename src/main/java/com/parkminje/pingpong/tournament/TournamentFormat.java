package com.parkminje.pingpong.tournament;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TournamentFormat {
    SINGLE_ELIMINATION("토너먼트", "시드 배정 후 바로 본선 토너먼트"),
    GROUP_KNOCKOUT("예선 리그 + 본선", "조별 리그 상위 입상자가 본선 토너먼트 진출");

    private final String label;
    private final String description;
}
