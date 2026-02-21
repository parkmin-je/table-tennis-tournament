package com.yourcompany.pingpong.modules.tournament.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yourcompany.pingpong.domain.Match;
import com.yourcompany.pingpong.domain.MatchGroup;
import com.yourcompany.pingpong.domain.MatchStatus;
import com.yourcompany.pingpong.domain.Participation;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.match.dto.GroupDTO;
import com.yourcompany.pingpong.modules.match.dto.MatchDTO;
import com.yourcompany.pingpong.modules.match.dto.SeedDTO;
import com.yourcompany.pingpong.modules.tournament.dto.TournamentDto;
import com.yourcompany.pingpong.modules.match.repository.MatchGroupRepository;
import com.yourcompany.pingpong.modules.match.repository.MatchRepository;
import com.yourcompany.pingpong.modules.match.service.BracketService;
import com.yourcompany.pingpong.modules.match.service.MatchService;
import com.yourcompany.pingpong.modules.match.service.ParticipationService;
import com.yourcompany.pingpong.modules.tournament.service.TournamentService;
import com.yourcompany.pingpong.modules.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tournament")
public class TournamentController {

    private final TournamentService tournamentService;
    private final ParticipationService participationService;
    private final UserService userService;
    private final MatchService matchService;
    private final MatchGroupRepository matchGroupRepository;
    private final BracketService bracketService;
    private final MatchRepository matchRepository;

