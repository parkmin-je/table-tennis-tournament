package com.parkminje.pingpong.entry;

import com.parkminje.pingpong.common.BaseTimeEntity;
import com.parkminje.pingpong.match.MatchGroup;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.tournament.Tournament;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 대회 참가 단위(엔트리). 단식은 선수 1명, 복식은 선수 + 파트너.
 * 경기·승자는 모두 엔트리를 기준으로 기록하므로 동명이인이 있어도 섞이지 않는다.
 */
@Getter
@Entity
@Table(name = "entries", uniqueConstraints = @UniqueConstraint(name = "uk_entries_player", columnNames = {"tournament_id", "player_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Player partner;

    /** 신청한 회원 (운영진 현장 접수면 운영진) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EntryStatus status;

    @Column(name = "checked_in", nullable = false)
    private boolean checkedIn;

    @Column(nullable = false)
    private boolean paid;

    /** 운영진 지정 시드 (작을수록 상위, null = 레이팅 순) */
    private Integer seed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private MatchGroup group;

    @Column(length = 200)
    private String memo;

    @Version
    private long version;

    public Entry(Tournament tournament, Player player, Player partner, User applicant, EntryStatus status) {
        this.tournament = tournament;
        this.player = player;
        this.partner = partner;
        this.applicant = applicant;
        this.status = status;
    }

    public void reactivate(Player partner, User applicant, EntryStatus status) {
        this.partner = partner;
        this.applicant = applicant;
        this.status = status;
        this.checkedIn = false;
        this.paid = false;
        this.seed = null;
        this.group = null;
    }

    public void changeStatus(EntryStatus status) {
        this.status = status;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void assignGroup(MatchGroup group) {
        this.group = group;
    }

    public boolean isWithdrawn() {
        return status == EntryStatus.WITHDRAWN;
    }

    /** 화면 표시명: "홍길동" 또는 "홍길동 · 김철수" */
    public String getDisplayName() {
        return partner == null ? player.getName() : player.getName() + " · " + partner.getName();
    }

    /** 동명이인 구분용 부가 정보 */
    public String getSubLabel() {
        String club = player.getClubName();
        if (partner != null && !partner.getClubName().equals(club)) {
            club = club + " / " + partner.getClubName();
        }
        return club;
    }

    /** 시드 결정용 레이팅 (복식은 평균) */
    public int getStrength() {
        return partner == null ? player.getRating() : (player.getRating() + partner.getRating()) / 2;
    }

    public boolean includes(Player p) {
        return player.getId().equals(p.getId()) || (partner != null && partner.getId().equals(p.getId()));
    }

    public boolean belongsTo(User user) {
        if (user == null) {
            return false;
        }
        return isUser(player, user) || isUser(partner, user) || (applicant != null && applicant.getId().equals(user.getId()));
    }

    private static boolean isUser(Player p, User user) {
        return p != null && p.getUser() != null && p.getUser().getId().equals(user.getId());
    }

    /** 프록시와 실제 객체를 같은 엔트리로 보도록 ID 기준 비교 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Entry other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(getId());
    }
}
