// src/main/resources/static/js/group-bracket.js

// 대진표 데이터 렌더링 함수
function renderGroupBrackets(tournamentId, bracketContainerSelector) {
    const bracketContainer = $(bracketContainerSelector);

    // 초기 로딩 스피너 표시
    bracketContainer.html(`
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-3 text-muted">대진표를 불러오는 중입니다...</p>
        </div>
    `);

    console.log('FRONTEND DEBUG (group-bracket.js): Initializing bracket script for Tournament ID:', tournamentId);

    // 함수 인자 유효성 검사
    if (!bracketContainerSelector || !$(bracketContainerSelector).length) {
        console.error("FRONTEND FATAL ERROR (group-bracket.js): Invalid bracket container selector provided:", bracketContainerSelector);
        bracketContainer.html(`
            <div class="alert alert-danger text-center py-4 rounded-3 shadow-sm">
                <i class="bi bi-x-circle-fill me-2"></i> 대진표 컨테이너를 찾을 수 없습니다.<br>개발자 도구 콘솔을 확인하세요!
            </div>
        `);
        return;
    }

    // 백엔드에서 대진표 데이터를 JSON 형태로 가져오는 AJAX 요청
    $.ajax({
        url: '/match/groupBracketData/' + tournamentId,
        method: 'GET',
        dataType: 'json',
        success: function(response) {
            console.log('FRONTEND DEBUG (group-bracket.js - AJAX Success): Received response:', response);

            // 응답 데이터 구조 유효성 검사
            if (!response || !response.matchGroups || !Array.isArray(response.matchGroups) ||
                !response.groupParticipants || typeof response.groupParticipants !== 'object' ||
                !response.groupMatches || typeof response.groupMatches !== 'object' ||
                !response.groupRankings || typeof response.groupRankings !== 'object') {

                console.error('FRONTEND FATAL ERROR (group-bracket.js - AJAX Error): Invalid or incomplete response structure received from API:', response);
                bracketContainer.html(`
                    <div class="alert alert-danger text-center py-4 rounded-3 shadow-sm">
                        <i class="bi bi-x-circle-fill me-2"></i> 대진표 데이터를 불러왔지만, 서버 응답 형식이 올바르지 않습니다.<br>백엔드 API 또는 개발자 도구 콘솔을 확인하세요!
                    </div>
                `);
                return;
            }

            console.log('FRONTEND DEBUG (group-bracket.js): Response contains all required group bracket data.');

            // ⭐⭐⭐ 조가 없으면 조 없는 예선 경기 조회 ⭐⭐⭐
            if (response.matchGroups.length === 0) {
                console.warn('FRONTEND WARNING (group-bracket.js): No match groups found for tournament ID:', tournamentId);
                console.log('FRONTEND INFO: Attempting to load preliminary matches without groups...');
                loadPreliminaryMatchesNoGroup(tournamentId, bracketContainer);
                return;
            }

            // 기존 조별 렌더링 로직 (변경 없음)
            renderGroupBasedBrackets(response, bracketContainer);

        },
        error: function(xhr, status, error) {
            console.error('FRONTEND FATAL ERROR (group-bracket.js - AJAX Failed): Error fetching bracket data. Status:', status, 'Error:', error, 'XHR:', xhr);
            console.error('FRONTEND FATAL ERROR (group-bracket.js - AJAX Failed): Response Text:', xhr.responseText);

            let errorMessage = "대진표 데이터를 불러오는 중 치명적인 오류가 발생했습니다.<br>";
            if (xhr.status === 404) {
                errorMessage += "API 엔드포인트를 찾을 수 없습니다. 백엔드 URL을 확인하세요.";
            } else if (xhr.status === 500) {
                errorMessage += "서버 내부 오류가 발생했습니다. 백엔드 로그를 확인하세요.";
            } else if (status === 'parsererror') {
                errorMessage += "서버 응답 데이터 형식이 올바르지 않습니다 (JSON 파싱 오류). 백엔드 API 응답을 확인하세요.";
            } else {
                errorMessage += `상태 코드: ${xhr.status || '알 수 없음'}, 오류 메시지: ${error || '알 수 없음'}`;
            }
            bracketContainer.html(`
                <div class="alert alert-danger text-center py-4 rounded-3 shadow-sm">
                    <i class="bi bi-x-circle-fill me-2"></i> ${errorMessage}<br>자세한 내용은 **개발자 도구 Console** 탭을 확인하세요!
                </div>
            `);
        }
    });
}

