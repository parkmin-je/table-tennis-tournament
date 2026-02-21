/**
 * /js/main-bracket.js
 * 안정적 렌더링 버전 (좌12 / 우12 같은 큰 초기 매치도 한눈에 보이도록 자동 스케일)
 *
 * 요구: 서버 컨트롤러 URL -> /tournament/mainBracket/{id}
 *
 * 붙여넣기 후 바로 사용 (경로 임의 변경 없음)
 */

console.log("main-bracket.js loaded");

let BRACKET_TOTAL_ROUNDS = 0;
let BRACKET_FINAL_INDEX = 0;

$(function () {
    if (typeof TOURNAMENT_ID === "undefined") {
        console.error("TOURNAMENT_ID not set");
        $("#bracket").html('<div class="loading">TOURNAMENT_ID 없음</div>');
        return;
    }
    loadBracket();
    $(window).on('resize', () => { /* 창 크기 바뀌면 재조정 */ setTimeout(adjustSvgAndScale, 120); });
});

function loadBracket() {
    $.ajax({
        url: `/tournament/mainBracket/${TOURNAMENT_ID}`,
        method: "GET",
        success: function (data) {
            renderBracket(data);
        },
        error: function (xhr) {
            console.error("Failed to load bracket:", xhr);
            $("#bracket").html('<div class="loading">대진표 로드 실패</div>');
        }
    });
}

/**
 * renderBracket
 * - teams: List<List<Map>> (각 라운드별 선수 객체 배열: [p1, p2, p3, p4, ...])
 * - results: List<List<List<Integer>>> (라운드별 [ [s1,s2], [s1,s2], ... ])
 */
function renderBracket(data) {
    const teams = data.teams || [];
    const results = data.results || [];

    const container = $("#bracket");
    container.empty();

    if (!teams.length) {
        container.html('<div class="loading">본선 대진표가 없습니다.</div>');
        return;
    }

    BRACKET_TOTAL_ROUNDS = teams.length;
    BRACKET_FINAL_INDEX = BRACKET_TOTAL_ROUNDS - 1;

    const processedRounds = teams.map((roundTeams, idx) => buildMatchesForRound(roundTeams, results[idx]));

    const leftSide = $('<div class="side left"></div>');
    const rightSide = $('<div class="side right"></div>');

    processedRounds.forEach((round, idx) => {
        if (idx === BRACKET_FINAL_INDEX) {
            return;
        }
        const { leftMatches, rightMatches } = splitMatches(round.matches);

        if (leftMatches.length) {
            leftSide.append(makeRoundColumn(leftMatches, idx, BRACKET_TOTAL_ROUNDS, 'left'));
        }
        if (rightMatches.length) {
            rightSide.append(makeRoundColumn(rightMatches, idx, BRACKET_TOTAL_ROUNDS, 'right'));
        }
    });

    const finalColumn = makeRoundColumn(processedRounds[BRACKET_FINAL_INDEX].matches, BRACKET_FINAL_INDEX, BRACKET_TOTAL_ROUNDS, 'final');

    const finalWrap = $('<div class="final-round"></div>').append(finalColumn);

    container.append(leftSide);
    container.append(finalWrap);
    container.append(rightSide);

    setTimeout(() => {
        drawLines();
        adjustSvgAndScale();
    }, 80);
}

/* makeRoundColumn
   - roundTeams: 배열(선수 객체 alternating: [p1,p2,p3,p4,...])
   - roundResults: 배열([[s1,s2], [s1,s2], ...])
*/
function makeRoundColumn(roundMatches = [], roundIndex, totalRounds, branch) {
    if (!roundMatches.length) return $('<div></div>');

    const round = $(`<div class="round" data-round="${roundIndex}" data-branch="${branch}"></div>`);
    round.append(`<div class="round-header">${getRoundName(roundIndex, totalRounds)}</div>`);

    const matchesWrap = $('<div class="round-matches"></div>');

    roundMatches.forEach(matchData => {
        const p1 = matchData.p1 || { name: "BYE" };
        const p2 = matchData.p2 || { name: "BYE" };
        const score = matchData.score || [null, null];

        const match = $('<div class="match"></div>');
        const row1 = $(`<div class="match-row ${score && score[0] > score[1] ? 'winner' : ''}"></div>`);
        const row2 = $(`<div class="match-row ${score && score[1] > score[0] ? 'winner' : ''}"></div>`);

        row1.append(`<div class="player-name">${escapeHtml(p1.name || 'BYE')}</div>`);
        row1.append(`<div class="player-score">${(score && score[0] !== null ? score[0] : '')}</div>`);
        row2.append(`<div class="player-name">${escapeHtml(p2.name || 'BYE')}</div>`);
        row2.append(`<div class="player-score">${(score && score[1] !== null ? score[1] : '')}</div>`);

        match.append(row1).append(row2);
        matchesWrap.append(match);
    });

    round.append(matchesWrap);
    return round;
}

