package com.yourcompany.pingpong.modules.tournament.service;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.tournament.dto.TournamentDto;

import java.util.List;

/**
 * 토너먼트 관리 서비스 인터페이스
 * 
 * <p>이 서비스는 탁구 토너먼트의 생성, 조회, 수정, 삭제를 담당합니다.</p>
 * 
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>대회 CRUD 작업</li>
 *   <li>진행 중인 대회 조회</li>
 *   <li>관리자용 대회 목록 조회</li>
 * </ul>
 * 
 * @author table-tennis-tournament
 * @version 1.0
 * @since 2026-01-24
 */
public interface TournamentService {

    /**
     * 전체 대회 목록 조회
     * 
     * @return 모든 대회 목록 (참가자 정보 포함)
     */
    List<Tournament> getAllTournaments();

    /**
     * 관리자용 전체 대회 목록 조회
     * 
     * <p>참가자 정보를 제외하고 Creator만 함께 조회하여 성능 최적화</p>
     * 
     * @return 모든 대회 목록 (생성자 정보만 포함)
     */
    List<Tournament> getAllTournamentsForAdmin();

    /**
     * 대회 단일 조회
     * 
     * @param id 대회 ID
     * @return 대회 엔티티
     * @throws IllegalArgumentException 대회를 찾을 수 없는 경우
     */
    Tournament getTournamentById(Long id);

    /**
     * 대회 상세 조회 (참가자 정보 포함)
     * 
     * @param id 대회 ID
     * @return 대회 엔티티 (참가자 정보 포함)
     * @throws IllegalArgumentException 대회를 찾을 수 없는 경우
     */
    Tournament getTournamentByIdWithDetails(Long id);

    /**
     * 대회 생성
     * 
     * @param dto 대회 생성 정보
     * @param creator 대회 생성자
     * @return 생성된 대회 엔티티
     */
    Tournament createTournament(TournamentDto dto, User creator);

    /**
     * 대회 정보 수정
     * 
     * @param id 대회 ID
     * @param dto 수정할 대회 정보
     * @return 수정된 대회 엔티티
     * @throws IllegalArgumentException 대회를 찾을 수 없는 경우
     */
    Tournament updateTournament(Long id, TournamentDto dto);

    /**
     * 대회 삭제
     * 
     * @param id 대회 ID
     * @throws IllegalArgumentException 대회를 찾을 수 없는 경우
     */
    void deleteTournament(Long id);

    /**
     * 진행 중인 대회 목록 조회
     * 
     * <p>모집중, 진행중, 본선 준비중 상태의 대회를 조회합니다.</p>
     * 
     * @return 진행 중인 대회 목록
     */
    List<Tournament> getOngoingTournaments();
}
