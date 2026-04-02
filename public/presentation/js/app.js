/* ════════════════════════════════════
   상태 & 상수
════════════════════════════════════ */
const TOTAL      = SLIDES.length;
const STEP_DELAY = 850;   // ms 각 스텝 사이 간격
const INIT_DELAY = 550;   // ms 슬라이드 진입 후 첫 스텝까지 대기
const HOLD_DELAY = 1600;  // ms 마지막 스텝 후 split 전환 전 대기

let cur       = 0;
let autoTimer = null;
const app     = document.getElementById('app');

/* ════════════════════════════════════
   타이머
════════════════════════════════════ */
function clearAuto() {
  if (autoTimer) { clearTimeout(autoTimer); autoTimer = null; }
  clearActions();
}

/* ════════════════════════════════════
   스텝 표시
════════════════════════════════════ */
function revealStep(slideIdx, step) {
  const el = app.children[slideIdx];
  if (!el) return;
  const items = [...el.querySelectorAll('[data-step="' + step + '"]')];
  items.forEach((item, i) => {
    setTimeout(() => item.classList.add('visible'), i * 60);
  });
  updateDots(slideIdx, step);
  updateBadge(step);
}

function revealAll(slideIdx) {
  const el = app.children[slideIdx];
  if (!el) return;
  el.querySelectorAll('.step-item').forEach(i => i.classList.add('visible'));
  const maxSteps = SLIDES[slideIdx].steps || 1;
  updateDots(slideIdx, maxSteps);
  updateBadge(maxSteps);
  stopPulse(slideIdx);
}

function resetSteps(slideIdx) {
  const el = app.children[slideIdx];
  if (!el) return;
  el.querySelectorAll('.step-item').forEach(i => i.classList.remove('visible'));
  updateDots(slideIdx, 0);
}

/* ════════════════════════════════════
   인디케이터
════════════════════════════════════ */
function updateDots(slideIdx, step) {
  const el = app.children[slideIdx];
  if (!el) return;
  const ind = el.querySelector('.step-indicator');
  if (!ind) return;
  const total = SLIDES[slideIdx].steps || 1;
  ind.innerHTML = '';
  for (let i = 1; i <= total; i++) {
    const d = document.createElement('div');
    d.className = 'sdot' + (i < step ? ' done' : i === step ? ' cur' : '');
    ind.appendChild(d);
  }
}

function stopPulse(slideIdx) {
  const el = app.children[slideIdx];
  if (!el) return;
  const p = el.querySelector('.play-pulse');
  if (p) p.classList.add('stopped');
}

/* ════════════════════════════════════
   자동 재생
════════════════════════════════════ */
function autoPlay(slideIdx) {
  clearAuto();
  if (SLIDES[slideIdx].type !== 'full') return;

  const maxSteps = SLIDES[slideIdx].steps || 1;
  let step = 0;

  function tick() {
    step++;
    revealStep(slideIdx, step);

    if (step < maxSteps) {
      autoTimer = setTimeout(tick, STEP_DELAY);
    } else {
      stopPulse(slideIdx);
      autoTimer = setTimeout(() => {
        if (slideIdx + 1 < TOTAL) transitionTo(slideIdx + 1, 1);
      }, HOLD_DELAY);
    }
  }

  autoTimer = setTimeout(tick, INIT_DELAY);
}

/* ════════════════════════════════════
   슬라이드 전환
════════════════════════════════════ */
function transitionTo(next, dir) {
  if (next < 0 || next >= TOTAL) return;
  clearAuto();

  const curEl  = app.children[cur];
  const nextEl = app.children[next];

  curEl.classList.remove('active');
  if (dir >= 0) {
    curEl.classList.add(SLIDES[cur].type === 'full' ? 'exit-up' : 'exit-left');
  } else {
    curEl.style.cssText = 'opacity:0;transform:translateY(20px)';
  }
  setTimeout(() => {
    curEl.classList.remove('exit-up', 'exit-left');
    curEl.style.cssText = '';
  }, 560);

  cur = next;

  if (SLIDES[cur].type === 'full') {
    resetSteps(cur);
  }

  nextEl.classList.add('active');
  updateNav();

  if (SLIDES[cur].type === 'full') {
    autoPlay(cur);
  } else {
    activateSplit(cur);
  }
}

/* ════════════════════════════════════
   iframe 자동 동작
════════════════════════════════════ */
let actionTimers = [];

function clearActions() {
  actionTimers.forEach(t => clearTimeout(t));
  actionTimers = [];
}

