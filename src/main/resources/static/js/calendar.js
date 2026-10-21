(function () {
  'use strict';
  var node = document.getElementById('calendar');
  if (!node || !window.FullCalendar) return;
  function pad(n) { return String(n).padStart(2, '0'); }
  function fmt(d) {
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':00';
  }
  var calendar = new FullCalendar.Calendar(node, {
    locale: 'ko',
    initialView: window.innerWidth < 768 ? 'listMonth' : 'dayGridMonth',
    headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,listMonth' },
    buttonText: { today: '오늘', month: '월', list: '목록' },
    height: 'auto',
    events: function (info, success, failure) {
      var url = '/api/calendar?start=' + encodeURIComponent(fmt(info.start)) + '&end=' + encodeURIComponent(fmt(info.end));
      fetch(url, { credentials: 'same-origin' }).then(function (r) { return r.json(); }).then(success).catch(failure);
    },
    eventDidMount: function (arg) {
      var p = arg.event.extendedProps;
      arg.el.title = arg.event.title + ' · ' + p.status + (p.venue ? ' · ' + p.venue : '');
    }
  });
  calendar.render();
})();
