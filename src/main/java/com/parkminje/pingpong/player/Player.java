package com.parkminje.pingpong.player;

import com.parkminje.pingpong.bracket.Elo;
import com.parkminje.pingpong.club.Club;
import com.parkminje.pingpong.common.BaseTimeEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 선수. 회원(User)과 1:1로 연결될 수도 있고, 현장 접수처럼 회원 없이 등록될 수도 있다.
 */
@Getter
@Entity
@Table(name = "players")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    /** 생활체육 부수 (1부가 가장 상위, null = 미정) */
    private Integer division;

    @Column(nullable = false)
    private int rating = Elo.INITIAL;

    @Column(name = "rated_matches", nullable = false)
    private int ratedMatches;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Version
    private long version;

    public Player(String name, Gender gender, Integer birthYear, Integer division, String phone, Club club, User user) {
        update(name, gender, birthYear, division, phone, club);
        this.user = user;
    }

    public void update(String name, Gender gender, Integer birthYear, Integer division, String phone, Club club) {
        this.name = name;
        this.gender = gender;
        this.birthYear = birthYear;
        this.division = division;
        this.phone = phone;
        this.club = club;
    }

    public void applyRating(int newRating, int matchesPlayed) {
        this.rating = newRating;
        this.ratedMatches += matchesPlayed;
    }

    public void linkUser(User user) {
        this.user = user;
    }

    public String getDivisionLabel() {
        return division == null ? "부수 미정" : division + "부";
    }

    public String getClubName() {
        return club == null ? "무소속" : club.getName();
    }
}
