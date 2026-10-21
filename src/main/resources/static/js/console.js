/* 운영 콘솔: 결과 입력 모달 채우기, 탭 상태 유지 */
(function () {
  'use strict';

  var modalEl = document.getElementById('resultModal');
  if (modalEl && window.bootstrap) {
    var modal = new bootstrap.Modal(modalEl);
    var form = modalEl.querySelector('form');

    document.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-result]');
      if (!btn) return;
      e.preventDefault();
      form.action = '/manage/matches/' + btn.getAttribute('data-result') + '/result';
      form.dataset.e1 = btn.getAttribute('data-e1');
      form.dataset.e2 = btn.getAttribute('data-e2');
      form.dataset.bestof = btn.getAttribute('data-bestof');
      form.querySelector('[name=correction]').value = btn.hasAttribute('data-correction') ? 'true' : 'false';
      modalEl.querySelector('[data-modal-title]').textContent =
        (btn.hasAttribute('data-correction') ? '결과 수정 · ' : '결과 입력 · ') + btn.getAttribute('data-round');
      window.ScoreForm.reset(form);
      modal.show();
    });

    modalEl.addEventListener('shown.bs.modal', function () {
      var first = form.querySelector('[data-g1]');
      if (first) first.focus();
    });
  }

  // 탭 위치를 주소에 반영 (새로고침·리다이렉트 후 유지)
  document.querySelectorAll('[data-tab-sync] [data-bs-toggle="tab"]').forEach(function (tab) {
    tab.addEventListener('shown.bs.tab', function () {
      var url = new URL(location.href);
      url.searchParams.set('tab', tab.getAttribute('data-tab'));
      history.replaceState(null, '', url);
    });
  });
})();