// ⭐⭐⭐ 새로 추가: 조 없는 예선 경기 렌더링 ⭐⭐⭐
function loadPreliminaryMatchesNoGroup(tournamentId, bracketContainer) {
    $.ajax({
        url: '/tournament/preliminaryMatchesNoGroup/' + tournamentId,
        method: 'GET',
        dataType: 'json',
        success: function(data) {
            console.log('FRONTEND DEBUG: Preliminary matches (no group) loaded:', data);

            if (!data.matches || data.matches.length === 0) {
                bracketContainer.html(`
                    <div class="alert alert-info text-center py-4 rounded-3 shadow-sm">
                        <i class="bi bi-info-circle-fill me-2"></i> 
                        생성된 예선 경기가 없습니다.<br>
                        관리자에게 문의하거나 예선 대진표 생성 후 다시 확인해 주세요.
                    </div>
                `);
                return;
            }

            // 조 없는 예선 경기 렌더링
            renderNoGroupPreliminaryMatches(data.matches, bracketContainer);
        },
        error: function(xhr, status, error) {
            console.error('FRONTEND ERROR: Failed to load preliminary matches without groups:', error);
            bracketContainer.html(`
                <div class="alert alert-danger text-center py-4 rounded-3 shadow-sm">
                    <i class="bi bi-x-circle-fill me-2"></i> 
                    예선 경기를 불러오는 중 오류가 발생했습니다.
                </div>
            `);
        }
    });
}

