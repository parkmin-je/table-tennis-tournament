package com.yourcompany.pingpong.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 유형 Enum
 * - 단식, 복식, 혼합복식, 리그+토너먼트 등
 */
@Getter
@RequiredArgsConstructor
public enum TournamentType {
    SINGLES("단식"),
    DOUBLES("복식"),
    MIXED_DOUBLES("혼합복식"),
    TEAM("단체전"),
    LEAGUE_AND_TOURNAMENT("예선 리그 + 본선 토너먼트");  // ⭐ 추가
    
    private final String description;
}