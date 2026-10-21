package com.parkminje.pingpong.tournament;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class TournamentForm {

    private static final String DT = "yyyy-MM-dd'T'HH:mm";

    @NotBlank(message = "대회명을 입력하세요.")
    @Size(max = 200)
    private String title;

    @Size(max = 4000, message = "소개는 4000자 이내로 입력하세요.")
    private String description;

    @Size(max = 200)
    private String venue;

    @NotNull(message = "시작 일시를 입력하세요.")
    @DateTimeFormat(pattern = DT)
    private LocalDateTime startAt;

    @NotNull(message = "종료 일시를 입력하세요.")
    @DateTimeFormat(pattern = DT)
    private LocalDateTime endAt;

    @DateTimeFormat(pattern = DT)
    private LocalDateTime registrationDeadline;

    @NotNull
    private TournamentFormat format = TournamentFormat.GROUP_KNOCKOUT;

    @NotNull
    private EntryType entryType = EntryType.SINGLES;

    @Min(1)
    @Max(9)
    private int bestOf = 5;

    @Min(value = 2, message = "정원은 2팀 이상이어야 합니다.")
    private Integer maxEntries;

    @Min(3)
    @Max(8)
    private int groupSize = 4;

    @Min(1)
    @Max(4)
    private int advancePerGroup = 2;

    @Min(1)
    @Max(100)
    private int tableCount = 8;

    @Min(0)
    private int entryFee;

    @Min(1)
    @Max(9)
    private Integer divisionMin;

    @Min(1)
    @Max(9)
    private Integer divisionMax;

    @NotNull
    private GenderRestriction genderRestriction = GenderRestriction.ANY;

    private boolean autoConfirm;

    private boolean requireCheckIn = true;

    public TournamentSpec toSpec() {
        return new TournamentSpec(title.trim(), blank(description), blank(venue), startAt, endAt, registrationDeadline,
                format, entryType, bestOf, maxEntries, groupSize, advancePerGroup, tableCount, entryFee,
                divisionMin, divisionMax, genderRestriction, autoConfirm, requireCheckIn);
    }

    public static TournamentForm from(Tournament t) {
        TournamentForm f = new TournamentForm();
        f.title = t.getTitle();
        f.description = t.getDescription();
        f.venue = t.getVenue();
        f.startAt = t.getStartAt();
        f.endAt = t.getEndAt();
        f.registrationDeadline = t.getRegistrationDeadline();
        f.format = t.getFormat();
        f.entryType = t.getEntryType();
        f.bestOf = t.getBestOf();
        f.maxEntries = t.getMaxEntries();
        f.groupSize = t.getGroupSize();
        f.advancePerGroup = t.getAdvancePerGroup();
        f.tableCount = t.getTableCount();
        f.entryFee = t.getEntryFee();
        f.divisionMin = t.getDivisionMin();
        f.divisionMax = t.getDivisionMax();
        f.genderRestriction = t.getGenderRestriction();
        f.autoConfirm = t.isAutoConfirm();
        f.requireCheckIn = t.isRequireCheckIn();
        return f;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
