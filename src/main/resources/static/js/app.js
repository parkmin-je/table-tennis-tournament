/**
 * app.js — Premium Global Utilities
 * 안산시 탁구 협회 · Commercial Grade
 */

'use strict';

/* ==================== NPROGRESS ==================== */
const NP = (() => {
  let bar, spinner, timer, val = 0;

  function init() {
    bar     = document.getElementById('nprogress-bar');
    spinner = document.getElementById('nprogress-spinner');
  }

  function start() {
    if (!bar) init();
    val = 1;
    if (bar)     { bar.style.width = '1%'; bar.classList.add('active'); }
    if (spinner) spinner.classList.add('active');
    clearInterval(timer);
    timer = setInterval(() => {
      const inc = val < 25 ? 8 : val < 55 ? 4 : val < 80 ? 2 : 0.5;
      val = Math.min(val + inc, 92);
      if (bar) bar.style.width = val + '%';
    }, 180);
  }

  function done() {
    clearInterval(timer);
    if (bar) bar.style.width = '100%';
    setTimeout(() => {
      if (bar)     { bar.classList.remove('active'); bar.style.width = '0%'; }
      if (spinner) spinner.classList.remove('active');
      val = 0;
    }, 300);
  }

  return { start, done };
})();

/* ==================== TOAST ==================== */
const Toast = (() => {
  let stack;

  function getStack() {
    if (!stack) {
      stack = document.getElementById('toastStack');
      if (!stack) {
        stack = document.createElement('div');
        stack.id = 'toastStack';
        stack.className = 'toast-stack';
        document.body.appendChild(stack);
      }
    }
    return stack;
  }

  const ICONS = {
    success: 'bi-check-circle-fill',
    error:   'bi-x-circle-fill',
    warning: 'bi-exclamation-triangle-fill',
    info:    'bi-info-circle-fill'
  };

  const TITLES = { success: '성공', error: '오류', warning: '경고', info: '알림' };

  function show(type, title, msg, dur = 4000) {
    const s = getStack();
    const el = document.createElement('div');
    el.className = `toast-item toast-${type}`;
    el.style.setProperty('--toast-dur', dur + 'ms');
    el.innerHTML = `
      <div class="toast-icon"><i class="bi ${ICONS[type] || ICONS.info}"></i></div>
      <div class="toast-body">
        <div class="toast-title">${title || TITLES[type]}</div>
        ${msg ? `<div class="toast-msg">${msg}</div>` : ''}
      </div>
      <button class="toast-close" title="닫기">✕</button>`;

    s.appendChild(el);
    requestAnimationFrame(() => requestAnimationFrame(() => el.classList.add('show')));

    const dismiss = () => {
      el.classList.add('hide');
      setTimeout(() => el.remove(), 280);
    };

    const timeout = setTimeout(dismiss, dur);
    el.querySelector('.toast-close').addEventListener('click', () => { clearTimeout(timeout); dismiss(); });
    el.addEventListener('click', () => { clearTimeout(timeout); dismiss(); });
    return el;
  }

  return {
    show,
    success: (t, m, d) => show('success', t, m, d),
    error:   (t, m, d) => show('error',   t, m, d),
    warning: (t, m, d) => show('warning', t, m, d),
    info:    (t, m, d) => show('info',    t, m, d)
  };
})();