function activateSplit(slideIdx) {
  clearActions();
  const slide = SLIDES[slideIdx];
  if (!slide.navPage) return;

  const t0 = setTimeout(() => {
    const iframe = getIframe(slideIdx);
    if (!iframe) return;
    iframe.contentWindow.postMessage(
      { type: 'PRESENTATION_NAV', page: slide.navPage },
      'https://meongnyangtrip.duckdns.org'
    );
  }, 300);
  actionTimers.push(t0);

  if (!slide.actions) return;
  slide.actions.forEach(({ delay, scroll }) => {
    const t = setTimeout(() => {
      scrollIframe(slideIdx, scroll);
    }, delay);
    actionTimers.push(t);
  });
}

function getIframe(slideIdx) {
  return app.children[slideIdx]?.querySelector('.site-iframe') || null;
}

function scrollIframe(slideIdx, amount, attempt = 0) {
  if (attempt > 8) return;
  const iframe = getIframe(slideIdx);
  if (!iframe?.contentWindow) return;
  try {
    const main = iframe.contentWindow.document.getElementById('main-scroll');
    if (main) {
      main.scrollTo({ top: amount, behavior: 'smooth' });
    } else if (attempt < 8) {
      setTimeout(() => scrollIframe(slideIdx, amount, attempt + 1), 250);
    }
  } catch(e) { /* cross-origin 예외 무시 */ }
}

/* ════════════════════════════════════
   네비게이션 버튼
════════════════════════════════════ */
function navNext() {
  clearAuto();
  if (SLIDES[cur].type === 'full') {
    revealAll(cur);
    setTimeout(() => {
      if (cur + 1 < TOTAL) transitionTo(cur + 1, 1);
    }, 250);
  } else {
    if (cur + 1 < TOTAL) transitionTo(cur + 1, 1);
  }
}

function navBack() {
  clearAuto();
  if (cur - 1 >= 0) transitionTo(cur - 1, -1);
}

function goTo(i) {
  if (i === cur) return;
  clearAuto();
  transitionTo(i, i > cur ? 1 : -1);
}

/* ════════════════════════════════════
   네비 UI 갱신
════════════════════════════════════ */
function updateBadge(step) {
  const s     = SLIDES[cur];
  const badge = document.getElementById('typeBadge');
  if (s.type === 'full') {
    const total  = s.steps || 1;
    const isDone = step >= total;
    badge.textContent = isDone
      ? `▶ 완료 — Space 로 계속`
      : `▶ 자동재생  ${step || 0} / ${total}`;
    badge.classList.toggle('playing', !isDone);
  } else {
    badge.textContent = 'Space → 다음 섹션';
    badge.classList.remove('playing');
  }
}

function updateNav() {
  document.getElementById('progressFill').style.width = ((cur+1)/TOTAL*100)+'%';
  document.getElementById('prevBtn').disabled = cur === 0;
  document.getElementById('nextBtn').disabled = cur === TOTAL - 1;
  document.querySelectorAll('.dot').forEach((d,i) => d.classList.toggle('active', i===cur));
  updateBadge(0);
}

/* ════════════════════════════════════
   렌더링
════════════════════════════════════ */
function render() {
  app.innerHTML = '';
  SLIDES.forEach((s, i) => {
    const el = document.createElement('div');
    if (s.type === 'full') {
      el.className = 'full-slide' + (i === cur ? ' active' : '');
      el.innerHTML = s.html;
    } else {
      el.className = 'split-slide' + (i === cur ? ' active' : '');
      el.innerHTML = `
        <div class="split-left">
          <div class="split-left-header">
            <div class="logo-dot"></div>
            <span class="logo">멍냥트립 2.0</span>
            <span class="counter">${Math.floor(i/2)+1} / ${TOTAL/2}</span>
          </div>
          <div class="split-left-body">${s.html}</div>
        </div>
        <div class="split-right">
          <div class="browser-bar">
            <div class="b-dots">
              <div class="b-dot r"></div><div class="b-dot y"></div><div class="b-dot g"></div>
            </div>
            <div class="b-url"><i data-lucide="lock"></i>${s.iframe}</div>
          </div>
          <iframe class="site-iframe" src="${s.iframe}" title="멍냥트립 2.0" loading="lazy"></iframe>
        </div>
      `;
    }
    app.appendChild(el);
  });

  // dot 생성
  const dotsEl = document.getElementById('dots');
  dotsEl.innerHTML = '';
  SLIDES.forEach((s,i) => {
    const d = document.createElement('div');
    d.className = 'dot' + (s.type==='split' ? ' split-dot' : '') + (i===cur ? ' active' : '');
    d.onclick = () => goTo(i);
    dotsEl.appendChild(d);
  });

  updateNav();

  // Lucide 아이콘 초기화 (innerHTML 삽입 후 반드시 호출)
  lucide.createIcons();

  if (SLIDES[cur].type === 'full') autoPlay(cur);
}

/* ════════════════════════════════════
   키보드
════════════════════════════════════ */
document.addEventListener('keydown', e => {
  if (e.key === ' ' || e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    e.preventDefault(); navNext();
  }
  if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    e.preventDefault(); navBack();
  }
});

render();
