# 탁구 대회 관리 시스템

안산 지역 탁구 동호회를 위한 대회 관리 웹 애플리케이션입니다.
실제 사용자 요구사항을 반영해 토너먼트 대진표 자동 생성부터 WebSocket 기반 실시간 결과 업데이트까지 구현했습니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5, Spring Security, Spring Data JPA |
| 데이터베이스 | MariaDB |
| 프론트엔드 | Thymeleaf, Bootstrap, WebSocket (STOMP) |
| 빌드 | Gradle 8 |

## 주요 기능

**대회 관리**
- 토너먼트 / 리그전 생성 및 참가자 등록
- 대진표 자동 생성 (시드 배정 지원)
- WebSocket 기반 실시간 경기 결과 입력 및 브로드캐스트

**클럽 / 선수**
- 클럽 가입, 선수 프로필 관리
- 참가 이력 및 전적 통계 조회

**커뮤니티**
- 게시판(카테고리별), 댓글, 검색
- 알림 시스템 (경기 결과, 대회 공지)

**보안**
- Spring Security 폼 로그인
- 로그인 시도 횟수 제한 (LoginAttemptService — Brute-Force 방어)
- 관리자 어드민 패널 (권한 분리)

## 프로젝트 구조

```
src/main/java/com/yourcompany/pingpong/
├── modules/
│   ├── tournament/   # 대회 생성·대진표 알고리즘
│   ├── match/        # 경기 결과 처리, WebSocket 이벤트
│   ├── player/       # 선수 프로필, 전적
│   ├── club/         # 클럽 관리
│   ├── post/         # 게시판
│   ├── notification/ # 알림
│   ├── calendar/     # 대회 일정
│   └── admin/        # 관리자 패널
├── common/
│   ├── security/     # 인증, Brute-Force 방어
│   └── exception/    # GlobalExceptionHandler
└── config/           # Security, WebSocket 설정
```

## 실행 방법

```bash
# 1. MariaDB 실행 및 데이터베이스 생성
# CREATE DATABASE pingpong;

# 2. application.yml DB 설정 확인 후 실행
./gradlew bootRun
```

접속: `http://localhost:8080`

관리자 계정은 애플리케이션 최초 실행 시 `DataInitializer`가 자동 생성합니다.

## 설계 포인트

- **WebSocket 실시간 업데이트**: STOMP over WebSocket으로 경기 결과 즉시 브로드캐스트. 폴링 없이 관전자에게 실시간 스코어 전달
- **Brute-Force 방어**: `LoginAttemptService`에서 IP별 실패 횟수를 추적, n회 초과 시 계정 잠금
- **토너먼트 대진표**: 참가자 수에 따라 바이(Bye) 처리 포함한 싱글 엘리미네이션 트리 자동 구성
