/* 공통 동작: 테마 전환, 확인 대화상자, 검색, 알림 수, 좋아요, 목록 필터 */
(function () {
  'use strict';

  var csrfToken = meta('_csrf');
  var csrfHeader = meta('_csrf_header');

  function meta(name) {
    var el = document.querySelector('meta[name="' + name + '"]');
    return el ? el.getAttribute('content') : null;
  }

  /** CSRF 헤더를 붙인 fetch */
  window.appFetch = function (url, options) {
    options = options || {};
    options.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }, options.headers || {});
    if (csrfToken && csrfHeader && options.method && options.method !== 'GET') {
      options.headers[csrfHeader] = csrfToken;
    }
    options.credentials = 'same-origin';
    return fetch(url, options);
  };

  /** 텍스트로만 요소 생성 (XSS 방지: innerHTML 사용 안 함) */
  window.el = function (tag, attrs, children) {
    var node = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (k) {
        if (k === 'text') node.textContent = attrs[k];
        else if (k === 'class') node.className = attrs[k];
        else node.setAttribute(k, attrs[k]);
      });
    }
    (children || []).forEach(function (c) { if (c) node.appendChild(c); });
    return node;
  };

  // ── 현재 메뉴 표시 ──────────────────────────────────────
  var path = location.pathname;
  document.querySelectorAll('[data-main-nav] .nav-link').forEach(function (a) {
    var href = a.getAttribute('href');
    var section = href.replace(/s$/, '');
    if (path === href || path.indexOf(href + '/') === 0 || (section !== href && path.indexOf(section + '/') === 0)) {
      a.classList.add('active');
      a.setAttribute('aria-current', 'page');
    }
  });
  if (path === '/ranking') {
    document.querySelectorAll('[data-main-nav] .nav-link[href="/players"]').forEach(function (a) { a.classList.remove('active'); });
  }

  // ── 테마 ────────────────────────────────────────────────
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-theme-toggle]');
    if (!btn) return;
    var next = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-bs-theme', next);
    try { localStorage.setItem('theme', next); } catch (err) { /* 무시 */ }
  });

  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-print]')) window.print();
  });

  // ── 위험 작업 확인 ──────────────────────────────────────
  document.addEventListener('submit', function (e) {
    var form = e.target;
    var msg = form.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) {
      e.preventDefault();
      return;
    }
    // 중복 제출 방지
    if (!form.hasAttribute('data-allow-resubmit')) {
      if (form.dataset.submitting === '1') { e.preventDefault(); return; }
      form.dataset.submitting = '1';
      var btn = form.querySelector('[type=submit]');
      if (btn) { btn.classList.add('disabled'); }
      setTimeout(function () { form.dataset.submitting = ''; if (btn) btn.classList.remove('disabled'); }, 4000);
    }
  }, true);

  // 선택 즉시 제출 (상태 변경 셀렉트 등)
  document.addEventListener('change', function (e) {
    var t = e.target;
    if (t.matches('[data-autosubmit]')) {
      var form = t.form;
      var msg = t.getAttribute('data-autosubmit');
      if (msg && !window.confirm(msg)) { t.value = t.getAttribute('data-original') || t.value; return; }
      form.requestSubmit ? form.requestSubmit() : form.submit();
    }
  });

  // ── 알림 메시지 자동 닫기 ───────────────────────────────
  document.querySelectorAll('.flash[data-autohide]').forEach(function (f) {
    setTimeout(function () { f.style.transition = 'opacity .3s'; f.style.opacity = '0'; setTimeout(function () { f.remove(); }, 300); }, 5000);
  });
  document.addEventListener('click', function (e) {
    var close = e.target.closest('.flash .btn-close');
    if (close) close.closest('.flash').remove();
  });

  // ── 목록 즉석 필터 ──────────────────────────────────────
  document.querySelectorAll('[data-filter-target]').forEach(function (input) {
    var target = document.querySelector(input.getAttribute('data-filter-target'));
    if (!target) return;
    input.addEventListener('input', function () {
      var q = input.value.trim().toLowerCase();
      target.querySelectorAll('[data-filter-text]').forEach(function (row) {
        row.style.display = !q || row.getAttribute('data-filter-text').toLowerCase().indexOf(q) >= 0 ? '' : 'none';
      });
    });
  });

  // 셀렉트 옵션 필터 (선수 선택 등)
  document.querySelectorAll('[data-option-filter]').forEach(function (input) {
    var select = document.querySelector(input.getAttribute('data-option-filter'));
    if (!select) return;
    var options = Array.prototype.slice.call(select.options);
    input.addEventListener('input', function () {
      var q = input.value.trim().toLowerCase();
      var first = null;
      options.forEach(function (o) {
        var show = !o.value || !q || o.textContent.toLowerCase().indexOf(q) >= 0;
        o.hidden = !show;
        if (show && o.value && !first) first = o;
      });
      if (first && q) select.value = first.value;
    });
  });

  // ── 좋아요 ──────────────────────────────────────────────
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-like]');
    if (!btn) return;
    e.preventDefault();
    appFetch(btn.getAttribute('data-like'), { method: 'POST' }).then(function (r) {
      if (r.status === 401 || r.redirected) { location.href = '/user/login'; return null; }
      return r.json();
    }).then(function (data) {
      if (!data || data.error) return;
      btn.classList.toggle('active', data.liked);
      btn.querySelector('i').className = data.liked ? 'bi bi-heart-fill' : 'bi bi-heart';
      btn.querySelector('[data-like-count]').textContent = data.count;
    });
  });

  // ── 읽지 않은 알림 수 (1분마다) ─────────────────────────
  var badge = document.querySelector('[data-unread]');
  if (badge) {
    setInterval(function () {
      if (document.hidden) return;
      appFetch('/api/notifications/unread-count').then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
        if (!d) return;
        badge.textContent = d.count > 99 ? '99+' : d.count;
        badge.hidden = d.count === 0;
      }).catch(function () {});
    }, 60000);
  }

  // ── 검색 (Ctrl/⌘ + K) ───────────────────────────────────
  var modalEl = document.getElementById('searchModal');
  if (modalEl && window.bootstrap) {
    var modal = new bootstrap.Modal(modalEl);
    var input = modalEl.querySelector('input');
    var results = modalEl.querySelector('.search-results');
    var timer = null;

    document.addEventListener('keydown', function (e) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); modal.show(); }
    });
    document.querySelectorAll('[data-search-open]').forEach(function (b) { b.addEventListener('click', function () { modal.show(); }); });
    modalEl.addEventListener('shown.bs.modal', function () { input.focus(); input.select(); });

    input.addEventListener('input', function () {
      clearTimeout(timer);
      timer = setTimeout(run, 180);
    });

    function run() {
      var q = input.value.trim();
      results.replaceChildren();
      if (!q) return;
      appFetch('/api/search?q=' + encodeURIComponent(q)).then(function (r) { return r.json(); }).then(function (d) {
        results.replaceChildren();
        if (!d.tournaments.length && !d.players.length) {
          results.appendChild(el('div', { class: 'empty', text: '"' + q + '" 검색 결과가 없습니다.' }));
          return;
        }
        if (d.tournaments.length) {
          results.appendChild(el('div', { class: 'search-group', text: '대회' }));
          d.tournaments.forEach(function (t) {
            results.appendChild(el('a', { class: 'list-row', href: '/tournament/' + t.id }, [
              el('i', { class: 'bi bi-trophy text-faint' }),
              el('div', { class: 'min-w-0 flex-grow-1' }, [
                el('div', { class: 'fw-semibold truncate', text: t.title }),
                el('div', { class: 'entry-sub', text: t.date + ' · ' + t.status })
              ])
            ]));
          });
        }
        if (d.players.length) {
          results.appendChild(el('div', { class: 'search-group', text: '선수' }));
          d.players.forEach(function (p) {
            results.appendChild(el('a', { class: 'list-row', href: '/player/' + p.id }, [
              el('span', { class: 'avatar', text: p.name.charAt(0) }),
              el('div', { class: 'min-w-0 flex-grow-1' }, [
                el('div', { class: 'fw-semibold truncate', text: p.name }),
                el('div', { class: 'entry-sub', text: p.club + ' · ' + p.division })
              ]),
              el('span', { class: 'rating', text: String(p.rating) })
            ]));
          });
        }
      }).catch(function () {
        results.replaceChildren(el('div', { class: 'empty', text: '검색 중 오류가 발생했습니다.' }));
      });
    }
  }
})();
