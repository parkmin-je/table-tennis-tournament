package com.parkminje.pingpong.match;

import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.bracket.KnockoutLayout;
import com.parkminje.pingpong.common.BaseTimeEntity;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.tournament.Tournament;
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
import java.util.List;

/**
 * 경기. 본선 경기는 nextMatch/nextSlot 으로 다음 경기와 연결된 트리 구조다.
 */
@Getter
@Entity
@Table(name = "matches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private MatchStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private MatchGroup group;

    /** 예선: 리그 라운드 / 본선: 1부터 시작하는 라운드 번호 */
    @Column(name = "round_no", nullable = false)
    private int roundNo;

    /** 본선: 이 라운드의 인원 (16 → 16강) */
    @Column(name = "bracket_size")
    private Integer bracketSize;

    /** 라운드 내 위치 (0부터) */
    @Column(name = "bracket_pos", nullable = false)
    private int bracketPos;

    /** 대회 전체 진행 순서 */
    @Column(name = "play_order", nullable = false)
    private int playOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry1_id")
    private Entry entry1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry2_id")
    private Entry entry2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Entry winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    /** 다음 경기에서 들어갈 자리 (1 또는 2) */
    @Column(name = "next_slot")
    private Integer nextSlot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "result_type", length = 20)
    private ResultType resultType;

    private Integer games1;
    private Integer games2;

    @Column(name = "game_scores", length = 200)
    private String gameScores;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Version
    private long version;

    public static Match group(Tournament t, MatchGroup group, int round, int pos, int order, Entry e1, Entry e2) {
        Match m = new Match();
        m.tournament = t;
        m.stage = MatchStage.GROUP;
        m.group = group;
        m.roundNo = round;
        m.bracketPos = pos;
        m.playOrder = order;
        m.entry1 = e1;
        m.entry2 = e2;
        return m;
    }

    public static Match knockout(Tournament t, int round, int roundPlayers, int pos, int order) {
        Match m = new Match();
        m.tournament = t;
        m.stage = MatchStage.KNOCKOUT;
        m.roundNo = round;
        m.bracketSize = roundPlayers;
        m.bracketPos = pos;
        m.playOrder = order;
        return m;
    }

    public void linkNext(Match next, int slot) {
        this.nextMatch = next;
        this.nextSlot = slot;
    }

    public void unlinkNext() {
        this.nextMatch = null;
        this.nextSlot = null;
    }

    // ── 상태 전이 ─────────────────────────────────────────

    public void start(int table) {
        this.status = MatchStatus.IN_PROGRESS;
        this.tableNumber = table;
        this.startedAt = LocalDateTime.now();
    }

    public void cancelStart() {
        this.status = MatchStatus.SCHEDULED;
        this.tableNumber = null;
        this.startedAt = null;
    }

    public void finish(Entry winner, ResultType type, GameScore score) {
        this.winner = winner;
        this.resultType = type;
        this.games1 = score == null ? null : score.games1();
        this.games2 = score == null ? null : score.games2();
        this.gameScores = score == null ? null : score.format();
        this.status = MatchStatus.COMPLETED;
        this.tableNumber = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void clearResult() {
        this.winner = null;
        this.resultType = null;
        this.games1 = null;
        this.games2 = null;
        this.gameScores = null;
        this.status = MatchStatus.SCHEDULED;
        this.tableNumber = null;
        this.startedAt = null;
        this.finishedAt = null;
    }

    public void place(int slot, Entry entry) {
        if (slot == 1) {
            this.entry1 = entry;
        } else {
            this.entry2 = entry;
        }
    }

    // ── 조회 ──────────────────────────────────────────────

    public boolean isCompleted() {
        return status == MatchStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == MatchStatus.IN_PROGRESS;
    }

    public boolean isScheduled() {
        return status == MatchStatus.SCHEDULED;
    }

    public boolean isKnockout() {
        return stage == MatchStage.KNOCKOUT;
    }

    public boolean hasBothEntries() {
        return entry1 != null && entry2 != null;
    }

    public boolean isBye() {
        return resultType == ResultType.BYE;
    }

    public boolean involves(Entry e) {
        return e != null && ((entry1 != null && entry1.getId().equals(e.getId())) || (entry2 != null && entry2.getId().equals(e.getId())));
    }

    public Entry entryAt(int slot) {
        return slot == 1 ? entry1 : entry2;
    }

    public Entry opponentOf(Entry e) {
        if (entry1 != null && entry1.getId().equals(e.getId())) {
            return entry2;
        }
        return entry1;
    }

    public Entry getLoser() {
        if (winner == null || !hasBothEntries()) {
            return null;
        }
        return winner.getId().equals(entry1.getId()) ? entry2 : entry1;
    }

    public boolean isWinner(Entry e) {
        return winner != null && e != null && winner.getId().equals(e.getId());
    }

    public int winnerSide() {
        if (winner == null) {
            return 0;
        }
        return entry1 != null && winner.getId().equals(entry1.getId()) ? 1 : 2;
    }

    public String getRoundLabel() {
        if (stage == MatchStage.GROUP) {
            return (group != null ? group.getName() + " " : "") + roundNo + "R";
        }
        return KnockoutLayout.roundLabel(bracketSize);
    }

    /** 화면 경과 시간 계산용 (초 단위 ISO) */
    public String getStartedAtIso() {
        return startedAt == null ? null : startedAt.withNano(0).toString();
    }

    public List<int[]> getGameList() {
        return GameScore.parseStored(gameScores);
    }

    /** "3 : 1" / "부전승" 등 결과 요약 */
    public String getScoreLabel() {
        if (!isCompleted()) {
            return "";
        }
        if (resultType == ResultType.BYE) {
            return "BYE";
        }
        if (resultType == ResultType.WALKOVER) {
            return "부전승";
        }
        String base = (games1 == null ? "-" : games1) + " : " + (games2 == null ? "-" : games2);
        return resultType == ResultType.RETIRED ? base + " (기권)" : base;
    }
}
