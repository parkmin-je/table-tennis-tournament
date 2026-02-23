package com.yourcompany.pingpong.common;

import com.yourcompany.pingpong.domain.*;
import com.yourcompany.pingpong.modules.club.repository.ClubRepository;
import com.yourcompany.pingpong.modules.player.repository.PlayerRepository;
import com.yourcompany.pingpong.modules.tournament.repository.TournamentRepository;
import com.yourcompany.pingpong.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개발/테스트용 초기 데이터 세팅
 * - DB에 admin 계정이 없을 때만 실행 (idempotent)
 * - admin / Admin1234! 로 로그인 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final TournamentRepository tournamentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 이미 admin 계정이 있으면 스킵
        if (userRepository.existsByUsername("admin")) {
            log.info("[DataInitializer] 이미 초기 데이터가 존재합니다. 스킵.");
            return;
        }

        log.info("[DataInitializer] 초기 데이터 생성 시작...");

        // ────────────────────────────────────────────
        // 1. 어드민 계정 생성
        // ────────────────────────────────────────────
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin1234!"))
                .email("admin@ansantt.kr")
                .name("관리자")
                .role("ADMIN")
                .build();
        userRepository.save(admin);
        log.info("[DataInitializer] 어드민 계정 생성 완료: admin / Admin1234!");

        // ────────────────────────────────────────────
        // 2. 일반 유저 6명 생성
        // ────────────────────────────────────────────
        String[][] userData = {
                {"user1", "박민제", "user1@ansantt.kr"},
                {"user2", "김철수", "user2@ansantt.kr"},
                {"user3", "이영희", "user3@ansantt.kr"},
                {"user4", "최강수", "user4@ansantt.kr"},
                {"user5", "정도전", "user5@ansantt.kr"},
                {"user6", "한빛나", "user6@ansantt.kr"},
        };

        List<User> users = new java.util.ArrayList<>();
        for (String[] ud : userData) {
            User u = User.builder()
                    .username(ud[0])
                    .password(passwordEncoder.encode("User1234!"))
                    .email(ud[2])
                    .name(ud[1])
                    .role("USER")
                    .build();
            users.add(userRepository.save(u));
        }
        log.info("[DataInitializer] 일반 유저 6명 생성 완료 (user1~user6 / User1234!)");

        // ────────────────────────────────────────────
        // 3. 클럽 생성
        // ────────────────────────────────────────────
        Club club = new Club();
        club.setName("안산시 탁구 클럽");
        club.setRegion("경기도 안산시");
        Club savedClub = clubRepository.save(club);
        log.info("[DataInitializer] 클럽 생성 완료: {}", savedClub.getName());

        // ────────────────────────────────────────────
        // 4. 선수 등록 (유저 연결)
        // ────────────────────────────────────────────
        String[] positions = {"공격형", "수비형", "올라운더", "공격형", "수비형", "올라운더"};
        String[] genders   = {"남", "남", "여", "남", "남", "여"};
        int[]    ages      = {28, 35, 29, 42, 31, 24};

        List<Player> players = new java.util.ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            Player p = Player.builder()
                    .name(users.get(i).getName())
                    .age(ages[i])
                    .gender(genders[i])
                    .ranking(1000)
                    .position(positions[i])
                    .user(users.get(i))
                    .club(savedClub)
                    .build();
            players.add(playerRepository.save(p));
        }
        log.info("[DataInitializer] 선수 6명 등록 완료");

        // ────────────────────────────────────────────
        // 5. 테스트 대회 생성 (접수중 상태)
        // ────────────────────────────────────────────
        Tournament tournament = Tournament.builder()
                .title("안산시 탁구 협회 춘계대회 2026")
                .name("춘계대회")
                .description("안산시 탁구 협회 공식 춘계 단식 대회입니다. 남녀 구분 없이 참가 가능합니다.")
                .startDate(LocalDateTime.of(2026, 3, 15, 9, 0))
                .endDate(LocalDateTime.of(2026, 3, 16, 18, 0))
                .status(TournamentStatus.RECRUITING)
                .type(TournamentType.SINGLES)
                .creator(admin)
                .build();
        Tournament savedTournament = tournamentRepository.save(tournament);
        log.info("[DataInitializer] 테스트 대회 생성 완료: {}", savedTournament.getTitle());

        log.info("[DataInitializer] ✅ 초기 데이터 생성 완료!");
        log.info("[DataInitializer] 🔑 어드민 로그인: admin / Admin1234!");
        log.info("[DataInitializer] 👤 일반 유저: user1~user6 / User1234!");
    }
}