    /**
     * 대회 목록
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        model.addAttribute("tournaments", tournaments);
        return "tournament/list";
    }

    /**
     * 대회 상세
     */
    // TournamentController.java의 detail 메서드를 이렇게 수정:

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);

        // 전체 매치 조회
        List<Match> matches = matchService.getMatchesByTournamentId(id);

        // ⭐⭐⭐ 본선 대진표 생성 여부 확인 (match_group_id가 NULL인 경기만) ⭐⭐⭐
        boolean isMainBracketGenerated = matches.stream()
                .anyMatch(m -> m.getMatchGroup() == null);  // 본선 경기 있으면 true

        // SCHEDULED 상태 경기
        List<Match> scheduledMatches = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.SCHEDULED)
                .collect(Collectors.toList());

        // IN_PROGRESS 상태 경기
        List<Match> inProgressMatches = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        model.addAttribute("tournament", tournament);
        model.addAttribute("isMainBracketGenerated", isMainBracketGenerated);
        model.addAttribute("scheduledMatches", scheduledMatches);
        model.addAttribute("inProgressMatches", inProgressMatches);

        return "tournament/detail";
    }

    /**
     * 대회 생성 폼 (GET)
     */
    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createTournamentForm(Model model) {
        model.addAttribute("tournamentDto", new TournamentDto());
        return "tournament/tournament_form";
    }

    /**
     * 대회 생성 처리 (POST)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createTournament(@Valid @ModelAttribute TournamentDto tournamentDto,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {

        if (bindingResult.hasErrors()) {
            log.error("Validation errors: {}", bindingResult.getAllErrors());
            return "tournament/tournament_form";
        }

        try {
            // 현재 로그인한 사용자 정보 가져오기
            User creator = userService.getUserByUsername(userDetails.getUsername());

            // TournamentService의 createTournament 메서드 호출 (TournamentDto, User 필요)
            tournamentService.createTournament(tournamentDto, creator);
            log.info("Tournament created successfully: {}", tournamentDto.getTitle());

            return "redirect:/tournament/list";

        } catch (Exception e) {
            log.error("Error creating tournament", e);
            model.addAttribute("errorMessage", "대회 생성 중 오류가 발생했습니다: " + e.getMessage());
            return "tournament/tournament_form";
        }
    }

    /**
     * 대회 수정 폼 (GET)
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentForm(@PathVariable("id") Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);

            // Tournament -> TournamentDto 변환
            TournamentDto dto = new TournamentDto();
            dto.setTitle(tournament.getTitle());
            dto.setDescription(tournament.getDescription());
            dto.setStartDate(tournament.getStartDate());
            dto.setEndDate(tournament.getEndDate());
            dto.setType(tournament.getType());
            dto.setStatus(tournament.getStatus());
            dto.setName(tournament.getName());

            model.addAttribute("tournamentDto", dto);
            model.addAttribute("tournamentId", id);

            return "tournament/tournament_edit";

        } catch (Exception e) {
            log.error("Error loading tournament for edit", e);
            model.addAttribute("errorMessage", "대회 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/tournament/list";
        }
    }

    /**
     * 대회 수정 처리 (POST)
     */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateTournament(@PathVariable("id") Long id,
                                   @Valid @ModelAttribute TournamentDto tournamentDto,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.error("Validation errors: {}", bindingResult.getAllErrors());
            model.addAttribute("tournamentId", id);
            return "tournament/tournament_edit";
        }

        try {
            // TournamentService의 updateTournament 메서드 호출 (Long, TournamentDto 필요)
            tournamentService.updateTournament(id, tournamentDto);

            log.info("Tournament updated successfully: {}", tournamentDto.getTitle());
            redirectAttributes.addFlashAttribute("message", "대회 정보가 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            return "redirect:/tournament/detail/" + id;

        } catch (Exception e) {
            log.error("Error updating tournament", e);
            model.addAttribute("errorMessage", "대회 수정 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("tournamentId", id);
            return "tournament/tournament_edit";
        }
    }

    /**
     * 대회 삭제
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTournament(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            tournamentService.deleteTournament(id);
            log.info("Tournament deleted: {}", id);
            redirectAttributes.addFlashAttribute("message", "대회가 삭제되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            log.error("Error deleting tournament", e);
            redirectAttributes.addFlashAttribute("message", "대회 삭제 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/tournament/list";
    }

    /**
     * 참가 신청
     */
    @PostMapping("/participate/{id}")
    public String participate(@PathVariable("id") Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("message", "로그인이 필요합니다.");
            redirectAttributes.addFlashAttribute("alertType", "danger");
            return "redirect:/user/login";
        }

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());
            Tournament tournament = tournamentService.getTournamentById(id);

            participationService.applyParticipation(user, tournament);

            redirectAttributes.addFlashAttribute("message", "참가 신청이 완료되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "warning");
        } catch (Exception e) {
            log.error("Error in participation", e);
            redirectAttributes.addFlashAttribute("message", "참가 신청 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }

        return "redirect:/tournament/detail/" + id;
    }

    /**
     * 대진표 페이지
     */
    @GetMapping("/bracket/{id}")
    public String bracket(@PathVariable("id") Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        return "tournament/bracket";
    }

    /**
     * ⭐⭐⭐ [NEW] 조 편성 목록 API ⭐⭐⭐
     */
    @GetMapping("/groupBrackets/{tournamentId}")
    @ResponseBody
    @Transactional(readOnly = true)  // ⭐ 추가: 지연 로딩 문제 해결
    public ResponseEntity<?> getGroupBrackets(@PathVariable Long tournamentId) {
        try {
            log.info("Loading group brackets for tournament: {}", tournamentId);

            Tournament tournament = tournamentService.getTournamentById(tournamentId);

            // 조 목록 조회
            List<MatchGroup> groups = matchGroupRepository.findByTournament(tournament);

            if (groups.isEmpty()) {
                log.info("No groups found for tournament: {}", tournamentId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // MatchGroup -> GroupDTO 변환
            List<GroupDTO> groupDTOs = groups.stream()
                    .map(group -> {
                        // 조에 속한 참가자 목록
                        List<SeedDTO> seeds = group.getParticipations().stream()  // ⭐ @Transactional로 해결
                                .sorted(Comparator.comparing(Participation::getSeedNumber,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(participation -> SeedDTO.builder()
                                        .participationId(participation.getId())
                                        .seedNumber(participation.getSeedNumber())
                                        .playerId(participation.getPlayer() != null ?
                                                participation.getPlayer().getId() : null)
                                        .playerName(participation.getPlayer() != null ?
                                                participation.getPlayer().getName() : "BYE")
                                        .clubName(participation.getPlayer() != null &&
                                                participation.getPlayer().getClub() != null ?
                                                participation.getPlayer().getClub().getName() : "-")
                                        .ranking(participation.getPlayer() != null ?
                                                participation.getPlayer().getRanking() : null)
                                        .isWinner(false) // TODO: 실제 진출 여부 로직 추가
                                        .build())
                                .collect(Collectors.toList());

                        // 조의 경기 통계
                        List<Match> groupMatches = group.getMatches();  // ⭐ @Transactional로 해결
                        long completedMatches = groupMatches.stream()
                                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                                .count();

                        return GroupDTO.builder()
                                .id(group.getId())
                                .groupName(group.getName())
                                .tournamentId(tournament.getId())
                                .tournamentTitle(tournament.getTitle())
                                .seeds(seeds)
                                .totalSeeds(seeds.size())
                                .completedMatches((int) completedMatches)
                                .totalMatches(groupMatches.size())
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("Successfully loaded {} groups for tournament {}", groupDTOs.size(), tournamentId);
            return ResponseEntity.ok(groupDTOs);

        } catch (Exception e) {
            log.error("Error loading group brackets for tournament: {}", tournamentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "조 편성을 불러오는 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ⭐⭐⭐ 예선 대진표 데이터 반환 (JSON API) ⭐⭐⭐
     */
    @GetMapping("/preliminaryBracket/{id}")
    @ResponseBody
    public ResponseEntity<?> getPreliminaryBracket(@PathVariable("id") Long id) {
        try {
            log.info("Loading preliminary bracket for tournament: {}", id);

            List<Match> allMatches = matchService.getMatchesByTournamentId(id);

            // 예선 경기만 필터링 (조가 있는 경기)
            List<Match> preliminaryMatches = allMatches.stream()
                    .filter(m -> m.getMatchGroup() != null) // 조가 있는 경기 = 예선
                    .filter(m -> "예선".equals(m.getRoundName()))
                    .sorted(Comparator.comparing((Match m) -> m.getMatchGroup().getId())
                            .thenComparing(Match::getMatchNumber))
                    .collect(Collectors.toList());

            if (preliminaryMatches.isEmpty()) {
                log.warn("No preliminary matches found for tournament: {}", id);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Match -> MatchDTO 변환
            List<MatchDTO> matchDTOs = preliminaryMatches.stream()
                    .map(MatchDTO::fromEntity)
                    .collect(Collectors.toList());

            log.info("Successfully loaded {} preliminary matches", matchDTOs.size());
            return ResponseEntity.ok(matchDTOs);

        } catch (Exception e) {
            log.error("Error loading preliminary bracket for tournament: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "예선 대진표를 불러오는 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ⭐⭐⭐ 본선 대진표 데이터 반환 (JSON API) ⭐⭐⭐
     */
    @GetMapping("/mainBracket/{id}")
    @ResponseBody
    public ResponseEntity<?> getMainBracket(@PathVariable("id") Long id) {
        try {
            log.info("Loading main bracket for tournament: {}", id);

            List<Match> allMatches = matchService.getMatchesByTournamentId(id);

            // 본선 경기만 필터링
            List<Match> mainBracketMatches = allMatches.stream()
                    .filter(m -> m.getMatchGroup() == null) // 조가 없는 경기 = 본선
                    .filter(m -> {
                        String rn = m.getRoundName();
                        return rn != null && (
                                rn.contains("32강") ||  // ⭐ 32강 추가!
                                        rn.contains("본선") ||
                                        rn.contains("16강") ||
                                        rn.contains("8강") ||
                                        rn.contains("4강") ||
                                        rn.contains("준결승") ||
                                        rn.contains("결승")
                        );
                    })
                    .sorted(Comparator.comparing(Match::getRoundName)
                            .thenComparing(Match::getMatchNumber))
                    .collect(Collectors.toList());

            if (mainBracketMatches.isEmpty()) {
                log.warn("No main bracket matches found for tournament: {}", id);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Match -> MatchDTO 변환
            List<MatchDTO> matchDTOs = mainBracketMatches.stream()
                    .map(MatchDTO::fromEntity)
                    .collect(Collectors.toList());

            log.info("Successfully loaded {} main bracket matches", matchDTOs.size());
            return ResponseEntity.ok(matchDTOs);

        } catch (Exception e) {
            log.error("Error loading main bracket for tournament: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "본선 대진표를 불러오는 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ⭐⭐⭐ 대회 통계 페이지 ⭐⭐⭐
     */
    @GetMapping("/statistics/{id}")
    public String statistics(@PathVariable("id") Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);
            List<Match> matches = matchService.getMatchesByTournamentId(id);

            long totalMatches = matches.size();
            long completedMatches = matches.stream()
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                    .count();

            int progressRate = totalMatches > 0 ? (int) ((completedMatches * 100) / totalMatches) : 0;

            Match highestScoreMatch = matches.stream()
                    .filter(m -> m.getScore1() != null && m.getScore2() != null)
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                    .max((m1, m2) -> {
                        int total1 = m1.getScore1() + m1.getScore2();
                        int total2 = m2.getScore1() + m2.getScore2();
                        return Integer.compare(total1, total2);
                    })
                    .orElse(null);

            model.addAttribute("tournament", tournament);
            model.addAttribute("totalMatches", totalMatches);
            model.addAttribute("completedMatches", completedMatches);
            model.addAttribute("progressRate", progressRate);
            model.addAttribute("highestScoreMatch", highestScoreMatch);

            return "tournament/statistics";

        } catch (Exception e) {
            log.error("Error loading tournament statistics", e);
            return "redirect:/tournament/list";
        }
    }

    /**
     * ⭐⭐⭐ 본선 대진표 생성 API ⭐⭐⭐
     */
    @PostMapping("/createMainBracket/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> createMainBracket(@PathVariable("id") Long id) {
        try {
            log.info("본선 대진표 생성 요청 - Tournament ID: {}", id);

            Tournament tournament = tournamentService.getTournamentById(id);

            // 이미 본선 대진표가 생성되었는지 확인
            if (bracketService.isFinalBracketGenerated(id)) {
                log.warn("본선 대진표가 이미 생성됨 - Tournament ID: {}", id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "본선 대진표가 이미 생성되었습니다."));
            }

            // 본선 대진표 생성 (각 조당 상위 2명 진출)
            List<Match> mainBracketMatches = bracketService.createFinalBracket(tournament);

            log.info("✅ 본선 대진표 생성 완료 - {} 경기 생성", mainBracketMatches.size());

            return ResponseEntity.ok(Map.of(
                    "message", "본선 대진표가 성공적으로 생성되었습니다.",
                    "matchCount", mainBracketMatches.size()
            ));

        } catch (IllegalStateException e) {
            log.error("본선 대진표 생성 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("본선 대진표 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "본선 대진표 생성 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ⭐⭐⭐ 예선 대진표 생성 API ⭐⭐⭐
     */
    @PostMapping("/createPreliminary/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> createPreliminary(@PathVariable("id") Long id) {
        try {
            log.info("예선 대진표 생성 요청 - Tournament ID: {}", id);

            Tournament tournament = tournamentService.getTournamentById(id);

            // 예선 경기 생성
            int matchCount = bracketService.createPreliminaryMatches(tournament);

            if (matchCount == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "예선 대진표를 생성할 수 없습니다. 조 편성이나 참가자를 확인해주세요."));
            }

            log.info("✅ 예선 대진표 생성 완료 - {} 경기 생성", matchCount);

            return ResponseEntity.ok(Map.of(
                    "message", "예선 대진표가 성공적으로 생성되었습니다.",
                    "matchCount", matchCount
            ));

        } catch (Exception e) {
            log.error("예선 대진표 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "예선 대진표 생성 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ⭐⭐⭐ 조 없는 예선 경기 조회 API (새로 추가) ⭐⭐⭐
     */
    @GetMapping("/preliminaryMatchesNoGroup/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPreliminaryMatchesNoGroup(@PathVariable("id") Long id) {
        try {
            log.info("조 없는 예선 경기 조회 - Tournament ID: {}", id);

            // 예선 경기 중 조가 없는 경기만 조회
            List<Match> preliminaryMatches = matchRepository
                    .findByTournamentIdAndRoundNameOrderByMatchNumberAsc(id, "예선")
                    .stream()
                    .filter(m -> m.getMatchGroup() == null) // 조 없는 예선만
                    .collect(Collectors.toList());

            if (preliminaryMatches.isEmpty()) {
                log.warn("조 없는 예선 경기 없음 - Tournament ID: {}", id);
                return ResponseEntity.ok(Collections.emptyMap());
            }

            List<MatchDTO> matchDTOs = preliminaryMatches.stream()
                    .map(MatchDTO::fromEntity)
                    .collect(Collectors.toList());

            log.info("✅ 조 없는 예선 경기 조회 완료 - {} 경기", matchDTOs.size());

            return ResponseEntity.ok(Map.of(
                    "matches", matchDTOs,
                    "totalMatches", matchDTOs.size(),
                    "hasGroups", false
            ));

        } catch (Exception e) {
            log.error("예선 경기 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "예선 경기를 불러오는 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }
    /**
     * ⭐⭐⭐ 조별 경기 목록 조회 API ⭐⭐⭐
     */
    @GetMapping("/groupMatches/{groupId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroupMatches(@PathVariable Long groupId) {
        try {
            log.info("조별 경기 조회 - Group ID: {}", groupId);

            MatchGroup group = matchGroupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("조를 찾을 수 없습니다."));

            // 조별 경기 조회 - Repository에서 직접 조회
            List<Match> matches = matchRepository.findByTournamentIdOrderByRoundNameAsc(group.getTournament().getId())
                    .stream()
                    .filter(m -> m.getMatchGroup() != null && m.getMatchGroup().getId().equals(groupId))
                    .sorted(Comparator.comparing(Match::getMatchNumber))
                    .collect(Collectors.toList());

            List<MatchDTO> matchDTOs = matches.stream()
                    .map(MatchDTO::fromEntity)
                    .collect(Collectors.toList());

            log.info("✅ 조별 경기 조회 완료 - {} 경기", matchDTOs.size());

            return ResponseEntity.ok(Map.of(
                    "groupId", groupId,
                    "groupName", group.getName(),
                    "matches", matchDTOs,
                    "totalMatches", matchDTOs.size()
            ));

        } catch (Exception e) {
            log.error("조별 경기 조회 중 오류 발생 - Group ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "조별 경기를 불러오는 중 오류가 발생했습니다.",
                            "message", e.getMessage()
                    ));
        }
    }
}