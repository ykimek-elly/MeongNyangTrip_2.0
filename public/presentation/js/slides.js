/* ════════════════════════════════════
   슬라이드 데이터 (모든 이모지 → Lucide 아이콘)
════════════════════════════════════ */
const SLIDES = [

  /* ── 0: 인트로 FULL (3 steps) ── */
  { type:'full', steps:3, html:`
    <span class="intro-emoji step-item icon-pink" data-step="1"><i data-lucide="paw-print"></i></span>
    <div class="full-title step-item" data-step="1"><span class="grad">멍냥트립 2.0</span></div>
    <div class="full-sub step-item" data-step="1">반려동물과 함께하는 스마트 여행 플랫폼<br>공공 API × AI × 소셜 커뮤니티의 통합</div>
    <div class="stat-row diagram step-item" data-step="2">
      <div class="stat-box"><span class="stat-num">1,200+</span><span class="stat-label">반려동물 동반 장소</span></div>
      <div class="stat-box"><span class="stat-num">21</span><span class="stat-label">전체 페이지</span></div>
      <div class="stat-box"><span class="stat-num">4</span><span class="stat-label">팀원</span></div>
      <div class="stat-box"><span class="stat-num">AWS</span><span class="stat-label">EC2 운영 배포</span></div>
    </div>
    <div class="tech-row step-item" data-step="3">
      <span class="ttag b">React 18</span><span class="ttag g">Spring Boot 3</span>
      <span class="ttag p">PostgreSQL + PostGIS</span><span class="ttag o">AWS EC2 / S3</span>
      <span class="ttag pk">Google Gemini AI</span><span class="ttag c">Redis 캐싱</span>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 1: 인트로 SPLIT ── */
  { type:'split', label:'서비스 소개', iframe:'https://meongnyangtrip.duckdns.org', navPage:'home',
    actions:[
      { delay:1200, scroll:320 },
      { delay:3800, scroll:0   },
    ], html:`
    <div class="split-h2">멍냥트립 2.0</div>
    <div class="split-sub">반려동물 동반 여행의 모든 것을 한 곳에서</div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-blue"><i data-lucide="map"></i></span><div><h4>장소 탐색</h4><p>1,200건 반려동물 동반 장소 — 카카오 지도 + PostGIS 반경 검색</p></div></div>
      <div class="fitem"><span class="fi icon-purple"><i data-lucide="bot"></i></span><div><h4>AI 산책 가이드</h4><p>날씨·펫 정보 기반 Gemini AI 맞춤 코멘트</p></div></div>
      <div class="fitem"><span class="fi icon-orange"><i data-lucide="camera"></i></span><div><h4>라운지 커뮤니티</h4><p>S3 이미지 업로드 · 좋아요 · 댓글 · 산책톡</p></div></div>
      <div class="fitem"><span class="fi icon-green"><i data-lucide="lock"></i></span><div><h4>소셜 로그인</h4><p>Google / Kakao OAuth2 + JWT 인증</p></div></div>
    </div>
  `},

  /* ── 2: 사용자 경험 FULL (6 steps) ── */
  { type:'full', steps:6, html:`
    <div class="tag step-item" data-step="1"><i data-lucide="user"></i> User Experience</div>
    <div class="full-title step-item" data-step="1" style="font-size:42px">사용자 여정 플로우</div>
    <div class="full-sub step-item" data-step="1">로그인부터 AI 추천까지 — 끊김 없는 반려동물 여행 경험</div>
    <div class="flow diagram">
      <div class="flow-node accent step-item" data-step="2">
        <span class="fn-icon icon-green"><i data-lucide="lock"></i></span>
        <span class="fn-title">소셜 로그인</span><span class="fn-sub">Google / Kakao</span>
      </div>
      <span class="flow-arrow step-item" data-step="3">→</span>
      <div class="flow-node step-item" data-step="3">
        <span class="fn-icon icon-orange"><i data-lucide="dog"></i></span>
        <span class="fn-title">펫 등록</span><span class="fn-sub">종·나이·활동량</span>
      </div>
      <span class="flow-arrow step-item" data-step="4">→</span>
      <div class="flow-node step-item" data-step="4">
        <span class="fn-icon icon-blue"><i data-lucide="search"></i></span>
        <span class="fn-title">장소 탐색</span><span class="fn-sub">위치·카테고리 필터</span>
      </div>
      <span class="flow-arrow step-item" data-step="5">→</span>
      <div class="flow-node accent step-item" data-step="5">
        <span class="fn-icon icon-pink"><i data-lucide="heart"></i></span>
        <span class="fn-title">찜 &amp; 리뷰</span><span class="fn-sub">낙관적 업데이트</span>
      </div>
      <span class="flow-arrow step-item" data-step="6">→</span>
      <div class="flow-node step-item" data-step="6">
        <span class="fn-icon icon-purple"><i data-lucide="bot"></i></span>
        <span class="fn-title">AI 추천</span><span class="fn-sub">맞춤 산책 가이드</span>
      </div>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 3: 사용자 경험 SPLIT ── */
  { type:'split', label:'사용자 경험', iframe:'https://meongnyangtrip.duckdns.org', navPage:'list',
    actions:[
      { delay:1200, scroll:400 },
      { delay:3500, scroll:800 },
      { delay:5500, scroll:0   },
    ], html:`
    <div class="split-h2">핵심 UX 기능</div>
    <div class="split-sub">모바일 퍼스트 설계 — 직관적인 반려동물 여행 플로우</div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-blue"><i data-lucide="map"></i></span><div><h4>카카오 지도 통합</h4><p>마커 클러스터링 · 동물병원 반경 탐색 · 길찾기</p></div></div>
      <div class="fitem"><span class="fi icon-blue"><i data-lucide="search"></i></span><div><h4>PostGIS 위치 기반 검색</h4><p>ST_DWithin 반경 검색 + Redis 1시간 캐싱</p></div></div>
      <div class="fitem"><span class="fi icon-pink"><i data-lucide="heart"></i></span><div><h4>찜 목록 낙관적 업데이트</h4><p>즉각 UI 반영 후 API 동기화 · 실패 시 자동 롤백</p></div></div>
      <div class="fitem"><span class="fi icon-green"><i data-lucide="circle-check"></i></span><div><h4>방문 인증 체크인</h4><p>카카오 로컬 API 교차 검증 · 방문 이력 누적</p></div></div>
    </div>
  `},

  /* ── 4: 팀원D FE FULL (5 steps) ── */
  { type:'full', steps:5, html:`
    <div class="tag step-item" data-step="1"><i data-lucide="palette"></i> Frontend &amp; Data Pipeline</div>
    <div class="full-title step-item" data-step="1" style="font-size:40px">FE 아키텍처 &amp; 배치 파이프라인</div>
    <div class="full-sub step-item" data-step="1">팀원D — React 18 · TypeScript · Zustand · Tailwind CSS v4</div>
    <div class="arch diagram">
      <div class="arch-row-d step-item" data-step="2">
        <div class="arch-box-d hl">
          <div class="ab-title"><i data-lucide="monitor"></i> React 18 + TypeScript</div>
          <div class="ab-sub">21개 페이지 · 모바일 퍼스트 · 컴포넌트 분리</div>
        </div>
      </div>
      <div class="arch-conn step-item" data-step="3">↕</div>
      <div class="arch-row-d step-item" data-step="3" style="gap:12px">
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="refresh-cw"></i> Zustand</div><div class="ab-sub">전역 상태 · persist · 낙관적 업데이트</div></div>
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="map-pin"></i> Kakao Maps SDK</div><div class="ab-sub">마커 · 클러스터 · 오버레이</div></div>
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="wand-2"></i> Framer Motion</div><div class="ab-sub">Spring 애니메이션 · 트랜지션</div></div>
      </div>
      <div class="arch-conn step-item" data-step="4">↕</div>
      <div class="arch-row-d step-item" data-step="4">
        <div class="arch-box-d gr">
          <div class="ab-title"><i data-lucide="palette"></i> Tailwind CSS v4 + CSS 변수 디자인 시스템</div>
          <div class="ab-sub">--primary · --brand-point · Spring 트랜지션 · 커스텀 아이콘 팩토리</div>
        </div>
      </div>
      <div class="arch-conn pk step-item" data-step="5">+ 별도 파이프라인 구축 (팀원D)</div>
      <div class="arch-row-d step-item" data-step="5">
        <div class="arch-box-d hl">
          <div class="ab-title"><i data-lucide="settings"></i> 배치 파이프라인 교차배치 구축</div>
          <div class="ab-sub">공공 API 수집 → Naver LCS 교차검증 → Gemini Vision 검증 → DB 적재 → 관리자 검토큐</div>
        </div>
      </div>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 5: 팀원D FE SPLIT ── */
  { type:'split', label:'FE 핵심 기술', iframe:'https://meongnyangtrip.duckdns.org', navPage:'home',
    actions:[
      { delay:1200, scroll:280 },
      { delay:3200, scroll:560 },
      { delay:5000, scroll:0   },
    ], html:`
    <div class="member-card">
      <div class="mn">팀원D — FE &amp; UI/UX &amp; 배치 파이프라인</div>
      <div class="mr">React · TypeScript · Tailwind CSS · Zustand · 데이터 파이프라인</div>
    </div>
    <div class="stags">
      <span class="stag b">React 18</span><span class="stag b">TypeScript</span>
      <span class="stag c">Vite</span><span class="stag g">Tailwind v4</span>
      <span class="stag p">Zustand</span><span class="stag o">Router v7</span>
      <span class="stag pk">Framer Motion</span><span class="stag c">Kakao Maps</span>
    </div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-blue"><i data-lucide="smartphone"></i></span><div><h4>모바일 퍼스트 디자인 시스템</h4><p>CSS 변수 기반 컬러 · max-width 600px · Spring 물리 애니메이션</p></div></div>
      <div class="fitem"><span class="fi icon-cyan"><i data-lucide="refresh-cw"></i></span><div><h4>Zustand Persist 전역 상태</h4><p>찜·펫·위치 localStorage 동기화 · 낙관적 업데이트 + 롤백</p></div></div>
      <div class="fitem"><span class="fi icon-orange"><i data-lucide="settings"></i></span><div><h4>배치 파이프라인 교차배치 구축</h4><p>공공 API 수집 → Naver LCS 교차검증 → Gemini Vision 이미지 검증 → AI 별점 산정 → 관리자 검토큐</p></div></div>
    </div>
  `},

  /* ── 6: 팀원B 피드 FULL (4 steps) ── */
  { type:'full', steps:4, html:`
    <div class="tag step-item" data-step="1"><i data-lucide="message-circle"></i> Feed &amp; Social</div>
    <div class="full-title step-item" data-step="1" style="font-size:42px">피드 &amp; S3 아키텍처</div>
    <div class="full-sub step-item" data-step="1">팀원B — Spring Boot · JPA · AWS S3 · 소셜 피드</div>
    <div class="arch diagram">
      <div class="arch-row-d step-item" data-step="2">
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="smartphone"></i> FE (React)</div><div class="ab-sub">Multipart 이미지 선택 · 미리보기</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="globe"></i> Spring Boot API</div><div class="ab-sub">LoungeController · S3 presigned URL</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="cloud"></i> AWS S3</div><div class="ab-sub">IAM Role · EC2 자격증명</div></div>
      </div>
      <div class="arch-conn step-item" data-step="3">↓ PostgreSQL 저장</div>
      <div class="arch-row-d step-item" data-step="4" style="gap:12px">
        <div class="arch-box-d"><div class="ab-title"><i data-lucide="file-text"></i> 피드 목록</div><div class="ab-sub">페이지네이션 · 좋아요</div></div>
        <div class="arch-box-d"><div class="ab-title"><i data-lucide="message-circle"></i> 댓글</div><div class="ab-sub">실시간 반영</div></div>
        <div class="arch-box-d"><div class="ab-title"><i data-lucide="footprints"></i> 산책톡</div><div class="ab-sub">태그 기반 분류</div></div>
      </div>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 7: 팀원B 피드 SPLIT ── */
  { type:'split', label:'피드 &amp; 소셜', iframe:'https://meongnyangtrip.duckdns.org', navPage:'lounge',
    actions:[
      { delay:1200, scroll:350 },
      { delay:3500, scroll:700 },
      { delay:5500, scroll:0   },
    ], html:`
    <div class="member-card">
      <div class="mn">팀원B — 피드 &amp; 소셜</div>
      <div class="mr">Spring Boot · JPA · AWS S3 · PostgreSQL</div>
    </div>
    <div class="stags">
      <span class="stag g">Spring Boot 3</span><span class="stag b">JPA</span>
      <span class="stag o">AWS S3</span><span class="stag p">PostgreSQL</span><span class="stag c">Multipart</span>
    </div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-blue"><i data-lucide="image"></i></span><div><h4>AWS S3 이미지 업로드</h4><p>EC2 IAM Role 기반 자격증명 · Multipart 직접 업로드</p></div></div>
      <div class="fitem"><span class="fi icon-cyan"><i data-lucide="file-text"></i></span><div><h4>라운지 피드 시스템</h4><p>게시글 CRUD · 페이지네이션 · 좋아요·댓글</p></div></div>
      <div class="fitem"><span class="fi icon-green"><i data-lucide="footprints"></i></span><div><h4>산책톡 커뮤니티</h4><p>태그 기반 게시글 분류 · 팔로우 소셜 피드</p></div></div>
    </div>
  `},

  /* ── 8: 팀원C AI FULL (4 steps) ── */
  { type:'full', steps:4, html:`
    <div class="tag step-item" data-step="1"><i data-lucide="bot"></i> AI Service</div>
    <div class="full-title step-item" data-step="1" style="font-size:42px">AI 파이프라인</div>
    <div class="full-sub step-item" data-step="1">팀원C — Spring AI · Gemini Vision API · 날씨 연동</div>
    <div class="arch diagram">
      <div class="arch-row-d step-item" data-step="2">
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="cloud-sun"></i> 날씨 API</div><div class="ab-sub">기온·날씨 상태</div></div>
        <span class="arch-arrow-d">+</span>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="paw-print"></i> 펫 프로필</div><div class="ab-sub">종·나이·활동량</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="bot"></i> Spring AI</div><div class="ab-sub">프롬프트 생성</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="sparkles"></i> Gemini</div><div class="ab-sub">맞춤 코멘트 생성</div></div>
      </div>
      <div class="arch-conn pk step-item" data-step="3"><i data-lucide="settings"></i> 별도 배치 파이프라인 교차배치 — 팀원D 구축</div>
      <div class="arch-row-d step-item" data-step="4" style="gap:12px">
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="camera"></i> 1,200건 이미지</div><div class="ab-sub">공공 API 장소 원본</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="eye"></i> Gemini Vision</div><div class="ab-sub">이미지 적합성 검증</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="star"></i> AI 별점 산정</div><div class="ab-sub">자동 큐레이션 244건</div></div>
      </div>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 9: 팀원C AI SPLIT ── */
  { type:'split', label:'AI 서비스', iframe:'https://meongnyangtrip.duckdns.org', navPage:'ai-walk-guide',
    actions:[
      { delay:1400, scroll:300 },
      { delay:3500, scroll:0   },
    ], html:`
    <div class="member-card">
      <div class="mn">팀원C — AI &amp; 알림 서비스</div>
      <div class="mr">Spring AI · Gemini Vision API · 날씨 API</div>
    </div>
    <div class="stags">
      <span class="stag p">Spring AI</span><span class="stag b">Gemini Vision</span>
      <span class="stag g">pgvector</span><span class="stag o">날씨 API</span><span class="stag c">배치 처리</span>
    </div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-yellow"><i data-lucide="sparkles"></i></span><div><h4>Gemini Vision 배치 검증</h4><p>1,200건 장소 이미지 적합성 자동 검증 · AI 별점 산정</p></div></div>
      <div class="fitem"><span class="fi icon-yellow"><i data-lucide="cloud-sun"></i></span><div><h4>AI 산책 가이드</h4><p>날씨·기온·펫 정보 기반 Spring AI 맞춤 코멘트 생성</p></div></div>
      <div class="fitem"><span class="fi icon-orange"><i data-lucide="bell"></i></span><div><h4>장소 알림 서비스</h4><p>AI 큐레이션 244건 기반 맞춤 푸시 알림</p></div></div>
    </div>
  `},

  /* ── 10: 팀원A 인프라 FULL (4 steps) ── */
  { type:'full', steps:4, html:`
    <div class="tag step-item" data-step="1"><i data-lucide="rocket"></i> Infrastructure</div>
    <div class="full-title step-item" data-step="1" style="font-size:42px">배포 아키텍처</div>
    <div class="full-sub step-item" data-step="1">팀원A — AWS EC2 · Docker · GitHub Actions CI/CD</div>
    <div class="arch diagram">
      <div class="arch-row-d step-item" data-step="2">
        <div class="arch-box-d bl"><div class="ab-title"><i data-lucide="code-2"></i> GitHub Push</div><div class="ab-sub">feat/frontend → main</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="settings"></i> GitHub Actions</div><div class="ab-sub">빌드 · 테스트 · SSM 배포</div></div>
        <span class="arch-arrow-d">→</span>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="cloud"></i> AWS EC2</div><div class="ab-sub">54.180.22.22</div></div>
      </div>
      <div class="arch-conn step-item" data-step="3">↓ Docker Compose</div>
      <div class="arch-row-d step-item" data-step="4" style="gap:10px">
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="globe"></i> Nginx</div><div class="ab-sub">React SPA + 프록시</div></div>
        <div class="arch-box-d hl"><div class="ab-title"><i data-lucide="coffee"></i> Spring Boot</div><div class="ab-sub">Java 21 Virtual Threads</div></div>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="database"></i> PostgreSQL</div><div class="ab-sub">+ PostGIS</div></div>
        <div class="arch-box-d gr"><div class="ab-title"><i data-lucide="zap"></i> Redis</div><div class="ab-sub">캐시 · 세션</div></div>
      </div>
    </div>
    <div class="step-indicator"></div>
    <div class="play-pulse"><div class="pulse-dot"></div><span>자동재생</span></div>
  `},

  /* ── 11: 팀원A 인프라 SPLIT ── */
  { type:'split', label:'인프라 &amp; 배포', iframe:'https://meongnyangtrip.duckdns.org', navPage:'home',
    actions:[
      { delay:1200, scroll:300 },
      { delay:3000, scroll:0   },
    ], html:`
    <div class="member-card">
      <div class="mn">팀원A — 인프라 &amp; 배포</div>
      <div class="mr">AWS EC2 · Docker · GitHub Actions · Nginx</div>
    </div>
    <div class="stags">
      <span class="stag o">AWS EC2</span><span class="stag b">Docker</span>
      <span class="stag g">GitHub Actions</span><span class="stag p">AWS SSM</span>
      <span class="stag c">Nginx</span><span class="stag pk">Redis 7</span>
    </div>
    <div class="flist">
      <div class="fitem"><span class="fi icon-cyan"><i data-lucide="layers"></i></span><div><h4>Docker 멀티 컨테이너</h4><p>Nginx · Spring Boot · PostgreSQL · Redis 분리 운영</p></div></div>
      <div class="fitem"><span class="fi icon-orange"><i data-lucide="settings"></i></span><div><h4>GitHub Actions CI/CD</h4><p>Push → 자동 빌드 → SSM → EC2 무중단 배포</p></div></div>
      <div class="fitem"><span class="fi icon-yellow"><i data-lucide="zap"></i></span><div><h4>Java 21 Virtual Threads</h4><p>Spring Boot 3 가상 스레드 · 고동시성 처리</p></div></div>
    </div>
  `},
];
