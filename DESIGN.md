---
name: 안산시 탁구 협회
description: 체육관 벽의 종이 대진표를 매직으로 채워 가는 생활체육 탁구 대회 운영 화면
colors:
  wall: "#e6eae6"
  sheet: "#ffffff"
  sheet-2: "#f6f8f6"
  rule: "#c7cec8"
  rule-soft: "#e1e6e2"
  ink: "#15181d"
  ink-2: "#3b4350"
  ink-3: "#59626e"
  marker-red: "#d42a1f"
  marker-red-ink: "#b51f15"
  marker-red-wash: "#fde9e7"
  marker-blue: "#1d4ccc"
  marker-blue-ink: "#173fa9"
  marker-blue-wash: "#e8eefc"
  marker-green: "#0c6f3f"
  marker-green-wash: "#e3f4ea"
  marker-amber: "#995700"
  marker-amber-wash: "#fdf1dc"
  highlighter: "#ffe45c"
  highlighter-soft: "#fff4b0"
  band: "#15181d"
  band-ink: "#f4f6f4"
  band-dim: "#a3acb6"
typography:
  display:
    fontFamily: "Pretendard Variable, Pretendard, Apple SD Gothic Neo, Malgun Gothic, sans-serif"
    fontSize: "clamp(2rem, 3.8vw, 2.9rem)"
    fontWeight: 880
    lineHeight: 1.12
    letterSpacing: "-0.045em"
  headline:
    fontFamily: "Pretendard Variable, Pretendard, sans-serif"
    fontSize: "1.9rem"
    fontWeight: 860
    lineHeight: 1.2
    letterSpacing: "-0.035em"
  title:
    fontFamily: "Pretendard Variable, Pretendard, sans-serif"
    fontSize: "1.08rem"
    fontWeight: 820
    lineHeight: 1.3
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Pretendard Variable, Pretendard, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "-0.008em"
    fontFeature: "\"tnum\" 1"
  label:
    fontFamily: "Pretendard Variable, Pretendard, sans-serif"
    fontSize: "0.82rem"
    fontWeight: 620
    lineHeight: 1.4
  board-numeral:
    fontFamily: "Pretendard Variable, Pretendard, sans-serif"
    fontSize: "2.4rem"
    fontWeight: 900
    lineHeight: 1
    letterSpacing: "-0.05em"
rounded:
  sheet: "3px"
  match: "4px"
  control: "8px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.marker-blue}"
    textColor: "{colors.sheet}"
    rounded: "{rounded.control}"
    padding: "8px 16px"
  button-primary-hover:
    backgroundColor: "{colors.marker-blue-ink}"
  button-ink:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.sheet}"
    rounded: "{rounded.control}"
  button-soft:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
  button-accent:
    backgroundColor: "{colors.marker-red}"
    textColor: "{colors.sheet}"
    rounded: "{rounded.control}"
  sheet:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.sheet}"
    padding: "16px 18px"
  match-box:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    rounded: "{rounded.match}"
  input:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    height: "44px"
  status-live:
    backgroundColor: "{colors.marker-red}"
    textColor: "{colors.sheet}"
    rounded: "5px"
---

# Design System: 안산시 탁구 협회

## Overview

**Creative North Star: "The Bracket Wall"**

대회 날 체육관 벽에는 전지에 그린 대진표가 붙고, 운영진이 매직으로 이긴 사람의 선을 빨갛게 이어 간다. 이 시스템은 그 장면을 화면 언어로 옮긴다. 바탕은 페인트칠한 체육관 벽(옅은 회녹색), 그 위에 흰 종이가 붙고, 글자는 검정 매직, 승자 경로와 진행 중은 빨간 매직, 동작은 파란 매직, 확정은 초록 매직, 그리고 "나"는 형광펜이다.

운영 화면(Operate) 위주의 시스템이라 배치와 컨트롤은 사람들이 이미 아는 표준을 따른다. 세계관은 색, 굵은 한글 타이포, 밀도, 그리고 하나의 시그니처(빨간 승자 선)로만 들어온다. 밝은 형광등 체육관에서 5~15m 떨어진 TV로도 읽혀야 하므로 기본은 밝은 종이 위 검정 잉크이며, 어두운 테마는 저녁 열람용으로만 둔다.

