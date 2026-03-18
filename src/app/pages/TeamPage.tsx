import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Github, Globe, Server, Cpu, Layout, Cloud, ChevronRight, Moon, Sun } from 'lucide-react';
import { useState } from 'react';

interface TeamPageProps {
  onNavigate: (page: string, params?: any) => void;
}

interface Developer {
  id: string;
  name: string;
  role: string;
  tagline: string;
  avatarUrl?: string; // 프로필 사진 필드
  roleIcon: React.ElementType;
  roleColor: string;
  dayRoleColor: string;
  cardBgFocus: string; // 카드 호버 시 뒷배경 빛나는 색상 추가
  dayCardBgFocus: string;
  glowColor: string;
  dayGlowColor: string;
  areas: string[];
  stack: string[];
  highlights: { text: string; metric?: string }[];
  github?: string;
}

const TEAM: Developer[] = [
  {
    id: 'A',
    name: '팀원 A',
    role: '인프라 & 보안',
    tagline: '보이지 않는 방패를 만드는 인프라 마스터',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Felix&backgroundColor=ffdfbf', // 임시 아바타
    roleIcon: Cloud,
    // 우주 테마: 태양풍 오렌지
    roleColor: 'bg-orange-500/20 text-orange-300 border-orange-500/40',
    // 멍냥트립 디자인 시스템 적용 (Point Color)
    dayRoleColor: 'bg-brand-point/10 text-brand-point border-brand-point/30',
    cardBgFocus: 'group-hover:bg-orange-950/40 border-white/5 group-hover:border-orange-500/30',
    dayCardBgFocus: 'group-hover:bg-white border-border group-hover:border-brand-point/30',
    glowColor: 'hover:shadow-[0_0_40px_rgba(249,115,22,0.15)]',
    dayGlowColor: 'hover:shadow-md hover:shadow-brand-point/10',
    areas: ['DevOps', 'Security', 'Backend'],
    stack: ['AWS EC2', 'GitHub Actions', 'Docker', 'Spring Security', 'JWT', 'OAuth2.0'],
    highlights: [
      { text: 'GitHub Actions CI/CD 파이프라인 구축 — main 브랜치 push 시 자동 배포' },
      { text: 'Spring Security + JWT 인증 시스템 구현 (로그인/회원가입)' },
      { text: 'Docker Compose 기반 PostgreSQL + Redis 컨테이너 구축' },
      { text: 'Google/Kakao OAuth2.0 소셜 로그인 구현' },
      { text: '빌드 실패 시 기존 컨테이너 유지 롤백 전략 적용' },
    ],
    github: 'https://github.com',
  },
  {
    id: 'B',
    name: '팀원 B',
    role: '백엔드 아키텍트',
    tagline: '안정적인 데이터의 뼈대를 세우는 설계자',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Liliana&backgroundColor=b6e3f4',
    roleIcon: Server,
    // 우주 테마: 딥 스페이스 블루
    roleColor: 'bg-blue-500/20 text-blue-300 border-blue-500/40',
    // 멍냥트립 디자인 시스템 적용 (Primary)
    dayRoleColor: 'bg-primary/10 text-primary border-primary/30',
    cardBgFocus: 'group-hover:bg-blue-950/40 border-white/5 group-hover:border-blue-500/30',
    dayCardBgFocus: 'group-hover:bg-white border-border group-hover:border-primary/30',
    glowColor: 'hover:shadow-[0_0_40px_rgba(59,130,246,0.15)]',
    dayGlowColor: 'hover:shadow-[0_2px_15px_rgba(227,99,148,0.1)]',
    areas: ['Backend', 'Database', 'API Design'],
    stack: ['Spring Boot', 'JPA/Hibernate', 'PostgreSQL', 'PostGIS', 'Redis', 'Swagger'],
    highlights: [
      { text: 'ERD 전체 설계 — 8개 핵심 엔티티 구성' },
      { text: 'PostGIS ST_DWithin 기반 초고속 위치 기반 장소 검색' },
      { text: 'AOP 기반 공통 예외처리 및 응답 포맷 통일' },
      { text: 'Wishlist / Review / Feed / Visit 도메인 비즈니스 로직 작성' },
      { text: 'Redis 캐싱 전면 도입 체감응답속도 향상' },
    ],
    github: 'https://github.com',
  },
  {
    id: 'C',
    name: '팀원 C',
    role: 'AI 엔지니어',
    tagline: '데이터에 생명을 불어넣는 AI 마술사',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Aidan&backgroundColor=c0aede',
    roleIcon: Cpu,
    // 우주 테마: 성운 바이올렛 (Nebula)
    roleColor: 'bg-purple-500/20 text-purple-300 border-purple-500/40',
    // 멍냥트립 디자인 시스템 적용 (Secondary)
    dayRoleColor: 'bg-secondary/10 text-secondary border-secondary/30',
    cardBgFocus: 'group-hover:bg-purple-950/40 border-white/5 group-hover:border-purple-500/30',
    dayCardBgFocus: 'group-hover:bg-white border-border group-hover:border-secondary/30',
    glowColor: 'hover:shadow-[0_0_40px_rgba(168,85,247,0.15)]',
    dayGlowColor: 'hover:shadow-md hover:shadow-secondary/10',
    areas: ['AI/ML', 'Backend', 'External API'],
    stack: ['Spring AI', 'Google Gemini', 'pgvector', 'Weather API', 'Kakao 알림톡'],
    highlights: [
      { text: 'Spring AI + Gemini 기반 AI 맞춤형 산책 코멘트 생성' },
      { text: 'pgvector 활용 리뷰 의미론적 검색(Semantic Search)' },
      { text: '날씨 API 실시간 연동 환상적인 산책 시간 추천' },
      { text: '카카오 알림톡 자동화 발송 봇 구현' },
    ],
    github: 'https://github.com',
  },
  {
    id: 'D',
    name: '팀원 D',
    role: '프론트/데이터 닌자',
    tagline: '경험을 빚어내고 데이터를 수확하는 닌자',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Jocelyn&backgroundColor=ffdfbf',
    roleIcon: Layout,
    // 우주 테마: 안드로메다 핑크
    roleColor: 'bg-pink-500/20 text-pink-300 border-pink-500/40',
    // 멍냥트립 디자인 시스템 적용 (Primary)
    dayRoleColor: 'bg-primary/10 text-primary border-primary/30',
    cardBgFocus: 'group-hover:bg-pink-950/40 border-white/5 group-hover:border-pink-500/30',
    dayCardBgFocus: 'group-hover:bg-white border-border group-hover:border-primary/30',
    glowColor: 'hover:shadow-[0_0_40px_rgba(236,72,153,0.15)]',
    dayGlowColor: 'hover:shadow-[0_2px_15px_rgba(227,99,148,0.1)]',
    areas: ['Frontend', 'Backend', 'Data Pipeline'],
    stack: ['React', 'TypeScript', 'Tailwind', 'Vite', 'Spring Boot', 'Naver API'],
    highlights: [
      { text: '17개 모바일 퍼스트 프론트엔드 UI/UX 구현', metric: '17 pages' },
      { text: '3,400+ 데이터 이중 교차검증 수집 파이프라인 개발', metric: '3.4K' },
      { text: '지도 SDK 기반 실좌표 핑 + 마커 클러스터링' },
      { text: '데이터셋 간 중복 제거 및 지번 컨텍스트 분리 알고리즘' },
    ],
    github: 'https://github.com',
  },
];

