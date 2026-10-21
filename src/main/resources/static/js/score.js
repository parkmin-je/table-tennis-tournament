/*
 * 결과 입력 폼 (운영 콘솔 모달 · 심판 화면 공용)
 * - 게임별 점수 행을 "11-9,8-11,..." 로 합쳐 gameScores 로 전송
 * - 게임 수 빠른 입력(3:0, 3:1 …), 기권/부전승 승자 선택
 */
(function () {
  'use strict';

  function need(form) { return Math.floor(Number(form.dataset.bestof || 5) / 2) + 1; }

  function rows(form) { return Array.prototype.slice.call(form.querySelectorAll('[data-game-row]')); }

  function collect(form) {
    var games = [];
    rows(form).forEach(function (row) {
      var a = row.querySelector('[data-g1]').value.trim();
      var b = row.querySelector('[data-g2]').value.trim();
      if (a !== '' && b !== '') games.push([Number(a), Number(b)]);
    });
    return games;
  }

  function validGame(a, b) {
    var hi = Math.max(a, b), lo = Math.min(a, b);
    return (hi === 11 && lo <= 9) || (lo >= 10 && hi - lo === 2);
  }

  function preview(form) {
    var out = form.querySelector('[data-preview]');
    if (!out) return;
    var games = collect(form);
    var w1 = 0, w2 = 0, bad = false;
    games.forEach(function (g) {
      if (!validGame(g[0], g[1])) bad = true;
      if (g[0] > g[1]) w1++; else w2++;
    });
    var n = need(form);
    var names = [form.dataset.e1 || '선수1', form.dataset.e2 || '선수2'];
    if (!games.length) { out.textContent = '게임 점수를 입력하세요'; out.className = 'result-preview text-muted'; return; }
    if (bad) { out.textContent = '11점 선취(10:10 이후 2점 차) 규칙에 맞지 않는 게임이 있습니다'; out.className = 'result-preview text-danger'; return; }
    if (w1 >= n || w2 >= n) {
      out.textContent = (w1 > w2 ? names[0] : names[1]) + ' 승 (' + w1 + ' : ' + w2 + ')';
      out.className = 'result-preview text-success';
    } else {
      out.textContent = '진행 중 ' + w1 + ' : ' + w2 + ' (' + n + '게임 선승)';
      out.className = 'result-preview text-muted';
    }
  }

  function setMode(form, mode) {
    form.querySelector('[name=mode]').value = mode;
    form.querySelectorAll('[data-mode-pane]').forEach(function (p) {
      p.hidden = p.getAttribute('data-mode-pane') !== mode && !(mode === 'COUNT' && p.getAttribute('data-mode-pane') === 'GAMES');
    });
    form.querySelectorAll('[data-mode-btn]').forEach(function (b) {
      b.classList.toggle('active', b.getAttribute('data-mode-btn') === mode || (mode === 'COUNT' && b.getAttribute('data-mode-btn') === 'GAMES'));
    });
  }

  window.ScoreForm = {
    reset: function (form) {
      rows(form).forEach(function (row, i) {
        row.querySelector('[data-g1]').value = '';
        row.querySelector('[data-g2]').value = '';
        row.hidden = i >= Number(form.dataset.bestof || 5);
      });
      form.querySelectorAll('[name=winnerSide]').forEach(function (r) { r.checked = false; });
      form.querySelectorAll('[data-name1]').forEach(function (n) { n.textContent = form.dataset.e1 || ''; });
      form.querySelectorAll('[data-name2]').forEach(function (n) { n.textContent = form.dataset.e2 || ''; });
      var quick = form.querySelector('[data-quick]');
      if (quick) {
        quick.replaceChildren();
        var n = need(form);
        for (var lose = 0; lose < n; lose++) {
          quick.appendChild(quickBtn(n, lose));
        }
        for (var l2 = n - 1; l2 >= 0; l2--) {
          quick.appendChild(quickBtn(l2, n));
        }
      }
      setMode(form, form.dataset.defaultMode || 'GAMES');
      preview(form);
    }
  };

  function quickBtn(a, b) {
    return el('button', { type: 'button', class: 'btn btn-soft btn-sm', 'data-count': a + ':' + b, text: a + ' : ' + b });
  }

  document.querySelectorAll('[data-score-form]').forEach(function (form) {
    form.addEventListener('input', function (e) {
      if (e.target.matches('[data-g1],[data-g2]')) {
        // 한쪽이 11 미만이면 상대 11 자동 채움은 하지 않음 (오입력 방지)
        preview(form);
      }
    });

    form.addEventListener('click', function (e) {
      var modeBtn = e.target.closest('[data-mode-btn]');
      if (modeBtn) { setMode(form, modeBtn.getAttribute('data-mode-btn')); return; }
      var q = e.target.closest('[data-count]');
      if (q) {
        var parts = q.getAttribute('data-count').split(':');
        var names = [form.dataset.e1 || '선수1', form.dataset.e2 || '선수2'];
        var winner = Number(parts[0]) > Number(parts[1]) ? names[0] : names[1];
        if (!window.confirm(winner + ' 승 (' + parts[0] + ' : ' + parts[1] + ') 으로 저장할까요?')) return;
        form.querySelector('[name=mode]').value = 'COUNT';
        form.querySelector('[name=games1]').value = parts[0];
        form.querySelector('[name=games2]').value = parts[1];
        form.requestSubmit ? form.requestSubmit() : form.submit();
      }
    });

    form.addEventListener('submit', function (e) {
      var mode = form.querySelector('[name=mode]').value;
      if (mode === 'GAMES') {
        var games = collect(form);
        if (!games.length) {
          e.preventDefault();
          window.alert('게임 점수를 입력하거나 게임 수 빠른 입력을 사용하세요.');
          return;
        }
        form.querySelector('[name=gameScores]').value = games.map(function (g) { return g[0] + '-' + g[1]; }).join(',');
      }
      if ((mode === 'RETIRED' || mode === 'WALKOVER') && !form.querySelector('[name=winnerSide]:checked')) {
        e.preventDefault();
        window.alert('승자를 선택하세요.');
      }
    }, true);

    if (form.hasAttribute('data-autoinit')) window.ScoreForm.reset(form);
  });
})();
