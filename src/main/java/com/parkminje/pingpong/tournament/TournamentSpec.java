package com.parkminje.pingpong.tournament;

import java.time.LocalDateTime;

/** 대회 생성·수정 시 입력값 묶음 */
public record TournamentSpec(
        String title,
        String description,
        String venue,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime registrationDeadline,
        TournamentFormat format,
        EntryType entryType,
        int bestOf,
        Integer maxEntries,
        int groupSize,
        int advancePerGroup,
        int tableCount,
        int entryFee,
        Integer divisionMin,
        Integer divisionMax,
        GenderRestriction genderRestriction,
        boolean autoConfirm,
        boolean requireCheckIn
) {
}