/* ==================== GLOBAL SEARCH ==================== */
const Search = (() => {
  let overlay, box, input, results;
  let debounceTimer;

  const QUICK_LINKS = [
    { href:'/tournament/list',  icon:'bi-trophy-fill',    bg:'#eff6ff', color:'#3b82f6', name:'대회 목록',   sub:'진행 중인 대회 보기' },
    { href:'/player/ranking',   icon:'bi-bar-chart-fill', bg:'#f0fdf4', color:'#16a34a', name:'선수 랭킹',   sub:'전체 순위 확인' },
    { href:'/player/list',      icon:'bi-people-fill',    bg:'#eff6ff', color:'#6366f1', name:'선수 목록',   sub:'등록된 선수 보기' },
    { href:'/club/list',        icon:'bi-buildings-fill', bg:'#f0fdf4', color:'#059669', name:'클럽 목록',   sub:'클럽 목록 보기' },
    { href:'/board/list',       icon:'bi-chat-dots-fill', bg:'#fdf4ff', color:'#9333ea', name:'커뮤니티',    sub:'게시판 바로가기' },
    { href:'/calendar',         icon:'bi-calendar-event-fill', bg:'#fff7ed', color:'#f97316', name:'캘린더', sub:'대회 일정 확인' }
  ];

  function init() {
    overlay = document.getElementById('searchOverlay');
    box     = document.getElementById('searchBox');
    input   = document.getElementById('searchInput');
    results = document.getElementById('searchResults');
    if (!overlay) return;

    overlay.addEventListener('click', e => { if (e.target === overlay) close(); });
    document.getElementById('searchClose')?.addEventListener('click', close);
    input?.addEventListener('input', e => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => query(e.target.value.trim()), 250);
    });
    input?.addEventListener('keydown', e => {
      if (e.key === 'Escape') close();
      if (e.key === 'Enter') {
        const focused = results?.querySelector('.search-item.focused');
        if (focused) { focused.click(); }
      }
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') navigate(e.key === 'ArrowDown');
    });

    document.addEventListener('keydown', e => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') { e.preventDefault(); open(); }
      if (e.key === 'Escape' && overlay.classList.contains('open')) close();
    });

    document.querySelectorAll('[data-search]').forEach(btn => {
      btn.addEventListener('click', open);
    });
  }

  function open() {
    if (!overlay) return;
    overlay.classList.add('open');
    setTimeout(() => input?.focus(), 100);
    renderQuickLinks();
  }

  function close() {
    overlay?.classList.remove('open');
    if (input) input.value = '';
  }

  function navigate(down) {
    const items = Array.from(results?.querySelectorAll('.search-item') || []);
    if (!items.length) return;
    const cur = results.querySelector('.search-item.focused');
    let idx = cur ? items.indexOf(cur) + (down ? 1 : -1) : (down ? 0 : -1);
    idx = Math.max(0, Math.min(idx, items.length - 1));
    cur?.classList.remove('focused');
    items[idx]?.classList.add('focused');
    items[idx]?.scrollIntoView({ block: 'nearest' });
  }

  function renderQuickLinks() {
    if (!results) return;
    const html = `
      <div class="search-category">빠른 이동</div>
      ${QUICK_LINKS.map(l => `
        <a href="${l.href}" class="search-item" onclick="Search.close()">
          <div class="search-item-icon" style="background:${l.bg};color:${l.color}">
            <i class="bi ${l.icon}"></i>
          </div>
          <div>
            <div class="search-item-name">${l.name}</div>
            <div class="search-item-meta">${l.sub}</div>
          </div>
        </a>`).join('')}`;
    results.innerHTML = html;
  }

  function renderLoading() {
    results.innerHTML = `
      <div class="search-category">검색 중...</div>
      ${Array(3).fill(`
        <div class="search-item">
          <div class="skeleton sk-avatar"></div>
          <div class="sk-col">
            <div class="skeleton sk-text" style="width:60%"></div>
            <div class="skeleton sk-text-sm"></div>
          </div>
        </div>`).join('')}`;
  }

  function highlight(text, q) {
    const re = new RegExp(`(${q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return text.replace(re, '<mark class="search-hl">$1</mark>');
  }

  async function query(q) {
    if (!q) { renderQuickLinks(); return; }
    if (q.length < 1) return;
    renderLoading();
    try {
      const res  = await fetch(`/api/search?q=${encodeURIComponent(q)}`);
      const data = await res.json();
      renderResults(data, q);
    } catch {
      results.innerHTML = `<div class="search-empty"><i class="bi bi-exclamation-circle"></i><p>검색 오류가 발생했습니다</p></div>`;
    }
  }

  function renderResults(data, q) {
    const { tournaments = [], players = [] } = data;
    if (!tournaments.length && !players.length) {
      results.innerHTML = `<div class="search-empty"><i class="bi bi-search"></i><p>"${q}" 검색 결과가 없습니다</p></div>`;
      return;
    }
    let html = '';
    if (tournaments.length) {
      html += `<div class="search-category">🏆 대회 (${tournaments.length})</div>`;
      tournaments.slice(0, 6).forEach(t => {
        html += `<a href="/tournament/detail/${t.id}" class="search-item" onclick="Search.close()">
          <div class="search-item-icon" style="background:#eff6ff;color:#3b82f6"><i class="bi bi-trophy-fill"></i></div>
          <div>
            <div class="search-item-name">${highlight(t.title, q)}</div>
            <div class="search-item-meta">${t.statusLabel} · ${t.typeLabel}</div>
          </div></a>`;
      });
    }
    if (players.length) {
      html += `<div class="search-category">🏓 선수 (${players.length})</div>`;
      players.slice(0, 6).forEach(p => {
        html += `<a href="/player/detail/${p.id}" class="search-item" onclick="Search.close()">
          <div class="search-item-icon" style="background:#f0fdf4;color:#16a34a"><i class="bi bi-person-fill"></i></div>
          <div>
            <div class="search-item-name">${highlight(p.name, q)}</div>
            <div class="search-item-meta">랭킹 ${p.ranking} · ${p.clubName || '클럽 없음'}</div>
          </div></a>`;
      });
    }
    results.innerHTML = html;
  }

  return { init, open, close };
})();

/* ==================== SCROLL PROGRESS ==================== */
const ScrollProgress = (() => {
  function init() {
    const bar = document.getElementById('scrollProgressBar');
    if (!bar) return;
    window.addEventListener('scroll', () => {
      const total = document.documentElement.scrollHeight - window.innerHeight;
      bar.style.width = total > 0 ? `${(window.scrollY / total) * 100}%` : '0%';
    }, { passive: true });
  }
  return { init };
})();

/* ==================== BACK TO TOP ==================== */
const BackToTop = (() => {
  function init() {
    const btn = document.getElementById('backToTop');
    if (!btn) return;
    window.addEventListener('scroll', () => {
      btn.classList.toggle('visible', window.scrollY > 500);
    }, { passive: true });
    btn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  }
  return { init };
})();

/* ==================== SCROLL ANIMATIONS ==================== */
const Scroll = (() => {
  function init() {
    if (!('IntersectionObserver' in window)) {
      document.querySelectorAll('.ani-up,.ani-left,.ani-right,.ani-scale').forEach(el => el.classList.add('in'));
      return;
    }
    const obs = new IntersectionObserver(entries => {
      entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); obs.unobserve(e.target); } });
    }, { threshold: 0.08, rootMargin: '0px 0px -20px 0px' });
    document.querySelectorAll('.ani-up,.ani-left,.ani-right,.ani-scale').forEach(el => obs.observe(el));
  }
  return { init };
})();

/* ==================== RIPPLE ==================== */
const Ripple = (() => {
  function add(el) {
    if (el.dataset.rippleAdded) return;
    el.classList.add('ripple-host');
    el.dataset.rippleAdded = '1';
    el.addEventListener('click', function(e) {
      const existing = this.querySelector('.ripple-wave');
      if (existing) existing.remove();
      const rect = this.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height);
      const w = document.createElement('span');
      w.className = 'ripple-wave';
      w.style.cssText = `width:${size}px;height:${size}px;left:${e.clientX-rect.left-size/2}px;top:${e.clientY-rect.top-size/2}px`;
      this.appendChild(w);
      w.addEventListener('animationend', () => w.remove());
    });
  }

  function init() {
    const sel = '.btn,.btn-submit,.btn-update,.btn-change,.btn-login,button[type="submit"],.mobile-nav-btn';
    document.querySelectorAll(sel).forEach(add);
    const obs = new MutationObserver(mutations => {
      mutations.forEach(m => m.addedNodes.forEach(n => {
        if (n.nodeType === 1) {
          if (n.matches?.(sel)) add(n);
          n.querySelectorAll?.(sel).forEach(add);
        }
      }));
    });
    obs.observe(document.body, { childList: true, subtree: true });
  }
  return { init };
})();

/* ==================== ACTIVE NAV ==================== */
const ActiveNav = (() => {
  function init() {
    const path = location.pathname;
    document.querySelectorAll('.navbar .nav-link[href]').forEach(a => {
      const h = a.getAttribute('href');
      if (!h) return;
      const isActive = h !== '/' ? path.startsWith(h) : path === '/';
      if (isActive) a.classList.add('nav-active');
    });
    document.querySelectorAll('.mobile-nav-btn[href]').forEach(a => {
      const h = a.getAttribute('href');
      if (h && path.startsWith(h)) a.classList.add('active');
    });
  }
  return { init };
})();

/* ==================== COUNTER ==================== */
const Counter = (() => {
  function init() {
    if (!('IntersectionObserver' in window)) return;
    const obs = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (!e.isIntersecting) return;
        const el = e.target;
        const target = parseInt(el.dataset.count, 10);
        if (isNaN(target)) return;
        let cur = 0; const inc = target / 60;
        const timer = setInterval(() => {
          cur = Math.min(cur + inc, target);
          el.textContent = Math.floor(cur).toLocaleString('ko-KR');
          if (cur >= target) { el.textContent = target.toLocaleString('ko-KR'); clearInterval(timer); }
        }, 16);
        obs.unobserve(el);
      });
    }, { threshold: 0.6 });
    document.querySelectorAll('[data-count]').forEach(el => obs.observe(el));
  }
  return { init };
})();

/* ==================== FLASH MESSAGE AUTO-DISMISS ==================== */
const Flash = (() => {
  function init() {
    document.querySelectorAll('.flash-message-container .alert').forEach(a => {
      setTimeout(() => { try { bootstrap.Alert.getOrCreateInstance(a).close(); } catch {} }, 4500);
    });
    // Convert to toast if bootstrap toast container exists
    const flashEl = document.querySelector('.flash-message-container .alert span');
    if (flashEl) {
      const text = flashEl.textContent?.trim();
      const isSuccess = flashEl.closest('.alert-success');
      if (text) Toast.show(isSuccess ? 'success' : 'error', isSuccess ? '처리 완료' : '오류', text);
    }
  }
  return { init };
})();

/* ==================== PAGE LOAD PROGRESS ==================== */
const PageLoad = (() => {
  function init() {
    // Start on link click / form submit
    document.addEventListener('click', e => {
      const a = e.target.closest('a[href]');
      if (!a) return;
      const href = a.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('javascript') || a.target) return;
      if (href !== location.href) NP.start();
    });
    document.addEventListener('submit', e => {
      if (e.target.tagName === 'FORM') NP.start();
    });
    NP.done();
  }
  return { init };
})();

/* ==================== NOTIFICATION PULSE ==================== */
const NotifBadge = (() => {
  function init() {
    const badge = document.getElementById('unreadNotificationBadge');
    if (badge && badge.style.display !== 'none') badge.classList.add('notif-badge-pulse');
  }
  return { init };
})();

/* ==================== INIT ALL ==================== */
document.addEventListener('DOMContentLoaded', () => {
  Search.init();
  ScrollProgress.init();
  BackToTop.init();
  Scroll.init();
  Ripple.init();
  ActiveNav.init();
  Counter.init();
  Flash.init();
  PageLoad.init();
  NotifBadge.init();
});

// Expose globals
window.App   = { Toast, Search, NP };
window.Toast = Toast;
window.Search = Search;
