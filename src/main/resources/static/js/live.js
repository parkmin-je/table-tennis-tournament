/*
 * 실시간 갱신: 서버가 "바뀌었다"는 신호만 보내면, 화면 조각(data-fragment)을 서버에서 다시 렌더링해 교체한다.
 * - 데이터는 항상 서버 렌더링(이스케이프 완료) HTML 로만 받는다.
 * - WebSocket 이 끊기면 30초마다 폴링으로 대체한다.
 */
(function () {
  'use strict';
  var root = document.querySelector('[data-live-tournament]');
  if (!root) return;

  var tournamentId = root.getAttribute('data-live-tournament');
  var conn = document.querySelectorAll('[data-conn]');
  var connected = false;
  var pending = null;
  var lastRefresh = 0;

  function setConn(on) {
    connected = on;
    conn.forEach(function (c) {
      var dot = c.querySelector('.conn-dot');
      if (dot) dot.classList.toggle('on', on);
      var label = c.querySelector('[data-conn-label]');
      if (label) label.textContent = on ? '실시간 연결됨' : '재연결 중…';
    });
  }

  function refresh() {
    clearTimeout(pending);
    pending = setTimeout(function () {
      lastRefresh = Date.now();
      document.querySelectorAll('[data-fragment]').forEach(function (box) {
        var name = box.getAttribute('data-fragment');
        fetch('/tournament/' + tournamentId + '/fragment/' + name, { credentials: 'same-origin', headers: { 'Accept': 'text/html' } })
          .then(function (r) { return r.ok ? r.text() : null; })
          .then(function (html) {
            if (html === null) return;
            var doc = new DOMParser().parseFromString(html, 'text/html');
            var fresh = doc.body.firstElementChild;
            if (fresh) {
              box.replaceChildren(document.importNode(fresh, true));
              updateElapsed();
            }
          })
          .catch(function () {});
      });
    }, 350);
  }

  function toast(message) {
    if (!message) return;
    var stack = document.querySelector('.flash-stack');
    if (!stack) return;
    var item = el('div', { class: 'flash flash-info' }, [el('i', { class: 'bi bi-broadcast' }), el('div', { text: message })]);
    stack.appendChild(item);
    setTimeout(function () { item.remove(); }, 6000);
  }

  if (window.StompJs) {
    var proto = location.protocol === 'https:' ? 'wss://' : 'ws://';
    var client = new StompJs.Client({
      brokerURL: proto + location.host + '/ws',
      reconnectDelay: 4000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000
    });
    client.onConnect = function () {
      setConn(true);
      client.subscribe('/topic/tournament/' + tournamentId, function (frame) {
        var event = {};
        try { event = JSON.parse(frame.body); } catch (e) { /* 무시 */ }
        if (root.hasAttribute('data-live-toast')) toast(event.message);
        refresh();
      });
      // 끊긴 동안 놓친 변경 반영
      if (Date.now() - lastRefresh > 5000) refresh();
    };
    client.onWebSocketClose = function () { setConn(false); };
    client.onStompError = function () { setConn(false); };
    client.activate();
  }

  setInterval(function () {
    if (document.hidden) return;
    if (!connected || Date.now() - lastRefresh > 60000) refresh();
  }, 30000);

  document.addEventListener('visibilitychange', function () {
    if (!document.hidden && Date.now() - lastRefresh > 15000) refresh();
  });

  // 현황판 시계
  var clock = document.querySelector('[data-clock]');
  if (clock) {
    var tick = function () {
      var d = new Date();
      clock.textContent = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
    };
    tick();
    setInterval(tick, 10000);
  }

  // 전체 화면
  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-fullscreen]')) {
      if (!document.fullscreenElement) document.documentElement.requestFullscreen().catch(function () {});
      else document.exitFullscreen();
    }
  });

  // 진행 시간 표시 (data-started="2026-10-21T10:20:00")
  function updateElapsed() {
    document.querySelectorAll('[data-started]').forEach(function (n) {
      var t = Date.parse(n.getAttribute('data-started'));
      if (isNaN(t)) return;
      var min = Math.max(0, Math.floor((Date.now() - t) / 60000));
      n.textContent = min + '분째';
    });
  }
  updateElapsed();
  setInterval(updateElapsed, 30000);
})();
