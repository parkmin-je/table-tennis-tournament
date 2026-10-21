package com.parkminje.pingpong.tournament;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntryType {
    SINGLES("단식"),
    DOUBLES("복식");

    private final String label;
}
