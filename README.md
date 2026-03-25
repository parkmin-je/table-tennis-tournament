# 탁구 대회 관리 시스템

안산 지역 탁구 동호회를 위한 대회 관리 웹 애플리케이션입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5 · Spring Security · Spring Data JPA |
| 데이터베이스 | MariaDB |
| 프론트엔드 | Thymeleaf · Bootstrap · WebSocket |
| 빌드 | Gradle 8 |

## 주요 기능

- 대회 생성 및 참가자 등록
- 토너먼트 대진표 자동 생성
- WebSocket 기반 실시간 경기 결과 업데이트
- 참가자 점수 · 순위 조회
- Spring Security 기반 관리자 인증

## 실행 방법

```bash
# MariaDB 실행 후
./gradlew bootRun
```

접속: `http://localhost:8080`
