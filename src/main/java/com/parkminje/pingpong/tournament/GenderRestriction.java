package com.parkminje.pingpong.tournament;

import com.parkminje.pingpong.player.Gender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GenderRestriction {
    ANY("성별 무관"),
    MALE("남자부"),
    FEMALE("여자부");

    private final String label;

    public boolean allows(Gender gender) {
        return switch (this) {
            case ANY -> true;
            case MALE -> gender == Gender.MALE;
            case FEMALE -> gender == Gender.FEMALE;
        };
    }
}