// 간단한 별 생성기 유틸리티
const STARS = Array.from({ length: 50 }).map((_, i) => ({
  id: i,
  top: `${Math.random() * 100}%`,
  left: `${Math.random() * 100}%`,
  size: Math.random() * 3 + 1,
  delay: Math.random() * 5,
  duration: Math.random() * 3 + 2,
}));

export function TeamPage({ onNavigate }: TeamPageProps) {
  const [isNightMode, setIsNightMode] = useState(true);

  return (
    <div className={`min-h-screen transition-colors duration-700 flex justify-center ${isNightMode ? 'bg-[#0A0C14] selection:bg-purple-500/30 text-slate-200' : 'bg-slate-100 selection:bg-primary/20 text-foreground'}`}>
      <div className={`w-full max-w-[600px] relative pb-16 font-sans overflow-hidden transition-colors duration-700 ${isNightMode ? 'bg-[#0A0C14]' : 'bg-background shadow-[0_0_30px_rgba(227,99,148,0.1)] border-x border-border/50'}`}>

        {/* Background Elements */}
        <div className="absolute inset-0 pointer-events-none transition-opacity duration-1000">
        <AnimatePresence>
          {isNightMode && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 1 }}
              className="absolute inset-0 z-0"
            >
              {/* 별(Stars) */}
              {STARS.map((star) => (
                <motion.div
                  key={star.id}
                  initial={{ opacity: 0.1, scale: 0.8 }}
                  animate={{ opacity: [0.1, 0.8, 0.1], scale: [0.8, 1, 0.8] }}
                  transition={{
                    duration: star.duration,
                    repeat: Infinity,
                    delay: star.delay,
                    ease: "easeInOut"
                  }}
                  className="absolute rounded-full bg-white"
                  style={{
                    top: star.top,
                    left: star.left,
                    width: star.size,
                    height: star.size,
                    boxShadow: `0 0 ${star.size * 2}px rgba(255,255,255,0.8)`
                  }}
                />
              ))}

              {/* 오로라 / 가스 구름 (은은한 그라데이션) */}
              <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-indigo-500/10 blur-[150px] rounded-full mix-blend-screen animate-pulse" style={{ animationDuration: '8s' }} />
              <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-purple-600/10 blur-[130px] rounded-full mix-blend-screen animate-pulse" style={{ animationDuration: '10s', animationDelay: '2s' }} />
              <div className="absolute top-[30%] left-[40%] w-[40%] h-[40%] bg-blue-500/5 blur-[120px] rounded-full mix-blend-screen animate-pulse" style={{ animationDuration: '12s', animationDelay: '4s' }} />
            </motion.div>
          )}
        </AnimatePresence>
        <AnimatePresence>
          {!isNightMode && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 1 }}
              className="absolute inset-0 z-0 bg-background"
            >
              {/* 단순하고 깔끔한 배경 (멍냥트립 가이드라인) - 글로우 효과 최소화 */}
              <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-primary/5 blur-[100px] rounded-full mix-blend-multiply" />
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Header */}
      <motion.div
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className={`sticky top-0 z-40 backdrop-blur-xl border-b px-4 py-3 flex items-center justify-between transition-colors duration-500 ${isNightMode ? 'bg-[#0A0C14]/60 border-white/5' : 'bg-white/80 border-border shadow-sm'}`}
      >
        <div className="flex items-center gap-4">
          <button
            onClick={() => onNavigate('home')}
            className={`p-2 rounded-full transition-all active:scale-95 ${isNightMode ? 'bg-white/5 hover:bg-white/15 text-slate-300' : 'bg-white hover:bg-muted border border-border text-foreground shadow-sm'}`}
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className={`text-lg font-bold tracking-tight drop-shadow-sm ${isNightMode ? 'text-white' : 'text-foreground'}`}>Space Makers</h1>
            <p className={`text-[10px] font-bold tracking-widest uppercase flex items-center gap-1 ${isNightMode ? 'text-indigo-300/80' : 'text-primary'}`}>
              MeongNyang Crew
            </p>
          </div>
        </div>

        {/* Theme Toggle */}
        <button
          onClick={() => setIsNightMode(!isNightMode)}
          className={`p-2 rounded-full transition-all duration-300 ${isNightMode ? 'bg-indigo-500/20 text-indigo-300 hover:bg-indigo-500/30' : 'bg-white text-brand-point hover:bg-muted border border-border shadow-sm active:scale-95'}`}
          aria-label="Toggle theme"
        >
          {isNightMode ? <Sun size={20} /> : <Moon size={20} />}
        </button>
      </motion.div>

      {/* Hero Section */}
      <div className="relative px-5 pt-12 pb-10 text-center z-10">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          <div className={`inline-flex items-center gap-2 border px-4 py-1.5 rounded-full text-xs font-bold mb-6 transition-colors duration-500 ${isNightMode ? 'bg-indigo-500/10 border-indigo-500/20 text-indigo-300 shadow-[0_0_20px_rgba(99,102,241,0.2)]' : 'bg-primary/10 border-primary/20 text-primary'}`}>
            <Globe size={14} /> Universe of Travel
          </div>
          <h2 className={`text-3xl md:text-4xl font-extrabold text-transparent bg-clip-text mb-4 tracking-tight transition-colors duration-500 ${isNightMode ? 'bg-gradient-to-br from-indigo-100 via-white to-purple-200 drop-shadow-md' : 'bg-gradient-to-br from-primary via-secondary to-brand-point'}`}>
            The Four Luminous Nebulae
          </h2>
          <p className={`text-sm leading-relaxed max-w-sm mx-auto font-medium transition-colors duration-500 ${isNightMode ? 'text-indigo-200/70' : 'text-muted-foreground'}`}>
            3,400개의 데이터를 넘어선 새로운 우주,<br />
            {isNightMode ? '어둠 속에서 찬란하게 피어나는 우리들의 여정' : '사용자를 위해 끊임없이 고민하는 우리들의 여정'}
          </p>

          <div className="grid grid-cols-3 gap-3 mt-10 max-w-md mx-auto">
            {[
              { label: 'Constellations', value: '17+' },
              { label: 'Nebulas Found', value: '3.4K' },
              { label: 'Light Years', value: '4 Wks' },
            ].map((s, i) => (
              <motion.div
                key={s.label}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 + (i * 0.1) }}
                className={`border rounded-2xl py-4 backdrop-blur-md transition-colors duration-500 ${isNightMode ? 'bg-white/[0.03] border-white/[0.05] shadow-lg' : 'bg-card border-border shadow-sm'}`}
              >
                <div className={`text-2xl font-black mb-1 transition-colors duration-500 ${isNightMode ? 'text-white drop-shadow-sm' : 'text-foreground'}`}>{s.value}</div>
                <div className={`text-[9px] font-bold uppercase tracking-widest transition-colors duration-500 ${isNightMode ? 'text-indigo-300/80' : 'text-muted-foreground'}`}>{s.label}</div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Stack Marquee */}
      <div className={`w-full overflow-hidden border-y py-3 mb-10 z-10 relative transition-colors duration-500 ${isNightMode ? 'border-white/[0.03] bg-white/[0.01]' : 'border-border bg-white'}`}>
        <div className="flex gap-6 items-center w-max animate-[marquee_20s_linear_infinite] px-4">
          {['React 18', 'TypeScript', 'Tailwind CSS', 'Spring Boot 3', 'PostgreSQL', 'PostGIS', 'Redis', 'AWS EC2', 'Docker', 'GitHub Actions', 'Spring AI', 'Gemini'].map((t, i) => (
            <span key={i} className={`text-xs font-bold whitespace-nowrap uppercase tracking-widest transition-colors duration-500 ${isNightMode ? 'text-slate-500/60' : 'text-muted-foreground/60'}`}>
              {t} <span className={`${isNightMode ? 'text-slate-700/50' : 'text-border'} ml-6`}>•</span>
            </span>
          ))}
        </div>
      </div>

      {/* Developer Cards */}
      <div className="px-5 space-y-8 z-10 relative max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <h3 className={`text-lg flex items-center gap-2 font-black text-transparent bg-clip-text bg-gradient-to-r tracking-widest uppercase transition-colors duration-500 ${isNightMode ? 'from-indigo-200 to-purple-300' : 'from-primary to-secondary'}`}>
            Galaxy Navigators
          </h3>
          <div className={`h-px flex-1 bg-gradient-to-r ${isNightMode ? 'from-indigo-500/30' : 'from-primary/30'} to-transparent transition-colors duration-500`}></div>
        </div>

        {TEAM.map((dev, idx) => (
          <motion.div
            key={dev.id}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + (0.15 * idx), duration: 0.6, ease: "easeOut" }}
            className={`group backdrop-blur-[20px] rounded-[2.5rem] border overflow-hidden transition-all duration-500 ease-out hover:-translate-y-1 ${isNightMode ? `bg-white/[0.02] ${dev.cardBgFocus} ${dev.glowColor}` : `bg-card ${dev.dayCardBgFocus} ${dev.dayGlowColor} shadow-sm border-border`}`}
          >
            <div className="p-1">
              <div className={`rounded-[2.25rem] p-6 relative overflow-hidden transition-colors duration-500 border ${isNightMode ? 'bg-[#0A0C14]/40 border-transparent' : 'bg-white border-white'}`}>
                {/* 몽환적 배경 포인트 글레어 효과 */}
                {isNightMode && (
                  <div className={`absolute -right-20 -top-20 w-40 h-40 ${dev.roleColor.split(' ')[0]} blur-[80px] rounded-full opacity-0 group-hover:opacity-40 transition-opacity duration-700 pointer-events-none`} />
                )}

                {/* Profile Header Block */}
                <div className="flex flex-col md:flex-row gap-5 mb-6 relative z-10">
                  {/* Avatar Container */}
                  <div className="relative shrink-0 flex self-start">
                    <div className={`w-24 h-24 rounded-[2rem] overflow-hidden border relative z-10 group-hover:border-opacity-50 transition-colors duration-500 ${isNightMode ? 'bg-white/5 border-white/10' : 'bg-muted border-border/50'}`}>
                      {dev.avatarUrl ? (
                        <img src={dev.avatarUrl} alt={dev.name} className="w-full h-full object-cover group-hover:scale-110 group-hover:rotate-2 transition-transform duration-700 ease-out" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-500 font-bold text-2xl">
                          {dev.id}
                        </div>
                      )}
                    </div>
                    {/* Floating Tech Icon */}
                    <motion.div
                      whileHover={{ scale: 1.15, rotate: 10 }}
                      className={`absolute -bottom-3 -right-3 w-11 h-11 rounded-2xl flex items-center justify-center border z-20 backdrop-blur-md shadow-xl transition-colors duration-500 ${isNightMode ? `border-white/20 ${dev.roleColor}` : dev.dayRoleColor}`}
                    >
                      <dev.roleIcon size={20} className={isNightMode ? "drop-shadow-lg" : ""} />
                    </motion.div>
                  </div>

                  {/* Info Container */}
                  <div className="flex-1 pt-1">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-3 mb-1.5">
                          <h3 className={`text-2xl font-extrabold tracking-tight transition-all ${isNightMode ? 'text-white drop-shadow-md group-hover:text-transparent group-hover:bg-clip-text group-hover:bg-gradient-to-r group-hover:from-white group-hover:to-slate-300' : 'text-foreground group-hover:text-primary'}`}>{dev.name}</h3>
                          <span className={`text-[9px] font-black px-2 py-0.5 border rounded-lg uppercase tracking-widest shadow-inner transition-colors duration-500 ${isNightMode ? dev.roleColor : dev.dayRoleColor}`}>
                            Sector {dev.id}
                          </span>
                        </div>
                        <p className={`text-sm font-bold tracking-wide transition-colors duration-500 ${isNightMode ? 'text-slate-300' : 'text-card-foreground'}`}>{dev.role}</p>
                      </div>
                      {dev.github && (
                        <a
                          href={dev.github}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={`p-3 rounded-2xl border transition-all transform hover:rotate-12 hover:-translate-y-1 shadow-sm ${isNightMode ? 'bg-white/5 border-white/5 hover:bg-white/10 hover:border-white/10 text-slate-400 hover:text-white' : 'bg-muted/50 border-border hover:bg-muted text-muted-foreground hover:text-foreground'}`}
                        >
                          <Github size={20} />
                        </a>
                      )}
                    </div>
                    {/* Tagline Box */}
                    <motion.div
                      whileHover={{ scale: 1.02, x: 5 }}
                      transition={{ type: "spring", stiffness: 400, damping: 10 }}
                      className={`mt-4 border px-4 py-2.5 rounded-2xl inline-block max-w-[95%] cursor-default transition-colors duration-500 ${isNightMode ? 'bg-white/[0.03] border-white/5 shadow-inner' : 'bg-muted/50 border-border shadow-sm'}`}
                    >
                      <p className={`text-[13px] font-medium italic transition-colors duration-500 ${isNightMode ? 'text-slate-300' : 'text-card-foreground'}`}>"{dev.tagline}"</p>
                    </motion.div>
                  </div>
                </div>

                {/* Badges */}
                <div className="space-y-3 mb-7 relative z-10">
                  <div className="flex flex-wrap gap-2">
                    {dev.areas.map(area => (
                      <motion.span
                        key={area}
                        whileHover={{ scale: 1.1, y: -2 }}
                        transition={{ type: "spring", stiffness: 400, damping: 10 }}
                        className={`text-[10px] border px-3 py-1.5 rounded-xl font-bold tracking-widest cursor-default inline-block uppercase transition-colors duration-500 ${isNightMode ? 'bg-white/[0.04] border-white/5 text-slate-300 drop-shadow-sm' : 'bg-muted/50 border-border text-muted-foreground'}`}
                      >
                        {area}
                      </motion.span>
                    ))}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {dev.stack.map(tech => (
                      <motion.span
                        key={tech}
                        whileHover={{ scale: 1.1, y: -2 }}
                        transition={{ type: "spring", stiffness: 400, damping: 10 }}
                        className={`text-[10px] px-3 py-1.5 rounded-xl font-black border backdrop-blur-md cursor-default inline-block uppercase tracking-wide transition-colors duration-500 ${isNightMode ? `${dev.roleColor} shadow-sm` : dev.dayRoleColor}`}
                      >
                        {tech}
                      </motion.span>
                    ))}
                  </div>
                </div>

                {/* Highlights List */}
                <div className={`pt-5 border-t space-y-3.5 relative z-10 transition-colors duration-500 ${isNightMode ? 'border-white/[0.05]' : 'border-border'}`}>
                  {dev.highlights.map((h, i) => (
                    <motion.div
                      key={i}
                      whileHover={{ x: 8 }}
                      transition={{ type: "spring", stiffness: 400, damping: 15 }}
                      className="flex items-start gap-4 group/item cursor-default"
                    >
                      <div className={`mt-0.5 w-6 h-6 rounded-full flex items-center justify-center border transition-all shrink-0 ${isNightMode ? 'bg-white/[0.03] border-white/5 group-hover/item:border-white/20 shadow-inner' : 'bg-muted border-border group-hover/item:border-primary/30 group-hover/item:text-primary group-hover/item:bg-primary/5'}`}>
                        <ChevronRight size={12} className={`transition-colors ${isNightMode ? 'text-slate-500' : 'text-muted-foreground'}`} />
                      </div>
                      <p className={`text-[13px] leading-relaxed font-medium transition-colors mt-px ${isNightMode ? 'text-slate-400 group-hover/item:text-slate-200' : 'text-muted-foreground group-hover/item:text-foreground'}`}>
                        {h.text}
                        {h.metric && (
                          <span className={`ml-2 text-[9px] font-black px-1.5 py-0.5 rounded border uppercase align-middle whitespace-nowrap inline-block transition-colors duration-500 ${isNightMode ? `${dev.roleColor} shadow-sm` : dev.dayRoleColor}`}>
                            {h.metric}
                          </span>
                        )}
                      </p>
                    </motion.div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* Footer */}
      <div className="mt-20 pb-10 px-4 text-center z-10 relative">
        <motion.div
          whileHover={{ rotate: 180 }}
          transition={{ duration: 0.8, ease: "backOut" }}
          className={`inline-flex items-center justify-center w-14 h-14 rounded-full border mb-5 transition-colors duration-500 ${isNightMode ? 'border-indigo-500/20 bg-indigo-500/10 shadow-[0_0_30px_rgba(99,102,241,0.2)]' : 'border-primary/20 bg-primary/10'}`}
        >
          <Globe size={24} className={isNightMode ? 'text-indigo-300' : 'text-primary'} />
        </motion.div>
        <p className={`font-extrabold text-transparent bg-clip-text bg-gradient-to-r tracking-widest uppercase mb-1 transition-colors duration-500 ${isNightMode ? 'from-indigo-200 to-purple-300' : 'from-primary to-brand-point'}`}>MeongNyang Space</p>
        <p className={`text-[11px] font-medium mb-6 uppercase tracking-widest transition-colors duration-500 ${isNightMode ? 'text-indigo-300/60' : 'text-muted-foreground'}`}>Endless Journey • 2026</p>
        <a
          href="https://github.com/ykimek-elly/MeongNyangTrip_2.0"
          target="_blank"
          rel="noopener noreferrer"
          className={`inline-flex items-center gap-2 text-xs font-bold border px-5 py-2.5 rounded-full transition-all ${isNightMode ? 'text-slate-300 hover:text-white bg-white/5 hover:bg-white/10 border-white/10 shadow-lg hover:shadow-[0_0_20px_rgba(255,255,255,0.1)]' : 'text-foreground hover:text-primary bg-card/80 hover:bg-white border-border shadow-sm'}`}
        >
          <Github size={16} /> Explore Repository
        </a>
      </div>

      <style>{`
        @keyframes marquee {
          0% { transform: translateX(0%); }
          100% { transform: translateX(-50%); }
        }
        .animate-\\[marquee_20s_linear_infinite\\] {
          animation: marquee 20s linear infinite;
        }
      `}</style>
    </div>
    </div>
  );
}
