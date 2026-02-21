package com.yourcompany.pingpong.modules.match.service;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.Tournament;

import java.util.List;
import java.util.Map;

/**
 * 토너먼트 대진표 관리 서비스 인터페이스
 * 
 * <p>이 서비스는 탁구 토너먼트의 대진표 생성 및 관리를 담당합니다.</p>
 * 
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>예선 경기 생성 (조별 리그전 또는 단일 토너먼트)</li>
 *   <li>본선 대진표 자동 생성</li>
 *   <li>경기 완료 시 승자 자동 진출</li>
 *   <li>대진표 데이터 조회</li>
 * </ul>
 * 
 * @author table-tennis-tournament
 * @version 1.0
 * @since 2026-01-28
 */
public interface BracketService {

    /**
     * 예선 경기 생성
     * 
     * <p>토너먼트의 조 편성 유무에 따라 다른 방식으로 경기를 생성합니다:</p>
     * <ul>
     *   <li>조 편성이 있는 경우: 각 조별로 리그전 방식의 경기 생성</li>
     *   <li>조 편성이 없는 경우: 전체 참가자로 단일 토너먼트 생성</li>
     * </ul>
     * 
     * @param tournament 대회 엔티티
     * @return 생성된 경기 수
     * @throws IllegalStateException Player 엔티티가 없는 참가자가 있는 경우
     */
    int createPreliminaryMatches(Tournament tournament);

    /**
     * 본선 대진표 생성
     * 
     * <p>예선 결과를 기반으로 본선 토너먼트 대진표를 자동 생성합니다.</p>
     * <p>조별 예선의 경우 각 조에서 상위 2명(기본값)이 본선에 진출합니다.</p>
     * 
     * @param tournament 대회 엔티티
     * @return 생성된 본선 경기 목록
     * @throws IllegalStateException 예선이 완료되지 않았거나 본선이 이미 생성된 경우
     */
    List<Match> createFinalBracket(Tournament tournament);

    /**
     * 경기 완료 시 승자를 다음 라운드로 자동 진출
     * 
     * <p>현재 라운드의 모든 경기가 완료되면 자동으로 다음 라운드 경기를 생성합니다.</p>
     * 
     * @param match 완료된 경기 엔티티
     */
    void autoAdvanceWinner(Match match);

    /**
     * 본선 대진표 생성 여부 확인
     * 
     * @param tournamentId 대회 ID
     * @return 본선 대진표 생성 여부 (true: 생성됨, false: 미생성)
     */
    boolean isFinalBracketGenerated(Long tournamentId);

    /**
     * 대진표 데이터 조회 (프론트엔드용)
     * 
     * <p>jQuery Bracket.js 라이브러리 형식의 JSON 데이터를 반환합니다.</p>
     * 
     * @param tournamentId 대회 ID
     * @return 대진표 데이터 Map (teams, results 포함)
     * @throws IllegalArgumentException 대회를 찾을 수 없는 경우
     */
    Map<String, Object> getBracketData(Long tournamentId);
}
