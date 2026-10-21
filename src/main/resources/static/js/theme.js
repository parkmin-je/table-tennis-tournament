// 화면 깜빡임 없이 테마를 먼저 적용 (head 에서 동기 로드)
(function () {
  var theme = 'light';
  try {
    var saved = localStorage.getItem('theme');
    if (saved === 'light' || saved === 'dark') {
      theme = saved;
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      theme = 'dark';
    }
  } catch (e) { /* 저장소 접근 불가 */ }
  if (document.documentElement.getAttribute('data-force-theme')) {
    theme = document.documentElement.getAttribute('data-force-theme');
  }
  document.documentElement.setAttribute('data-bs-theme', theme);
})();