// ⭐⭐⭐ 새로 추가: 조 없는 예선 경기 테이블 렌더링 ⭐⭐⭐
function renderNoGroupPreliminaryMatches(matches, container) {
    let html = `
        <div class="group-section">
            <h3 class="group-title">
                <i class="bi bi-trophy-fill"></i> 예선 토너먼트
            </h3>
            <div class="alert alert-info mb-4">
                <i class="bi bi-info-circle me-2"></i>
                참가자 수가 적어 조 편성 없이 단일 토너먼트로 진행됩니다.
            </div>
            
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover align-middle">
                    <thead class="table-info text-center">
                        <tr>
                            <th style="width: 5%;">#</th>
                            <th style="width: 20%;">선수1</th>
                            <th style="width: 10%;">점수</th>
                            <th style="width: 20%;">선수2</th>
                            <th style="width: 15%;">경기시간</th>
                            <th style="width: 15%;">상태</th>
                            <th style="width: 15%;">액션</th>
                        </tr>
                    </thead>
                    <tbody>
    `;

    matches.forEach(match => {
        const player1Name = match.player1Name || 'BYE';
        const player2Name = match.player2Name || 'BYE';
        const score1 = match.score1 !== null && match.score1 !== undefined ? match.score1 : '';
        const score2 = match.score2 !== null && match.score2 !== undefined ? match.score2 : '';

        const winnerClass1 = (match.winner && match.winner === player1Name) ? 'winner-highlight' : '';
        const winnerClass2 = (match.winner && match.winner === player2Name) ? 'winner-highlight' : '';

        const statusBadge = match.status === 'COMPLETED' ? 'bg-success' :
            match.status === 'IN_PROGRESS' ? 'bg-warning' : 'bg-secondary';
        const statusText = match.status === 'COMPLETED' ? '완료' :
            match.status === 'IN_PROGRESS' ? '진행중' : '예정';

        const matchTimeFormatted = '-'; // 필요시 날짜 포맷팅

        // 액션 버튼
        let actionHtml = '';
        if (match.status === 'SCHEDULED') {
            actionHtml = `
                <div>
                    <input type="number" class="form-control form-control-sm d-inline-block match-score-input" 
                           style="width: 50px;" value="${score1}" data-match-id="${match.id}" 
                           data-player="1" min="0" placeholder="0">
                    :
                    <input type="number" class="form-control form-control-sm d-inline-block match-score-input" 
                           style="width: 50px;" value="${score2}" data-match-id="${match.id}" 
                           data-player="2" min="0" placeholder="0">
                    <button type="button" class="btn btn-primary btn-sm save-score-btn mt-1" 
                            data-match-id="${match.id}">저장</button>
                </div>
            `;
        } else {
            actionHtml = `<button type="button" class="btn btn-secondary btn-sm" disabled>입력완료</button>`;
        }

        html += `
            <tr class="text-center" data-match-id="${match.id}">
                <td>${match.matchNumber}</td>
                <td class="text-start">${player1Name}</td>
                <td>
                    <span class="score-display score1 ${winnerClass1}">
                        ${score1 === '' ? '-' : score1}
                    </span>
                    :
                    <span class="score-display score2 ${winnerClass2}">
                        ${score2 === '' ? '-' : score2}
                    </span>
                </td>
                <td class="text-start">${player2Name}</td>
                <td>${matchTimeFormatted}</td>
                <td>
                    <span class="badge ${statusBadge} match-status">${statusText}</span>
                    ${match.winner ? `<br><small class="text-muted">승자: ${match.winner}</small>` : ''}
                </td>
                <td>${actionHtml}</td>
            </tr>
        `;
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
    `;

    container.html(html);

    // 이벤트 리스너 부착
    attachMatchResultEventListeners(container);

    console.log('FRONTEND DEBUG: No-group preliminary matches rendered successfully.');
}

// ⭐⭐⭐ 기존 조별 렌더링 로직 분리 ⭐⭐⭐
function renderGroupBasedBrackets(response, bracketContainer) {
    let allGroupsHtml = '';

    response.matchGroups.forEach(group => {
        const groupId = group.id;
        const participants = response.groupParticipants[groupId] || [];
        const matches = response.groupMatches[groupId] || [];
        const rankings = (response.groupRankings[groupId] && response.groupRankings[groupId].rankingList) || [];

        // 참가 선수 목록 HTML 생성
        let participantsHtml = `
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover align-middle">
                    <thead class="table-primary">
                        <tr>
                            <th scope="col" style="width: 10%;">번호</th>
                            <th scope="col" style="width: 45%;">선수명</th>
                            <th scope="col" style="width: 45%;">소속 클럽</th>
                        </tr>
                    </thead>
                    <tbody>`;
        if (participants.length > 0) {
            participantsHtml += participants.map((p, index) => {
                const playerName = p.player ? p.player.name : '알 수 없음';
                const clubName = p.player && p.player.clubName ? p.player.clubName : '없음';
                return `
                    <tr>
                        <td>${index + 1}</td>
                        <td>${playerName}</td>
                        <td>${clubName}</td>
                    </tr>`;
            }).join('');
        } else {
            participantsHtml += `<tr><td colspan="3" class="text-center text-muted fst-italic">조에 배정된 선수가 없습니다.</td></tr>`;
        }
        participantsHtml += `</tbody></table></div>`;

        // 경기 진행 안내 HTML 생성 (기존 코드 유지)
        let matchesHtml = `
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover align-middle">
                    <thead class="table-info text-center">
                        <tr>
                            <th scope="col" style="width: 5%;">#</th>
                            <th scope="col" style="width: 15%;">매치업</th>
                            <th scope="col" style="width: 10%;">선수1</th>
                            <th scope="col" style="width: 8%;">점수1</th>
                            <th scope="col" style="width: 10%;">선수2</th>
                            <th scope="col" style="width: 8%;">점수2</th>
                            <th scope="col" style="width: 14%;">경기 시간</th>
                            <th scope="col" style="width: 15%;">상태 및 승자</th>
                            <th scope="col" style="width: 15%;">액션</th>
                        </tr>
                    </thead>
                    <tbody>`;
        if (matches.length > 0) {
            matchesHtml += matches.map(match => {
                const player1Name = match.player1 ? match.player1.name : '부전승';
                const player2Name = match.player2 ? match.player2.name : '부전승';
                const score1 = match.score1 !== null ? match.score1 : '';
                const score2 = match.score2 !== null ? match.score2 : '';
                const winnerClass1 = (match.winner && match.winner === player1Name) ? 'match-score winner-highlight' : 'match-score';
                const winnerClass2 = (match.winner && match.winner === player2Name) ? 'match-score winner-highlight' : 'match-score';
                const matchStatus = match.status;
                const matchWinnerText = match.winner && match.status === '완료' ? ` (<i class="bi bi-star-fill text-warning"></i> <span>${match.winner}</span>)` : '';

                // LocalDateTime 파싱
                let formattedMatchTime = '-';
                if (match.matchTime) {
                    let dateObj;
                    if (Array.isArray(match.matchTime)) {
                        dateObj = new Date(match.matchTime[0], match.matchTime[1] - 1, match.matchTime[2],
                            match.matchTime[3], match.matchTime[4], match.matchTime[5] || 0);
                    } else if (typeof match.matchTime === 'string') {
                        dateObj = new Date(match.matchTime);
                    } else {
                        console.warn("FRONTEND WARNING: Unknown matchTime format:", match.matchTime);
                        dateObj = null;
                    }
                    formattedMatchTime = dateObj ? dateObj.toLocaleString('ko-KR', {
                        year: 'numeric', month: '2-digit', day: '2-digit',
                        hour: '2-digit', minute: '2-digit', hour12: false
                    }) : '-';
                }

                const actionColumn = matchStatus === '예정' ? `
                    <div>
                        <input type="number" class="form-control form-control-sm d-inline-block match-score-input" style="width: 45px;" value="${score1}" data-match-id="${match.id}" data-player="1" min="0">
                        :
                        <input type="number" class="form-control form-control-sm d-inline-block match-score-input" style="width: 45px;" value="${score2}" data-match-id="${match.id}" data-player="2" min="0">
                        <button type="button" class="btn btn-primary btn-sm save-score-btn mt-1" data-match-id="${match.id}">저장</button>
                    </div>
                ` : `
                    <button type="button" class="btn btn-secondary btn-sm" disabled>결과입력 완료</button>
                `;

                return `
                    <tr class="text-center align-middle" data-match-id="${match.id}">
                        <td>${match.id}</td>
                        <td><span>${player1Name}</span> vs <span>${player2Name}</span></td>
                        <td class="text-start">${player1Name}</td>
                        <td><span class="score-display score1 ${winnerClass1}">${score1 === '' ? '-' : score1}</span></td>
                        <td class="text-start">${player2Name}</td>
                        <td><span class="score-display score2 ${winnerClass2}">${score2 === '' ? '-' : score2}</span></td>
                        <td>${formattedMatchTime}</td>
                        <td>
                            <span class="badge ${matchStatus === '완료' ? 'bg-success' : 'bg-warning'} match-status">${matchStatus}</span>
                            <span class="match-winner">${matchWinnerText}</span>
                        </td>
                        <td>${actionColumn}</td>
                    </tr>
                `;
            }).join('');
        } else {
            matchesHtml += `<tr><td colspan="9" class="text-center text-muted fst-italic">조에 배정된 경기가 없습니다.</td></tr>`;
        }
        matchesHtml += `</tbody></table></div>`;

        // 순위표 HTML 생성 (기존 코드 유지)
        let rankingsHtml = `
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover align-middle">
                    <thead class="table-success text-center">
                        <tr>
                            <th scope="col" style="width: 5%;">순위</th>
                            <th scope="col" style="width: 25%;">선수명</th>
                            <th scope="col" style="width: 15%;">경기</th>
                            <th scope="col" style="width: 15%;">승</th>
                            <th scope="col" style="width: 15%;">패</th>
                            <th scope="col" style="width: 10%;">득실차</th>
                            <th scope="col" style="width: 15%;">승률(%)</th>
                        </tr>
                    </thead>
                    <tbody>`;
        if (rankings.length > 0) {
            rankingsHtml += rankings.map((rankEntry, index) => {
                const playerName = rankEntry.player ? rankEntry.player.name : '알 수 없음';
                const winRate = rankEntry.played > 0 ? (rankEntry.wins / rankEntry.played * 100).toFixed(2) : '0.00';
                return `
                    <tr class="text-center align-middle">
                        <td>${index + 1}</td>
                        <td class="text-start">${playerName}</td>
                        <td>${rankEntry.played}</td>
                        <td>${rankEntry.wins}</td>
                        <td>${rankEntry.losses}</td>
                        <td>${rankEntry.score_diff}</td>
                        <td>${winRate}%</td>
                    </tr>
                `;
            }).join('');
        } else {
            rankingsHtml += `<tr><td colspan="7" class="text-center text-muted fst-italic">경기 데이터가 없어 순위를 표시할 수 없습니다.</td></tr>`;
        }
        rankingsHtml += `</tbody></table></div>`;

        allGroupsHtml += `
            <div class="group-section mb-5">
                <h3 class="group-title"><i class="bi bi-people-fill"></i><span>${group.name} 조</span></h3>

                <section class="mb-4">
                    <h4 class="section-subtitle">참가 선수</h4>
                    ${participantsHtml}
                </section>

                <section class="mb-4">
                    <h4 class="section-subtitle">경기 진행 안내 (<span>${group.name}</span> 예선)</h4>
                    ${matchesHtml}
                </section>

                <section>
                    <h4 class="section-subtitle">조별 순위 (<span>${group.name}</span>)</h4>
                    ${rankingsHtml}
                </section>
            </div>
        `;
    });

    bracketContainer.html(allGroupsHtml);
    attachMatchResultEventListeners(bracketContainer);
    console.log('FRONTEND DEBUG: Group brackets rendered successfully.');
}

// 경기 결과 저장 버튼 이벤트 리스너 (기존 코드 유지)
function attachMatchResultEventListeners(container) {
    container.find('.save-score-btn').on('click', function() {
        const button = $(this);
        const matchId = button.data('match-id');
        const matchRow = button.closest('tr');

        const score1Input = matchRow.find(`input[data-match-id="${matchId}"][data-player="1"]`);
        const score2Input = matchRow.find(`input[data-match-id="${matchId}"][data-player="2"]`);

        const score1 = parseInt(score1Input.val());
        const score2 = parseInt(score2Input.val());

        if (isNaN(score1) || isNaN(score2) || score1 < 0 || score2 < 0) {
            alert('유효한 점수를 입력해 주세요 (0 이상의 숫자).');
            return;
        }
        if (score1 === score2) {
            alert('무승부는 현재 지원되지 않습니다. 승패가 갈리도록 입력해 주세요.');
            return;
        }

        button.prop('disabled', true).text('저장 중...');

        $.ajax({
            url: `/match/api/updateResult/${matchId}`,
            method: 'POST',
            dataType: 'json',
            data: { score1: score1, score2: score2 },
            success: function(response) {
                alert(response.message);
                console.log('FRONTEND DEBUG: Score update success:', response);

                // UI 즉시 업데이트
                matchRow.find('.score-display.score1').text(score1);
                matchRow.find('.score-display.score2').text(score2);
                matchRow.find('.match-status').removeClass('bg-warning').addClass('bg-success').text('완료');

                let winnerName = '';
                if (score1 > score2) {
                    winnerName = matchRow.find('td:nth-child(3)').text().trim();
                    matchRow.find('.score-display.score1').addClass('winner-highlight');
                    matchRow.find('.score-display.score2').removeClass('winner-highlight');
                } else {
                    winnerName = matchRow.find('td:nth-child(5)').text().trim();
                    matchRow.find('.score-display.score2').addClass('winner-highlight');
                    matchRow.find('.score-display.score1').removeClass('winner-highlight');
                }
                matchRow.find('.match-winner').html(` (<i class="bi bi-star-fill text-warning"></i> <span>${winnerName}</span>)`);

                matchRow.find('td:last-child').html(`<button type="button" class="btn btn-secondary btn-sm" disabled>결과입력 완료</button>`);

                if (confirm('경기 결과가 업데이트되었습니다. 변경된 순위표를 확인하려면 페이지를 새로고침하시겠습니까?')) {
                    window.location.reload();
                }
            },
            error: function(xhr, status, error) {
                alert('경기 결과 업데이트 실패: ' + (xhr.responseJSON && xhr.responseJSON.error ? xhr.responseJSON.error : error));
                console.error('FRONTEND ERROR: Score update failed:', xhr, status, error);
            },
            complete: function() {
                button.prop('disabled', false).text('저장');
            }
        });
    });
}