/* drawLines
   - 라운드간 매치 연결선을 그림
   - 왼쪽 라운드는 오른쪽(다음 라운드)으로, 오른쪽 라운드는 왼쪽(다음 라운드)으로 연결
*/
function drawLines() {
    const svg = $("#svg");
    const stage = $(".bracket-stage");
    svg.empty();

    const stageOffset = stage.offset();
    const rounds = $(".round");

    rounds.each(function () {
        const round = $(this);
        const roundIndex = parseInt(round.attr("data-round"), 10);
        const branch = round.attr("data-branch");

        if (!branch || branch === 'final') return;

        let targetRound;
        if (roundIndex + 1 === BRACKET_FINAL_INDEX) {
            targetRound = $(`.round[data-round="${BRACKET_FINAL_INDEX}"][data-branch="final"]`);
        } else {
            targetRound = $(`.round[data-round="${roundIndex + 1}"][data-branch="${branch}"]`);
        }

        if (!targetRound || !targetRound.length) return;

        const matches = round.find('.match');
        matches.each(function (mi) {
            const match = $(this);
            const nextMatch = targetRound.find('.match').eq(Math.floor(mi / 2));
            if (!nextMatch || !nextMatch.length) return;

            const a = match.offset();
            const b = nextMatch.offset();

            const matchWidth = match.outerWidth();
            const nextMatchWidth = nextMatch.outerWidth();

            const y1 = a.top - stageOffset.top + match.outerHeight() / 2;
            const y2 = b.top - stageOffset.top + nextMatch.outerHeight() / 2;

            const isRightBranch = branch === 'right';

            const x1 = isRightBranch
                ? a.left - stageOffset.left
                : a.left - stageOffset.left + matchWidth;

            const x2 = isRightBranch
                ? b.left - stageOffset.left + nextMatchWidth
                : b.left - stageOffset.left;

            const midX = isRightBranch ? (x1 + x2) / 2 : (x1 + x2) / 2;

            const d = `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2} L ${x2} ${y2}`;

            const hasWinner = match.find('.match-row.winner').length > 0;
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", d);
            path.setAttribute("fill", "none");
            path.setAttribute("stroke", hasWinner ? "#c41e3a" : "#999");
            path.setAttribute("stroke-width", hasWinner ? "2" : "1.3");
            svg[0].appendChild(path);
        });
    });
}

/* adjustSvgAndScale
   - SVG 크기를 wrapper 기준으로 세팅
   - 좌/우 중 큰 매치 수에 따라 필요하면 축소(scale)하여 세로 스크롤 없이 한 화면에 보기
*/
function adjustSvgAndScale() {
    const wrapper = $('.bracket-wrapper');
    const stage = $('.bracket-stage');
    const bracket = $('#bracket');
    const svg = $('#svg');

    const width = bracket.outerWidth(true);
    const height = Math.max(bracket.outerHeight(true), wrapper.innerHeight());

    stage.css({ width, height, transform: 'scale(1)' });

    svg.attr('width', width);
    svg.attr('height', height);
    svg.attr('viewBox', `0 0 ${width} ${height}`);

    svg.empty();
    drawLines();
}

function buildMatchesForRound(roundTeams = [], roundResults = []) {
    const matches = [];
    const sanitizedTeams = Array.isArray(roundTeams) ? roundTeams : [];
    const sanitizedResults = Array.isArray(roundResults) ? roundResults : [];

    const matchCount = Math.ceil(sanitizedTeams.length / 2);
    for (let m = 0; m < matchCount; m++) {
        matches.push({
            p1: sanitizedTeams[m * 2] || { name: "BYE" },
            p2: sanitizedTeams[m * 2 + 1] || { name: "BYE" },
            score: sanitizedResults[m] || [null, null]
        });
    }
    return { matches };
}

function splitMatches(matches = []) {
    const half = Math.ceil(matches.length / 2);
    return {
        leftMatches: matches.slice(0, half),
        rightMatches: matches.slice(matches.length - half)
    };
}

/* helpers */
function getRoundName(roundIndex, totalRounds) {
    // Use common labels if possible; fallback to roundIndex text
    const labels = ["16강", "8강", "4강", "준결승", "결승"];
    // try to map from end: last index -> 결승
    const positionFromEnd = totalRounds - 1 - roundIndex;
    if (positionFromEnd >= 0 && positionFromEnd < labels.length) {
        return labels[positionFromEnd];
    }
    // else fallback
    return `라운드 ${roundIndex + 1}`;
}

function escapeHtml(text) {
    if (typeof text !== 'string') return text;
    return text.replace(/[&<>"'`=\/]/g, function (s) {
        return ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;',
            "'": '&#39;', '/': '&#x2F;', '`': '&#x60;', '=': '&#x3D;'
        })[s];
    });
}
