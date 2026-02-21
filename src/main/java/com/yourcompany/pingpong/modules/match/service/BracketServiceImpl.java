package com.yourcompany.pingpong.modules.match.service;

import com.yourcompany.pingpong.domain.*;
import com.yourcompany.pingpong.modules.match.repository.MatchGroupRepository;
import com.yourcompany.pingpong.modules.match.repository.MatchRepository;
import com.yourcompany.pingpong.modules.match.repository.ParticipationRepository;
import com.yourcompany.pingpong.modules.player.repository.PlayerRepository;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BracketServiceImpl implements BracketService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final ParticipationRepository participationRepository;
    private final MatchGroupRepository matchGroupRepository;

    /**
     * ⭐⭐⭐ 예선 경기 생성 (조 편성 없이도 가능) + 방어 로직 강화 ⭐⭐⭐
     * 조 편성이 있으면 조별 리그전, 없으면 전체 참가자로 토너먼트 생성
     */
    @Override
    @Transactional
    public int createPreliminaryMatches(Tournament tournament) {
        log.info("[BracketService] 예선 경기 생성 시작 - Tournament ID: {}", tournament.getId());

        // 1. 이미 예선 경기가 생성되었는지 확인
        List<Match> existingPreliminaries = matchRepository.findByTournamentIdAndRoundNameOrderByMatchNumberAsc(
                tournament.getId(), "예선"
        );

        // 또는 본선 경기가 있는지 확인
        List<Match> existingMainMatches = matchRepository.findByTournamentIdOrderByRoundNameAsc(tournament.getId())
                .stream()
                .filter(m -> !"예선".equals(m.getRoundName()))
                .toList();

        if (!existingPreliminaries.isEmpty() || !existingMainMatches.isEmpty()) {
            log.info("[BracketService] 경기가 이미 존재 for Tournament ID: {}. 예선: {}개, 본선: {}개",
                    tournament.getId(), existingPreliminaries.size(), existingMainMatches.size());
            return existingPreliminaries.size() + existingMainMatches.size();
        }

        // 2. 조 편성 확인
        List<MatchGroup> groups = matchGroupRepository.findByTournament(tournament);

        List<Match> allMatches = new ArrayList<>();
        int globalMatchNumber = 1;

        if (!groups.isEmpty()) {
            // ⭐ 조 편성이 있는 경우: 조별 리그전
            log.info("[BracketService] 조편성 발견: {}개 조. 조별 리그전 생성", groups.size());

            for (MatchGroup group : groups) {
                List<Participation> participants = participationRepository.findByMatchGroup(group);

                if (participants.size() < 2) {
                    log.warn("SERVICE WARN: Group {} has less than 2 participants. Skipping.", group.getName());
                    continue;
                }

                // ⭐⭐⭐ Player가 없는 참가자 필터링 및 경고 ⭐⭐⭐
                List<Player> players = participants.stream()
                        .filter(p -> {
                            if (p.getPlayer() == null) {
                                log.warn("⚠️ Participation ID {} has no Player! User: {}",
                                        p.getId(),
                                        p.getUser() != null ? p.getUser().getUsername() : "null");
                                return false;
                            }
                            return true;
                        })
                        .map(Participation::getPlayer)
                        .collect(Collectors.toList());

                if (players.size() < participants.size()) {
                    log.error("❌ GROUP {}: {} participants but only {} have Player entities!",
                            group.getName(), participants.size(), players.size());
                }

                if (players.size() < 2) {
                    log.error("❌ GROUP {}: Not enough players with Player entities (need 2+, have {})",
                            group.getName(), players.size());
                    throw new IllegalStateException(
                            String.format("조 '%s'에 Player가 연결된 참가자가 2명 미만입니다. " +
                                            "(전체 참가자: %d, Player 있음: %d)\n" +
                                            "관리자에게 문의하여 Player 정보를 확인해주세요.",
                                    group.getName(), participants.size(), players.size())
                    );
                }

                log.info("✅ GROUP {}: Creating matches for {} players", group.getName(), players.size());

                // 조 내 총당 경기 생성
                for (int i = 0; i < players.size(); i++) {
                    for (int j = i + 1; j < players.size(); j++) {
                        Match match = Match.builder()
                                .tournament(tournament)
                                .matchGroup(group)
                                .roundName("예선")
                                .matchNumber(globalMatchNumber++)
                                .player1(players.get(i))
                                .player2(players.get(j))
                                .status(MatchStatus.SCHEDULED)
                                .matchTime(calculateMatchTime(tournament.getStartDate(), globalMatchNumber))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        allMatches.add(match);
                    }
                }
            }
        } else {
            // ⭐ 조 편성이 없는 경우: 전체 참가자로 단일 토너먼트 생성
            log.info("SERVICE INFO: No groups found. Creating single elimination tournament.");

            List<Participation> allParticipants = participationRepository.findByTournament(tournament);

            if (allParticipants.size() < 2) {
                log.warn("SERVICE WARN: Less than 2 participants. Cannot create matches.");
                return 0;
            }

            // ⭐⭐⭐ Player가 없는 참가자 필터링 및 경고 ⭐⭐⭐
            List<Player> players = allParticipants.stream()
                    .filter(p -> {
                        if (p.getPlayer() == null) {
                            log.warn("⚠️ Participation ID {} has no Player! User: {}",
                                    p.getId(),
                                    p.getUser() != null ? p.getUser().getUsername() : "null");
                            return false;
                        }
                        return true;
                    })
                    .map(Participation::getPlayer)
                    .collect(Collectors.toList());

            if (players.size() < allParticipants.size()) {
                log.error("❌ {} participants but only {} have Player entities!",
                        allParticipants.size(), players.size());
            }

            if (players.size() < 2) {
                log.error("❌ Not enough players with Player entities (need 2+, have {})", players.size());
                throw new IllegalStateException(
                        String.format("Player가 연결된 참가자가 2명 미만입니다. " +
                                        "(전체 참가자: %d, Player 있음: %d)\n\n" +
                                        "일반 사용자가 참가 신청을 하면 Player가 자동으로 생성됩니다.\n" +
                                        "문제가 지속되면 관리자에게 문의해주세요.",
                                allParticipants.size(), players.size())
                );
            }

            log.info("SERVICE INFO: Found {} participants with Player entities. Creating tournament bracket.", players.size());

            // 참가자 수를 2의 거듭제곱으로 맞춤
            int targetSize = 2;
            while (targetSize < players.size()) {
                targetSize *= 2;
            }

            // BYE 추가
            while (players.size() < targetSize) {
                players.add(null);
            }

            Collections.shuffle(players);

            // 첫 라운드 경기 생성
            String roundName = getRoundName(targetSize);
            log.info("SERVICE INFO: Creating {} round with {} matches", roundName, targetSize / 2);

            for (int i = 0; i < players.size(); i += 2) {
                Match match = Match.builder()
                        .tournament(tournament)
                        .roundName(roundName)
                        .matchNumber(globalMatchNumber++)
                        .player1(players.get(i))
                        .player2(players.get(i + 1))
                        .status(MatchStatus.SCHEDULED)
                        .matchTime(calculateMatchTime(tournament.getStartDate(), globalMatchNumber))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                // BYE 자동 승리 처리
                if (match.getPlayer1() == null || match.getPlayer2() == null) {
                    match.setScore1(0);
                    match.setScore2(0);
                    match.setWinner(match.getPlayer1() != null ? match.getPlayer1().getName() :
                            (match.getPlayer2() != null ? match.getPlayer2().getName() : "BYE"));
                    match.setStatus(MatchStatus.COMPLETED);
                }

                allMatches.add(match);
            }
        }

        // 3. 모든 경기 저장
        if (allMatches.isEmpty()) {
            log.warn("SERVICE WARN: No matches created for Tournament ID: {}", tournament.getId());
            return 0;
        }

        List<Match> savedMatches = matchRepository.saveAll(allMatches);
        log.info("[BracketService] ✅ 경기 생성 완료 - Tournament ID: {}, 총 {}경기",
                tournament.getId(), savedMatches.size());

        return savedMatches.size();
    }

    /**
     * 경기 시간 계산 (시작 시간 + 경기 번호 * 20분)
     */
    private LocalDateTime calculateMatchTime(LocalDateTime startDate, int matchNumber) {
        return startDate.plusMinutes((matchNumber - 1) * 20L);
    }

    /**
     * ✅ 본선 브라켓 JSON 데이터 반환 (jQuery Bracket.js 형식)
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getBracketData(Long tournamentId) {
        log.info("SERVICE INFO: [getBracketData] Generating main bracket data for Tournament ID: {}", tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("대회를 찾을 수 없습니다. ID=" + tournamentId));

        List<Match> allMatches = matchRepository.findByTournamentIdOrderByRoundNameAsc(tournamentId);

        List<Match> allMainMatches = allMatches.stream()
                .filter(m -> !"예선".equals(m.getRoundName()))
                .collect(Collectors.toList());

        if (allMainMatches.isEmpty()) {
            log.warn("SERVICE WARN: [getBracketData] No main matches found for bracket generation in Tournament ID: {}. Returning empty data.", tournamentId);
            return Map.of("teams", Collections.emptyList(), "results", Collections.emptyList());
        }

        Map<String, List<Match>> matchesByRound = allMainMatches.stream()
                .collect(Collectors.groupingBy(Match::getRoundName));

        List<String> sortedRoundNames = matchesByRound.keySet().stream()
                .sorted(Comparator.comparing(this::getRoundOrder))
                .collect(Collectors.toList());

        if (sortedRoundNames.isEmpty()) {
            log.warn("SERVICE WARN: [getBracketData] No sorted main round names for Tournament ID: {}. Returning empty data.", tournamentId);
            return Map.of("teams", Collections.emptyList(), "results", Collections.emptyList());
        }

        List<List<String>> initialTeamsForBracket = new ArrayList<>();
        String firstRoundName = sortedRoundNames.get(0);
        List<Match> firstRoundMatches = matchesByRound.getOrDefault(firstRoundName, Collections.emptyList());
        firstRoundMatches.sort(Comparator.comparing(Match::getMatchNumber));

        if (firstRoundMatches.isEmpty()) {
            log.warn("SERVICE WARN: [getBracketData] No matches found for the first main round '{}' in Tournament ID: {}. Returning empty teams.", firstRoundName, tournamentId);
            return Map.of("teams", Collections.emptyList(), "results", Collections.emptyList());
        }

        for (Match match : firstRoundMatches) {
            List<String> matchParticipants = new ArrayList<>();
            String player1Name = (match.getPlayer1() != null) ? match.getPlayer1().getName() : "BYE";
            String player2Name = (match.getPlayer2() != null) ? match.getPlayer2().getName() : "BYE";

            log.debug("SERVICE DEBUG: [getBracketData] Match ID {} (Round: {}) - Player1 Name: '{}', Player2 Name: '{}'",
                    match.getId(), match.getRoundName(), player1Name, player2Name);

            matchParticipants.add(player1Name);
            matchParticipants.add(player2Name);
            initialTeamsForBracket.add(matchParticipants);
        }

        List<List<List<Integer>>> allRoundResultsForBracket = new ArrayList<>();

        for (String roundName : sortedRoundNames) {
            List<Match> roundMatches = matchesByRound.getOrDefault(roundName, Collections.emptyList());
            roundMatches.sort(Comparator.comparing(Match::getMatchNumber));

            List<List<Integer>> currentRoundResults = new ArrayList<>();
            for (Match match : roundMatches) {
                List<Integer> matchScores = new ArrayList<>();
                if (match.getStatus() == MatchStatus.COMPLETED) {
                    matchScores.add(match.getScore1() != null ? match.getScore1() : 0);
                    matchScores.add(match.getScore2() != null ? match.getScore2() : 0);
                } else {
                    matchScores.add(0);
                    matchScores.add(0);
                }
                currentRoundResults.add(matchScores);
            }
            allRoundResultsForBracket.add(currentRoundResults);
        }

        Map<String, Object> bracketData = new HashMap<>();
        bracketData.put("teams", initialTeamsForBracket);
        bracketData.put("results", allRoundResultsForBracket);

        log.info("SERVICE INFO: [getBracketData] Successfully generated bracket data for Tournament ID: {}", tournamentId);
        return bracketData;
    }

    /** ✅ 예선 결과 기반 본선 생성 */
    // 변경 후
    @Override
    @Transactional
    public List<Match> createFinalBracket(Tournament tournament) {
        int playersPerGroup = 2;  // 기본값 설정
        log.info("[BracketService] 본선 대진표 생성 시작 - Tournament ID: {}", tournament.getId());
        if (isFinalBracketGenerated(tournament.getId())) {
            log.warn("SERVICE WARN: [createFinalBracket] Final bracket already generated for Tournament ID: {}. Throwing exception.", tournament.getId());
            throw new IllegalStateException("이미 본선 대진표가 생성되었습니다. 중복 생성할 수 없습니다.");
        }

        List<Match> allMatches = matchRepository.findByTournamentIdOrderByRoundNameAsc(tournament.getId());
        List<Match> preliminaries = allMatches.stream()
                .filter(m -> "예선".equals(m.getRoundName()))
                .collect(Collectors.toList());

        boolean allPreliminariesDone = preliminaries.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED && m.getWinner() != null);

        if (!allPreliminariesDone) {
            log.warn("SERVICE WARN: [createFinalBracket] Not all preliminary matches are completed for Tournament ID: {}.", tournament.getId());
            throw new IllegalStateException("모든 예선 경기가 완료되지 않았거나 승자가 결정되지 않았습니다.");
        }

        Map<String, Map<Player, Integer>> groupWins = new HashMap<>();
        Map<String, Map<Player, Integer>> groupScoreDiffs = new HashMap<>();

        for (Match m : preliminaries) {
            if (m.getWinner() == null) continue;

            Player winnerPlayer = null;
            Player loserPlayer = null;

            if (m.getPlayer1() != null && m.getWinner().equals(m.getPlayer1().getName())) {
                winnerPlayer = m.getPlayer1();
                loserPlayer = m.getPlayer2();
            } else if (m.getPlayer2() != null && m.getWinner().equals(m.getPlayer2().getName())) {
                winnerPlayer = m.getPlayer2();
                loserPlayer = m.getPlayer1();
            } else {
                log.error("SERVICE ERROR: [createFinalBracket] Match ID {} winner mismatch.", m.getId());
                continue;
            }

            if (winnerPlayer == null || loserPlayer == null) continue;

            String groupName = (m.getMatchGroup() != null) ? m.getMatchGroup().getName() : "단일그룹";

            groupWins.computeIfAbsent(groupName, k -> new HashMap<>());
            groupWins.get(groupName).merge(winnerPlayer, 1, Integer::sum);

            groupScoreDiffs.computeIfAbsent(groupName, k -> new HashMap<>());
            groupScoreDiffs.get(groupName).putIfAbsent(winnerPlayer, 0);
            groupScoreDiffs.get(groupName).putIfAbsent(loserPlayer, 0);

            if (m.getScore1() != null && m.getScore2() != null) {
                int scoreDiff = m.getScore1() - m.getScore2();
                if (winnerPlayer.equals(m.getPlayer1())) {
                    groupScoreDiffs.get(groupName).merge(winnerPlayer, scoreDiff, Integer::sum);
                    groupScoreDiffs.get(groupName).merge(loserPlayer, -scoreDiff, Integer::sum);
                } else {
                    groupScoreDiffs.get(groupName).merge(winnerPlayer, -scoreDiff, Integer::sum);
                    groupScoreDiffs.get(groupName).merge(loserPlayer, scoreDiff, Integer::sum);
                }
            }
        }

        List<Player> qualifiedPlayers = groupWins.entrySet().stream()
                .flatMap(entry -> {
                    String groupName = entry.getKey();
                    Map<Player, Integer> winsMap = entry.getValue();
                    Map<Player, Integer> scoreDiffMap = groupScoreDiffs.getOrDefault(groupName, Collections.emptyMap());

                    return winsMap.entrySet().stream()
                            .sorted((e1, e2) -> {
                                int cmpWins = e2.getValue().compareTo(e1.getValue());
                                if (cmpWins != 0) return cmpWins;
                                return scoreDiffMap.getOrDefault(e2.getKey(), 0)
                                        .compareTo(scoreDiffMap.getOrDefault(e1.getKey(), 0));
                            })
                            .limit(playersPerGroup)
                            .map(Map.Entry::getKey);
                })
                .collect(Collectors.toList());

        log.info("SERVICE INFO: [createFinalBracket] Qualified Players: {}",
                qualifiedPlayers.stream().map(Player::getName).collect(Collectors.toList()));

        int initialPlayersCount = qualifiedPlayers.size();
        int targetSize = 2;
        while (targetSize < initialPlayersCount) {
            targetSize *= 2;
        }

        List<Player> playersForBracket = new ArrayList<>(qualifiedPlayers);
        if (initialPlayersCount < targetSize) {
            for (int i = 0; i < targetSize - initialPlayersCount; i++) {
                playersForBracket.add(null);
            }
        }
        Collections.shuffle(playersForBracket);

        if (playersForBracket.size() < 2 && qualifiedPlayers.size() > 0) {
            throw new IllegalStateException("본선 대진표 구성에 문제가 발생했습니다.");
        } else if (qualifiedPlayers.isEmpty()) {
            throw new IllegalStateException("본선 진출자가 없습니다.");
        }

        List<Match> finalBracketMatches = new ArrayList<>();
        int currentRoundPlayerCount = playersForBracket.size();
        String roundLabel = getRoundName(currentRoundPlayerCount);
        int matchNumber = 1;

        for (int i = 0; i < currentRoundPlayerCount / 2; i++) {
            Match match = Match.builder()
                    .tournament(tournament)
                    .roundName(roundLabel)
                    .matchNumber(matchNumber++)
                    .player1(playersForBracket.get(i * 2))
                    .player2(playersForBracket.get(i * 2 + 1))
                    .status(MatchStatus.SCHEDULED)
                    .matchTime(LocalDateTime.now().plusMinutes(i * 15 + 60))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            if (match.getPlayer1() == null || match.getPlayer2() == null) {
                match.setScore1(0);
                match.setScore2(0);
                match.setWinner(match.getPlayer1() != null ? match.getPlayer1().getName() :
                        (match.getPlayer2() != null ? match.getPlayer2().getName() : ""));
                match.setStatus(MatchStatus.COMPLETED);
            }
            finalBracketMatches.add(match);
        }

        log.info("SERVICE INFO: [createFinalBracket] Generated {} main bracket matches.", finalBracketMatches.size());
        return matchRepository.saveAll(finalBracketMatches);
    }

    /** ✅ 경기 결과 반영 시 다음 라운드 자동 생성 */
    @Transactional
    @Override
    public synchronized void autoAdvanceWinner(Match match) {
        log.info("SERVICE INFO: [autoAdvanceWinner] Match ID: {}", match.getId());

        Tournament tournament = match.getTournament();
        String currentRound = match.getRoundName();

        if ("결승".equals(currentRound) && match.getWinner() != null && match.getStatus() == MatchStatus.COMPLETED) {
            log.info("🏆 Tournament ID {} 결승 완료 — 우승자: {}", tournament.getId(), match.getWinner());
            return;
        }

        String nextRound = getNextRound(currentRound);
        if (nextRound == null) return;

        // ⭐ DB에서 최신 데이터 다시 조회 (동시성 문제 방지)
        List<Match> allTournamentMatches = matchRepository.findByTournamentIdOrderByRoundNameAsc(tournament.getId());

        // ⭐ 다음 라운드가 이미 생성되었는지 먼저 확인 (중복 방지)
        boolean nextRoundAlreadyExists = allTournamentMatches.stream()
                .anyMatch(m -> nextRound.equals(m.getRoundName()));

        if (nextRoundAlreadyExists) {
            log.info("Next round ({}) already exists. Skipping.", nextRound);
            return;
        }

        List<Match> currentRoundMatches = allTournamentMatches.stream()
                .filter(m -> currentRound.equals(m.getRoundName()))
                .collect(Collectors.toList());

        boolean allCurrentRoundMatchesCompleted = currentRoundMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED && m.getWinner() != null);

        if (!allCurrentRoundMatchesCompleted) {
            log.info("Current round ({}) not all completed yet.", currentRound);
            return;
        }

        List<Player> currentRoundWinners = currentRoundMatches.stream()
                .map(m -> {
                    if (m.getWinner() == null) return null;
                    if (m.getPlayer1() != null && m.getWinner().equals(m.getPlayer1().getName())) {
                        return m.getPlayer1();
                    } else if (m.getPlayer2() != null && m.getWinner().equals(m.getPlayer2().getName())) {
                        return m.getPlayer2();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (currentRoundWinners.isEmpty()) return;

        List<Match> nextRoundMatches = new ArrayList<>();
        int nextRoundMatchNumber = 1;
        List<Player> playersForNextRound = new ArrayList<>(currentRoundWinners);

        if (!nextRound.equals("결승") || playersForNextRound.size() > 1) {
            int nextRoundTargetSize = 2;
            while (nextRoundTargetSize < playersForNextRound.size()) {
                nextRoundTargetSize *= 2;
            }
            if (playersForNextRound.size() < nextRoundTargetSize) {
                for (int i = 0; i < nextRoundTargetSize - playersForNextRound.size(); i++) {
                    playersForNextRound.add(null);
                }
                Collections.shuffle(playersForNextRound);
            }

            for (int i = 0; i < playersForNextRound.size(); i += 2) {
                Match newMatch = Match.builder()
                        .tournament(tournament)
                        .roundName(nextRound)
                        .status(MatchStatus.SCHEDULED)
                        .matchTime(LocalDateTime.now().plusHours(2))
                        .matchNumber(nextRoundMatchNumber++)
                        .player1(playersForNextRound.get(i))
                        .player2((i + 1 < playersForNextRound.size()) ? playersForNextRound.get(i + 1) : null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                if (newMatch.getPlayer1() == null || newMatch.getPlayer2() == null) {
                    newMatch.setScore1(0);
                    newMatch.setScore2(0);
                    newMatch.setWinner(newMatch.getPlayer1() != null ? newMatch.getPlayer1().getName() :
                            (newMatch.getPlayer2() != null ? newMatch.getPlayer2().getName() : ""));
                    newMatch.setStatus(MatchStatus.COMPLETED);
                }
                nextRoundMatches.add(newMatch);
            }
        }

        if (!nextRoundMatches.isEmpty()) {
            matchRepository.saveAll(nextRoundMatches);
            log.info("✅ Next round ({}) generated: {} matches.", nextRound, nextRoundMatches.size());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isFinalBracketGenerated(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundNameAsc(tournamentId).stream()
                .anyMatch(match -> !"예선".equals(match.getRoundName()));
    }

    private String getRoundName(int size) {
        if (size <= 2) return "결승";
        if (size == 4) return "4강";
        if (size == 8) return "8강";
        if (size == 16) return "16강";
        if (size == 32) return "32강";
        return "본선";
    }

    private String getNextRound(String currentRound) {
        return switch (currentRound) {
            case "예선" -> "16강";
            case "32강" -> "16강";
            case "16강" -> "8강";
            case "8강" -> "4강";
            case "4강" -> "결승";
            case "결승" -> "우승";
            default -> null;
        };
    }

    private int getRoundOrder(String roundName) {
        return switch (roundName) {
            case "예선" -> 0;
            case "32강" -> 1;
            case "16강" -> 2;
            case "8강" -> 3;
            case "4강" -> 4;
            case "결승" -> 5;
            case "우승" -> 6;
            default -> 99;
        };
    }
}