**Key Characteristics:**
- 벽(ground) 위에 종이(sheet): 검정 2px 괘선으로 머리를 나누는 종이 패널
- 매직 색마다 고정된 역할, 장식용 색은 없다
- 승자 경로는 빨간 선으로 다음 라운드까지 이어진다
- 로그인한 회원의 엔트리는 형광펜으로 칠해진다
- 이름이 주인공: 굵은 이름 아래 작은 소속(이름표)

## Colors

벽과 종이의 무채색 위에 매직 다섯 자루(검정, 빨강, 파랑, 초록, 형광 노랑)를 역할별로 쓴다.

### Primary
- **Marker Red** (marker-red): 승자 선, 진행 중 경기, 진행률 막대, 우승자 동그라미, 결승 라운드 이름. 이 색이 보이면 "이겼다/지금 하고 있다"는 뜻이다.
- **Marker Blue** (marker-blue): 버튼과 링크, 포커스 링. 누르면 무언가 일어나는 곳.

### Secondary
- **Marker Green** (marker-green): 확정, 체크 완료, 끝난 단계.
- **Highlighter** (highlighter): 로그인한 회원의 엔트리, 텍스트 선택, 방금 바뀐 현황판 줄.

### Neutral
- **Gym Wall** (wall): 페이지 바탕.
- **Paper** (sheet / sheet-2): 패널, 경기 칸, 입력창.
- **Pencil Rule** (rule / rule-soft): 테두리와 구분선.
- **Marker Black** (ink / ink-2 / ink-3): 본문, 보조 글, 메타 글. ink-3 도 벽 위 4.5:1 이상.
- **Header Band** (band): 상단 내비게이션 띠, 현황판 머리.

### Named Rules
**The Marker Jobs Rule.** 매직 색은 정해진 일만 한다. 빨강을 강조나 장식에 쓰지 않는다. 빨강이 보이면 반드시 승자 또는 진행 중이다.

**The Highlighter Is Me Rule.** 형광 노랑은 "나"와 "방금 바뀐 것"에만 쓴다. 섹션 배경이나 배지 장식에 쓰지 않는다.

## Typography

**Display / Body Font:** Pretendard Variable (자체 호스팅, Apple SD Gothic Neo, Malgun Gothic 대체)

**Character:** 한 가족으로 모든 역할을 맡는다. 제목과 이름은 매우 굵게(820~900)와 좁은 자간으로 매직 글씨의 무게를 내고, 숫자는 모두 고정폭(tabular) 숫자다.

### Hierarchy
- **Display** (880, clamp(2rem, 3.8vw, 2.9rem), 1.12): 홈 첫 화면 제목만.
- **Headline** (860, 1.9rem, 1.2): 페이지 제목.
- **Title** (820, 1.08rem, 1.3): 패널 머리, 대회 종이 제목.
- **Body** (400, 1rem, 1.55): 본문. 게시글은 72ch 이하.
- **Label** (620, 0.82rem): 소속, 라운드, 메타 정보. 대문자 변환이나 넓은 자간 없음.
- **Board Numeral** (900, 2.4rem): 현황판 테이블 번호.

### Named Rules
**The Name Plate Rule.** 엔트리는 어디서나 같은 이름표로 나온다: 굵은 이름 한 줄, 그 아래 작은 소속 한 줄.

**The No Kicker Rule.** 제목 위에 작은 라벨을 붙이지 않는다. 제목이 스스로 말한다.

## Layout

Bootstrap 12단 그리드 위에 콘텐츠 최대 폭 컨테이너. 운영 화면은 밀도를 허용한다(표, 다열 테이블 카드). 상세 화면은 왼쪽 작업 영역 + 오른쪽 정보 열이며 넓은 화면에서 오른쪽 열은 스크롤을 따라온다. 대진표는 가로 스크롤 영역에 라운드별 열(232px, 간격 30px)로 그리고, 각 열의 칸 높이를 같게 나눠 선이 짝의 가운데에서 만난다. 576px 이하에서 테이블 현황은 2열, 현황판은 1200px 이하에서 한 열로 접힌다. 간격 단위는 4/8/16/24/32px.

## Elevation & Depth

종이가 벽에 붙어 있는 정도의 얕은 그림자 하나만 쓴다. 깊이는 대부분 검정 괘선과 종이/벽의 명도 차로 표현한다.

### Shadow Vocabulary
- **Pinned Sheet** (`box-shadow: 0 1px 0 rgba(21,24,29,.06), 0 3px 10px -4px rgba(21,24,29,.16)`): 패널과 경기 칸.
- **Lifted Note** (`box-shadow: 0 10px 30px -10px rgba(21,24,29,.35)`): 알림 메시지, 드롭다운.

