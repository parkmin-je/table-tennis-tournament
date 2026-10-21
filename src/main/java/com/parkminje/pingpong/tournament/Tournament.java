package com.parkminje.pingpong.tournament;

import com.parkminje.pingpong.common.BaseTimeEntity;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tournaments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 200)
    private String venue;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TournamentStatus status = TournamentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;

    /** 게임 수 (3, 5, 7 …) */
    @Column(name = "best_of", nullable = false)
    private int bestOf;

    @Column(name = "max_entries")
    private Integer maxEntries;

    @Column(name = "group_size", nullable = false)
    private int groupSize;

    @Column(name = "advance_per_group", nullable = false)
    private int advancePerGroup;

    @Column(name = "table_count", nullable = false)
    private int tableCount;

    @Column(name = "entry_fee", nullable = false)
    private int entryFee;

    @Column(name = "division_min")
    private Integer divisionMin;

    @Column(name = "division_max")
    private Integer divisionMax;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender_restriction", nullable = false, length = 10)
    private GenderRestriction genderRestriction;

    @Column(name = "auto_confirm", nullable = false)
    private boolean autoConfirm;

    @Column(name = "require_check_in", nullable = false)
    private boolean requireCheckIn;

    @Column(name = "rating_applied", nullable = false)
    private boolean ratingApplied;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_entry_id")
    private Entry champion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Version
    private long version;

    public Tournament(TournamentSpec spec, User creator) {
        apply(spec);
        this.creator = creator;
    }

    public void apply(TournamentSpec spec) {
        this.title = spec.title();
        this.description = spec.description();
        this.venue = spec.venue();
        this.startAt = spec.startAt();
        this.endAt = spec.endAt();
        this.registrationDeadline = spec.registrationDeadline();
        this.format = spec.format();
        this.entryType = spec.entryType();
        this.bestOf = spec.bestOf();
        this.maxEntries = spec.maxEntries();
        this.groupSize = spec.groupSize();
        this.advancePerGroup = spec.advancePerGroup();
        this.tableCount = spec.tableCount();
        this.entryFee = spec.entryFee();
        this.divisionMin = spec.divisionMin();
        this.divisionMax = spec.divisionMax();
        this.genderRestriction = spec.genderRestriction();
        this.autoConfirm = spec.autoConfirm();
        this.requireCheckIn = spec.requireCheckIn();
    }

    /** 대진이 만들어진 뒤에는 바꾸면 안 되는 설정이 있는지 */
    public boolean changesStructure(TournamentSpec spec) {
        return format != spec.format() || entryType != spec.entryType() || bestOf != spec.bestOf()
                || groupSize != spec.groupSize() || advancePerGroup != spec.advancePerGroup();
    }

    public void changeStatus(TournamentStatus status) {
        this.status = status;
    }

    public void complete(Entry champion) {
        this.champion = champion;
        this.status = TournamentStatus.COMPLETED;
    }

    public void reopen() {
        this.champion = null;
        this.status = TournamentStatus.IN_PROGRESS;
    }

    public void markRatingApplied() {
        this.ratingApplied = true;
    }

    public void unmarkRatingApplied() {
        this.ratingApplied = false;
    }

    public boolean isRegistrationOpen(LocalDateTime now) {
        return status == TournamentStatus.RECRUITING
                && (registrationDeadline == null || now.isBefore(registrationDeadline));
    }

    public boolean isDoubles() {
        return entryType == EntryType.DOUBLES;
    }

    public boolean hasGroupStage() {
        return format == TournamentFormat.GROUP_KNOCKOUT;
    }

    public boolean acceptsDivision(Integer division) {
        if (divisionMin == null && divisionMax == null) {
            return true;
        }
        if (division == null) {
            return false;
        }
        return (divisionMin == null || division >= divisionMin) && (divisionMax == null || division <= divisionMax);
    }

    public String getDivisionLabel() {
        if (divisionMin == null && divisionMax == null) {
            return "부수 제한 없음";
        }
        if (divisionMin != null && divisionMax != null) {
            return divisionMin.equals(divisionMax) ? divisionMin + "부" : divisionMin + "~" + divisionMax + "부";
        }
        return divisionMin != null ? divisionMin + "부 이하" : "1~" + divisionMax + "부";
    }

    public String getBestOfLabel() {
        return bestOf + "게임 " + (bestOf / 2 + 1) + "선승제";
    }
}
