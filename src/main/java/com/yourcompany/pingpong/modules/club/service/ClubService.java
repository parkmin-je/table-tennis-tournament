package com.yourcompany.pingpong.modules.club.service;

import com.yourcompany.pingpong.domain.Club;
import com.yourcompany.pingpong.modules.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;

    /**
     * ✅ 선수 포함 전체 조회
     */
    public List<Club> getAllClubsWithPlayers() {
        log.info("SERVICE INFO: Fetching all clubs with players");
        return clubRepository.findAllWithPlayers();
    }

    /**
     * ✅ 일반 목록 조회
     */
    public List<Club> getAllClubs() {
        log.info("SERVICE INFO: Fetching all clubs");
        return clubRepository.findAll();
    }

    /**
     * ✅ 클럽 단일 조회
     */
    public Club getClub(Long id) {
        log.info("SERVICE INFO: Finding club by ID: {}", id);
        return clubRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("클럽을 찾을 수 없습니다. ID=" + id));
    }

    /**
     * ✅ 클럽 저장
     */
    @Transactional
    public Club saveClub(Club club) {
        log.info("SERVICE INFO: Saving new club: {}", club.getName());
        if (club.getCreatedAt() == null) {
            club.setCreatedAt(LocalDateTime.now());
        }
        if (club.getUpdatedAt() == null) {
            club.setUpdatedAt(LocalDateTime.now());
        }
        return clubRepository.save(club);
    }

    /**
     * ⭐ 클럽 수정
     */
    @Transactional
    public Club updateClub(Club club) {
        log.info("SERVICE INFO: Updating club with ID: {}", club.getId());

        Club existingClub = clubRepository.findById(club.getId())
                .orElseThrow(() -> new IllegalArgumentException("클럽을 찾을 수 없습니다. ID=" + club.getId()));

        existingClub.setName(club.getName());
        existingClub.setRegion(club.getRegion());
        existingClub.setUpdatedAt(LocalDateTime.now());

        return clubRepository.save(existingClub);
    }

    /**
     * ✅ 클럽 삭제
     */
    @Transactional
    public void deleteClub(Long id) {
        log.info("SERVICE INFO: Deleting club with ID: {}", id);

        if (!clubRepository.existsById(id)) {
            throw new IllegalArgumentException("클럽을 찾을 수 없습니다. ID=" + id);
        }

        clubRepository.deleteById(id);
        log.info("SERVICE INFO: Successfully deleted club with ID: {}", id);
    }

    /**
     * ⭐ 전체 클럽 수 조회
     */
    public long countAll() {
        return clubRepository.count();
    }
}