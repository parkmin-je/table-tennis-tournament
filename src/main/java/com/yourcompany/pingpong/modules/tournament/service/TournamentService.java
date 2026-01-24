package com.yourcompany.pingpong.modules.tournament.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.TournamentStatus;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.tournament.dto.TournamentDto;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    /**
     * 전체 대회 목록 조회
     */
    public List<Tournament> getAllTournaments() {
        log.info("SERVICE INFO: Fetching all tournaments");
        return tournamentRepository.findAllWithParticipations();
    }

    /**
     * ⭐⭐⭐ 관리자용 전체 대회 목록 (Creator만 FETCH) ⭐⭐⭐
     */
    public List<Tournament> getAllTournamentsForAdmin() {
        log.info("SERVICE INFO: Fetching all tournaments for admin");
        return tournamentRepository.findAllWithCreator();
    }

    /**
     * 대회 단일 조회
     */
    public Tournament getTournamentById(Long id) {
        log.debug("SERVICE DEBUG: Fetching tournament with ID: {}", id);
        return tournamentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("SERVICE ERROR: Tournament not found with ID: {}", id);
                    return new IllegalArgumentException("대회를 찾을 수 없습니다. ID=" + id);
                });
    }

    /**
     * 대회 상세 조회 (Participations 포함)
     */
    public Tournament getTournamentByIdWithDetails(Long id) {
        log.debug("SERVICE DEBUG: Fetching tournament with details for ID: {}", id);
        return tournamentRepository.findByIdWithParticipations(id)
                .orElseThrow(() -> {
                    log.error("SERVICE ERROR: Tournament not found with ID: {}", id);
                    return new IllegalArgumentException("대회를 찾을 수 없습니다. ID=" + id);
                });
    }

    /**
     * 대회 생성
     */
    @Transactional
    public Tournament createTournament(TournamentDto dto, User creator) {
        log.info("SERVICE INFO: Creating tournament: {}", dto.getTitle());

        Tournament tournament = Tournament.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .type(dto.getType())
                .status(dto.getStatus() != null ? dto.getStatus() : TournamentStatus.READY)
                .name(dto.getName())
                .creator(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Tournament savedTournament = tournamentRepository.save(tournament);
        log.info("SERVICE INFO: Tournament created successfully with ID: {}", savedTournament.getId());

        return savedTournament;
    }

    /**
     * 대회 수정
     */
    @Transactional
    public Tournament updateTournament(Long id, TournamentDto dto) {
        log.info("SERVICE INFO: Updating tournament ID: {}", id);

        Tournament tournament = getTournamentById(id);

        tournament.setTitle(dto.getTitle());
        tournament.setDescription(dto.getDescription());
        tournament.setStartDate(dto.getStartDate());
        tournament.setEndDate(dto.getEndDate());
        tournament.setType(dto.getType());
        tournament.setStatus(dto.getStatus());
        tournament.setName(dto.getName());
        tournament.setUpdatedAt(LocalDateTime.now());

        Tournament updatedTournament = tournamentRepository.save(tournament);
        log.info("SERVICE INFO: Tournament updated successfully: {}", updatedTournament.getId());

        return updatedTournament;
    }

    /**
     * 대회 삭제
     */
    @Transactional
    public void deleteTournament(Long id) {
        log.info("SERVICE INFO: Deleting tournament ID: {}", id);

        Tournament tournament = getTournamentById(id);
        tournamentRepository.delete(tournament);

        log.info("SERVICE INFO: Tournament deleted successfully: {}", id);
    }

    /**
     * 진행 중인 대회 목록 조회
     */
    public List<Tournament> getOngoingTournaments() {
        log.info("SERVICE INFO: Fetching ongoing tournaments");
        List<TournamentStatus> ongoingStatuses = Arrays.asList(
                TournamentStatus.RECRUITING,
                TournamentStatus.IN_PROGRESS,
                TournamentStatus.MAIN_READY
        );
        return tournamentRepository.findOngoingTournamentsWithParticipations(ongoingStatuses);
    }
}