### Named Rules
**The Paper Not Glass Rule.** 유리, 블러, 그라데이션 바탕을 쓰지 않는다. 깊이는 종이 한 장 두께까지만.

## Shapes

종이는 거의 각진 모서리(3px), 경기 칸 4px, 버튼과 입력 8px, 필터 칩만 완전한 알약형. 모서리 반경이 위계를 말한다: 종이일수록 각지고 손이 닿는 컨트롤일수록 둥글다. 진행 중 강조에 쓰는 굵은 색 윗변은 모서리 반경 0에서만 쓴다.

## Components

### Buttons
- **Shape:** 손에 쥐는 둥근 모서리 (8px)
- **Primary (파란 매직):** 저장, 시작, 확정처럼 주 동작.
- **Ink (검정):** 페이지의 첫 행동(대회 보기, 검색).
- **Accent (빨간 매직):** 대진 생성처럼 대회 흐름을 바꾸는 동작.
- **Soft:** 흰 종이 버튼, 회색 테두리. 보조 동작.
- **Danger outline:** 기권, 삭제, 결과 취소. 테두리만 빨갛고 다른 버튼과 떨어뜨려 둔다.
- **Hover / Focus:** 150ms 색 전환, 2.5px 파란 포커스 링.

### Chips
- **Filter:** 알약형 흰 칩, 선택되면 검정 바탕 흰 글자.
- **Status:** 작은 사각 라벨. 진행중=빨강 바탕 흰 글자(깜빡이는 점), 확정=초록, 신청/마감=황색, 기권=점선 테두리 회색.

### Cards / Containers
- **Corner Style:** 3px
- **Background:** 흰 종이
- **Header:** 제목 아래 검정 2px 괘선
- **Border:** 1px 연필 회색
- **Internal Padding:** 16~18px

### Inputs / Fields
- **Style:** 흰 바탕, 1px 회색 테두리, 8px 모서리, 최소 높이 44px
- **Focus:** 파란 테두리 + 연한 파란 3px 링
- **Error:** 빨간 테두리와 필드 바로 아래 빨간 문구

### Navigation
- **Style:** 검정 띠 위 회색 글자. 현재 메뉴는 흰 글자 + 아래 형광펜 3px 줄. 모바일에서는 접히고 현재 메뉴 글자가 형광색.

### Bracket (Signature Component)
경기 칸은 검정 1.5px 테두리 종이 조각. 승자 줄은 굵은 이름과 빨간 점수, 진 쪽은 회색. 칸에서 나가는 선은 끝난 경기면 빨간색으로 다음 칸까지 이어지고, 결승에 가까울수록 선이 굵어진다(2 → 2.5 → 3px). BYE 칸은 점선, 기권한 이름은 빨간 취소선. 우승자 이름은 빨간 매직으로 손으로 그린 듯한 동그라미 안에 넣는다.

### Live Board (Signature Component)
TV 현황판은 시간표: 열은 고정(테이블 · 라운드 · 선수 · 선수 · 경과), 값만 바뀐다. 경기 중인 테이블 번호 칸은 빨간 바탕 흰 숫자. 바뀐 줄은 형광펜으로 한 번(2.4초) 칠해진다.

## Do's and Don'ts

### Do:
- **Do** 승자와 진행 중에만 빨간 매직을 쓴다.
- **Do** 엔트리는 항상 이름표(굵은 이름 + 작은 소속)로 보여 준다.
- **Do** 상태를 색과 함께 글자나 모양으로도 구분한다(기권 취소선, BYE 점선, 진행 점).
- **Do** 운영 화면의 컨트롤은 표준 버튼, 셀렉트, 표, 탭을 쓴다.
- **Do** 위험한 동작은 테두리 버튼으로, 다른 동작과 떨어뜨려 둔다.

### Don't:
- **Don't** 제목 위 작은 라벨, 이모지 아이콘, 그라데이션 글자를 쓰지 않는다.
- **Don't** 카드나 목록에 두꺼운 색 왼쪽 띠(side stripe)를 두지 않는다.
- **Don't** 모눈종이나 격자 배경을 깔지 않는다.
- **Don't** "A · B · C" 처럼 가운뎃점으로 메타 정보를 잇지 않는다. 쉼표나 별도 요소로 나눈다.
- **Don't** 형광 노랑을 장식 배경으로 쓰지 않는다.
