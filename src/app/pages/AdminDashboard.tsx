import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft, Heart, MessageCircle, Send, Eye, EyeOff, Trash2,
  AlertTriangle, BarChart3, Mail, Image as ImageIcon,
  ChevronDown, ChevronUp, Search, Shield, Settings,
  Play, CheckCircle, XCircle, Loader, ImageOff, Star, Database, Wrench, Sparkles, RefreshCw,
  MapPin, ExternalLink, Check, X, Edit3, Activity, Zap, ArrowRight,
  Calendar, Clock, Bell, BellOff, Plus
} from 'lucide-react';
import { useFeedStore, type FeedPost } from '../store/useFeedStore';
import { useAppStore } from '../store/useAppStore';
import { adminApi, type PendingPlaceDto, type BatchStatsDto } from '../api/adminApi';

interface AdminDashboardProps {
  onNavigate: (page: string, params?: any) => void;
}

type TabType = 'overview' | 'batch' | 'places' | 'posts' | 'comments' | 'dms';
type SortType = 'latest' | 'likes' | 'comments' | 'dms' | 'reported';
type PlacesSection = 'pending' | 'noImage' | 'rejected' | 'editActive' | 'create';

export function AdminDashboard({ onNavigate }: AdminDashboardProps) {
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [placesSection, setPlacesSection] = useState<PlacesSection | null>(null);
  const { posts } = useFeedStore();

  const switchToPlaces = (section: PlacesSection) => {
    setPlacesSection(section);
    setActiveTab('places');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Admin Header */}
      <div className="bg-gray-900 text-white sticky top-0 z-50">
        <div className="flex items-center gap-3 px-4 py-3">
          <button onClick={() => onNavigate('home')} className="hover:opacity-70">
            <ArrowLeft size={20} />
          </button>
          <div className="flex items-center gap-2">
            <Shield size={18} className="text-primary" />
            <h1 className="text-base font-bold">관리자 대시보드</h1>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-t border-gray-800 overflow-x-auto scrollbar-hide">
          {([
            { key: 'overview', label: '현황', icon: BarChart3 },
            { key: 'batch',    label: '배치',  icon: Settings },
            { key: 'places',   label: '장소검토', icon: MapPin },
            { key: 'posts',    label: '게시글', icon: ImageIcon },
            { key: 'comments', label: '댓글',  icon: MessageCircle },
            { key: 'dms',      label: 'DM',    icon: Mail },
          ] as { key: TabType; label: string; icon: any }[]).map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex-1 min-w-[56px] py-2.5 text-xs font-bold transition-spring relative flex items-center justify-center gap-1 ${
                activeTab === tab.key ? 'text-primary' : 'text-gray-500'
              }`}
            >
              <tab.icon size={14} />
              {tab.label}
              {activeTab === tab.key && (
                <motion.div layoutId="admin-tab" className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <AnimatePresence mode="wait">
        {activeTab === 'overview' && <OverviewTab key="overview" onSwitchTab={setActiveTab} onSwitchToPlaces={switchToPlaces} />}
        {activeTab === 'batch'    && <BatchTab    key="batch" />}
        {activeTab === 'places'   && <PlacesReviewTab key="places" initialSection={placesSection} />}
        {activeTab === 'posts'    && <PostsTab    key="posts" posts={posts} />}
        {activeTab === 'comments' && <CommentsTab key="comments" posts={posts} />}
        {activeTab === 'dms'      && <DMsTab      key="dms" posts={posts} />}
      </AnimatePresence>
    </div>
  );
}

// ─── 배치 관리 탭 ──────────────────────────────────────────────────────────────

const SCHEDULE_KEY = 'meongnyang_batch_schedule';
const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];

interface BatchSchedule {
  enabled: boolean;
  dayOfWeek: number; // 0=일 ~ 6=토
  hour: number;
  minute: number;
  lastRunDate: string; // 'YYYY-MM-DD'
}
const DEFAULT_SCHEDULE: BatchSchedule = { enabled: false, dayOfWeek: 1, hour: 9, minute: 0, lastRunDate: '' };

function loadSchedule(): BatchSchedule {
  try {
    const s = localStorage.getItem(SCHEDULE_KEY);
    return s ? { ...DEFAULT_SCHEDULE, ...JSON.parse(s) } : DEFAULT_SCHEDULE;
  } catch { return DEFAULT_SCHEDULE; }
}
function saveSchedule(s: BatchSchedule) {
  localStorage.setItem(SCHEDULE_KEY, JSON.stringify(s));
}
function todayStr() {
  return new Date().toISOString().slice(0, 10);
}
function nextRunLabel(s: BatchSchedule): string {
  if (!s.enabled) return '비활성';
  const now = new Date();
  const target = new Date();
  target.setHours(s.hour, s.minute, 0, 0);
  const diff = s.dayOfWeek - now.getDay();
  const daysUntil = diff < 0 ? diff + 7 : diff === 0 && now >= target ? 7 : diff;
  target.setDate(now.getDate() + daysUntil);
  return `${target.getMonth() + 1}/${target.getDate()}(${DAY_LABELS[s.dayOfWeek]}) ${String(s.hour).padStart(2, '0')}:${String(s.minute).padStart(2, '0')}`;
}

type BatchStatus = 'idle' | 'running' | 'done' | 'error';

// ─── 배치 실행 이력 ─────────────────────────────────────────────────────────────
const BATCH_HISTORY_KEY = 'meongnyang_batch_history';
const BATCH_HISTORY_MAX = 50;

interface BatchRunRecord {
  id: string;          // uuid-ish
  jobId: string;
  jobLabel: string;
  startedAt: string;   // ISO string
  completedAt: string; // ISO string
  durationSec: number;
  status: 'success' | 'error';
  result: string;      // 서버 메시지 또는 에러 메시지
}

function loadBatchHistory(): BatchRunRecord[] {
  try {
    const s = localStorage.getItem(BATCH_HISTORY_KEY);
    return s ? JSON.parse(s) : [];
  } catch { return []; }
}
function saveBatchHistory(records: BatchRunRecord[]) {
  localStorage.setItem(BATCH_HISTORY_KEY, JSON.stringify(records.slice(0, BATCH_HISTORY_MAX)));
}
function appendBatchRecord(record: BatchRunRecord) {
  const prev = loadBatchHistory();
  saveBatchHistory([record, ...prev]);
}
function fmtDateTime(iso: string) {
  const d = new Date(iso);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
}
function fmtDuration(sec: number) {
  if (sec < 60) return `${sec}초`;
  return `${Math.floor(sec / 60)}분 ${sec % 60}초`;
}

interface BatchJob {
  id: string;
  step?: number;
  label: string;
  desc: string;
  icon: React.ElementType;
  warn?: string;
  run: () => Promise<any>;
}

const PIPELINE_JOBS: BatchJob[] = [
  {
    id: 'collect',
    step: 1,
    label: '공공데이터 수집',
    desc: 'KTO(한국관광공사) + KCISA(한국문화정보원) 동시 수집 · 검증 없이 raw 임시 저장',
    icon: Database,
    run: adminApi.runCollectBatch,
  },
  {
    id: 'dedup',
    step: 2,
    label: '소스간 중복 제거',
    desc: 'KTO·KCISA 수집 데이터 비교 · 제목 유사도 ≥ 90% + 좌표 50m 이내 → 중복 제거 후 하나만 유지',
    icon: RefreshCw,
    run: adminApi.runDedupBatch,
  },
  {
    id: 'verify',
    step: 3,
    label: '카카오·네이버 교차검증',
    desc: '중복 제거된 데이터 외부 지도 API 검증 · 유사도 80%↑ ACTIVE / 50~79% PENDING / 미만 REJECTED',
    icon: Zap,
    run: adminApi.runVerifyBatch,
  },
  {
    id: 'validate-images',
    step: 4,
    label: 'Gemini 이미지 검증',
    desc: 'Gemini Vision 이미지 적합성 검증 · 부적합(SNS·뉴스·관련없음) → imageUrl null 처리',
    icon: Sparkles,
    warn: 'Gemini API 15 RPM 제한',
    run: adminApi.runValidateImagesBatch,
  },
  {
    id: 'ai-rating',
    step: 5,
    label: 'AI 별점 계산',
    desc: '전체 장소 aiRating 확정 · 리뷰 수 · 이미지 유무 · 교차검증 여부 · 카테고리 가중치 반영',
    icon: Star,
    run: adminApi.runAiRatingBatch,
  },
];

const UTILITY_JOBS: BatchJob[] = [
  {
    id: 're-verify-all',
    label: '전체 교차검증 재실행',
    desc: '전체 ACTIVE 장소 카카오 재검증 · 폐업 의심 장소 → REJECTED 처리',
    icon: RefreshCw,
    warn: '카카오 쿼터(300,000건/일) · KST 09:00 리셋',
    run: adminApi.runReVerifyAllBatch,
  },
  {
    id: 'ai-rating-all',
    label: 'AI 별점 전체 재계산',
    desc: '전체 장소 blogCount 포함 강제 재계산 · 점수 공식 변경 후 일괄 반영 시 사용',
    icon: Wrench,
    run: adminApi.runAiRatingAllBatch,
  },
];

function BatchJobCard({
  job,
  status,
  msg,
  onRun,
}: {
  job: BatchJob;
  status: BatchStatus;
  msg: string;
  onRun: () => void;
}) {
  return (
    <div className="bg-white rounded-2xl p-4 shadow-sm flex items-start gap-3">
      {/* 스텝 번호 or 아이콘 */}
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 text-sm font-black ${
        status === 'done'     ? 'bg-green-100 text-green-600' :
        status === 'error'    ? 'bg-red-100 text-red-500'     :
        status === 'running'  ? 'bg-blue-100 text-blue-500'   :
        job.step              ? 'bg-primary/10 text-primary'  : 'bg-gray-100 text-gray-500'
      }`}>
        {status === 'running' ? (
          <Loader size={18} className="animate-spin" />
        ) : status === 'done' ? (
          <CheckCircle size={18} />
        ) : status === 'error' ? (
          <XCircle size={18} />
        ) : job.step ? (
          job.step
        ) : (
          <job.icon size={18} />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <p className="text-sm font-bold text-gray-800">{job.label}</p>
          {job.step && status === 'idle' && (
            <job.icon size={13} className="text-gray-400" />
          )}
        </div>
        <p className="text-xs text-gray-500 leading-relaxed">{job.desc}</p>
        {job.warn && status === 'idle' && (
          <p className="text-[10px] text-amber-600 font-medium mt-1 flex items-center gap-1">
            <AlertTriangle size={10} className="shrink-0" />{job.warn}
          </p>
        )}
        {msg && (
          <p className={`text-xs font-bold mt-1 ${status === 'error' ? 'text-red-500' : 'text-green-600'}`}>
            {msg}
          </p>
        )}
      </div>

      <button
        onClick={onRun}
        disabled={status === 'running'}
        className={`shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all active:scale-[0.97] ${
          status === 'running'
            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
            : 'bg-primary text-white hover:opacity-90'
        }`}
      >
        <Play size={12} />
        실행
      </button>
    </div>
  );
}

function BatchTab() {
  const [statuses, setStatuses] = useState<Record<string, BatchStatus>>({});
  const [messages, setMessages] = useState<Record<string, string>>({});
  const [showUtility, setShowUtility] = useState(false);
  const [showScheduler, setShowScheduler] = useState(false);
  const [schedule, setSchedule] = useState<BatchSchedule>(loadSchedule);
  const [autoRunMsg, setAutoRunMsg] = useState('');
  const [history, setHistory] = useState<BatchRunRecord[]>(loadBatchHistory);
  const [showHistory, setShowHistory] = useState(false);

  // 마운트 시 자동 수집 체크
  useEffect(() => {
    const s = loadSchedule();
    if (!s.enabled) return;
    const now = new Date();
    if (s.dayOfWeek !== now.getDay()) return;
    if (s.lastRunDate === todayStr()) return;
    const target = new Date();
    target.setHours(s.hour, s.minute, 0, 0);
    if (now < target) return;
    // 스케줄 조건 충족 → STEP 1 자동 실행
    setAutoRunMsg('⏰ 자동 수집 실행 중...');
    adminApi.runCollectBatch()
      .then(() => {
        const updated = { ...s, lastRunDate: todayStr() };
        saveSchedule(updated);
        setSchedule(updated);
        setAutoRunMsg('✅ 자동 수집 완료 — 수집 대기 탭에서 확인하세요');
      })
      .catch(() => setAutoRunMsg('❌ 자동 수집 오류'));
  }, []);

  const updateSchedule = (patch: Partial<BatchSchedule>) => {
    const updated = { ...schedule, ...patch };
    setSchedule(updated);
    saveSchedule(updated);
  };

  const run = async (job: BatchJob) => {
    setStatuses(s => ({ ...s, [job.id]: 'running' }));
    setMessages(m => ({ ...m, [job.id]: '' }));
    const startedAt = new Date().toISOString();
    try {
      const res = await job.run();
      const completedAt = new Date().toISOString();
      const serverMsg = res?.data?.message ?? res?.data ?? null;
      const resultText = typeof serverMsg === 'string' ? serverMsg : '완료';
      setStatuses(s => ({ ...s, [job.id]: 'done' }));
      setMessages(m => ({ ...m, [job.id]: resultText }));
      const record: BatchRunRecord = {
        id: `${job.id}-${Date.now()}`,
        jobId: job.id,
        jobLabel: job.label,
        startedAt,
        completedAt,
        durationSec: Math.round((new Date(completedAt).getTime() - new Date(startedAt).getTime()) / 1000),
        status: 'success',
        result: resultText,
      };
      appendBatchRecord(record);
      setHistory(loadBatchHistory());
    } catch (e: any) {
      const completedAt = new Date().toISOString();
      const errMsg = e?.response?.data?.message ?? '오류 발생';
      setStatuses(s => ({ ...s, [job.id]: 'error' }));
      setMessages(m => ({ ...m, [job.id]: errMsg }));
      const record: BatchRunRecord = {
        id: `${job.id}-${Date.now()}`,
        jobId: job.id,
        jobLabel: job.label,
        startedAt,
        completedAt,
        durationSec: Math.round((new Date(completedAt).getTime() - new Date(startedAt).getTime()) / 1000),
        status: 'error',
        result: errMsg,
      };
      appendBatchRecord(record);
      setHistory(loadBatchHistory());
    }
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-4">

      {/* 자동 수집 예약 */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <button
          onClick={() => setShowScheduler(v => !v)}
          className="w-full flex items-center justify-between px-4 py-3.5"
        >
          <div className="flex items-center gap-2">
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${schedule.enabled ? 'bg-primary/10' : 'bg-gray-100'}`}>
              <Calendar size={16} className={schedule.enabled ? 'text-primary' : 'text-gray-400'} />
            </div>
            <div className="text-left">
              <p className="text-sm font-bold text-gray-800">자동 수집 예약</p>
              <p className="text-[11px] text-gray-400">
                {schedule.enabled ? `다음 실행: ${nextRunLabel(schedule)}` : '비활성 — 수동 실행만'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* 토글 */}
            <button
              onClick={e => { e.stopPropagation(); updateSchedule({ enabled: !schedule.enabled }); }}
              className={`relative w-11 h-6 rounded-full transition-spring ${schedule.enabled ? 'bg-primary' : 'bg-gray-200'}`}
            >
              <span className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform ${schedule.enabled ? 'translate-x-5' : 'translate-x-0.5'}`} />
            </button>
            {showScheduler ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />}
          </div>
        </button>

        <AnimatePresence>
          {showScheduler && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="px-4 pb-4 space-y-4 border-t border-gray-100 pt-3">
                {/* 요일 선택 */}
                <div>
                  <p className="text-xs font-bold text-gray-500 mb-2 flex items-center gap-1">
                    <Calendar size={12} /> 수집 요일
                  </p>
                  <div className="flex gap-1.5">
                    {DAY_LABELS.map((d, i) => (
                      <button
                        key={i}
                        onClick={() => updateSchedule({ dayOfWeek: i })}
                        className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all ${
                          schedule.dayOfWeek === i
                            ? 'bg-primary text-white shadow-sm'
                            : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                        }`}
                      >
                        {d}
                      </button>
                    ))}
                  </div>
                </div>

                {/* 시간 선택 */}
                <div>
                  <p className="text-xs font-bold text-gray-500 mb-2 flex items-center gap-1">
                    <Clock size={12} /> 실행 시각
                  </p>
                  <input
                    type="time"
                    value={`${String(schedule.hour).padStart(2, '0')}:${String(schedule.minute).padStart(2, '0')}`}
                    onChange={e => {
                      const [h, m] = e.target.value.split(':').map(Number);
                      updateSchedule({ hour: h, minute: m });
                    }}
                    className="w-full px-4 py-3 rounded-2xl border-2 border-gray-100 text-sm text-gray-800 outline-none focus:border-primary transition-spring"
                  />
                </div>

                {/* 안내 */}
                <div className="bg-blue-50 rounded-xl px-3 py-2.5 space-y-1">
                  <p className="text-xs font-bold text-blue-700 flex items-center gap-1">
                    <Bell size={11} /> 자동 수집 안내
                  </p>
                  <p className="text-[11px] text-blue-600 leading-relaxed">
                    설정한 요일/시각에 관리자 페이지가 열려 있으면 STEP 1 수집이 자동 실행됩니다.<br />
                    수집된 장소는 <span className="font-bold">장소검토 탭 → 수집 대기</span>에서 확인하세요.
                  </p>
                </div>

                {schedule.lastRunDate && (
                  <p className="text-[11px] text-gray-400 text-center">
                    마지막 자동 실행: {schedule.lastRunDate}
                  </p>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* 자동 실행 메시지 */}
      {autoRunMsg && (
        <div className={`rounded-2xl px-4 py-3 text-sm font-bold ${
          autoRunMsg.startsWith('✅') ? 'bg-green-50 text-green-700 border border-green-200'
          : autoRunMsg.startsWith('❌') ? 'bg-red-50 text-red-600 border border-red-200'
          : 'bg-blue-50 text-blue-700 border border-blue-200'
        }`}>
          {autoRunMsg}
        </div>
      )}

      {/* 파이프라인 V3 안내 */}
      <div className="bg-blue-50 border border-blue-200 rounded-2xl px-4 py-3 space-y-1">
        <p className="text-xs font-bold text-blue-800">데이터 파이프라인 V3 — 권장 실행 순서</p>
        <p className="text-[11px] text-blue-600">
          STEP 1 수집 → STEP 2 소스간 중복 제거 → STEP 3 카카오·네이버 검증 → STEP 4 이미지 검증 → STEP 5 AI 별점
        </p>
        <p className="text-[11px] text-blue-500 opacity-80">
          Kakao 쿼터 300,000건/일 · KST 09:00 리셋 · Gemini 15 RPM 제한
        </p>
      </div>

      {/* 메인 파이프라인 */}
      <div className="space-y-2">
        <p className="text-xs font-bold text-gray-500 uppercase tracking-wider px-1">메인 파이프라인</p>
        {PIPELINE_JOBS.map(job => (
          <BatchJobCard
            key={job.id}
            job={job}
            status={statuses[job.id] ?? 'idle'}
            msg={messages[job.id] ?? ''}
            onRun={() => run(job)}
          />
        ))}
      </div>

      {/* 유틸리티 — 토글 */}
      <div>
        <button
          onClick={() => setShowUtility(v => !v)}
          className="w-full flex items-center justify-between px-1 py-2 text-xs font-bold text-gray-500 uppercase tracking-wider hover:text-gray-700 transition-spring"
        >
          <span>유틸리티 (개별 실행)</span>
          {showUtility ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>

        <AnimatePresence>
          {showUtility && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden space-y-2 mt-2"
            >
              {UTILITY_JOBS.map(job => (
                <BatchJobCard
                  key={job.id}
                  job={job}
                  status={statuses[job.id] ?? 'idle'}
                  msg={messages[job.id] ?? ''}
                  onRun={() => run(job)}
                />
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* 실행 이력 */}
      <div>
        <button
          onClick={() => setShowHistory(v => !v)}
          className="w-full flex items-center justify-between px-1 py-2 text-xs font-bold text-gray-500 uppercase tracking-wider hover:text-gray-700 transition-spring"
        >
          <span className="flex items-center gap-1.5">
            실행 이력
            {history.length > 0 && (
              <span className="bg-gray-200 text-gray-600 text-[10px] font-bold rounded-full px-1.5 py-0.5">
                {history.length}
              </span>
            )}
          </span>
          <div className="flex items-center gap-2">
            {history.length > 0 && (
              <button
                onClick={e => {
                  e.stopPropagation();
                  localStorage.removeItem(BATCH_HISTORY_KEY);
                  setHistory([]);
                }}
                className="text-[10px] text-red-400 hover:text-red-600 font-medium px-1.5 py-0.5 rounded"
              >
                전체 삭제
              </button>
            )}
            {showHistory ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          </div>
        </button>

        <AnimatePresence>
          {showHistory && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden mt-2"
            >
              {history.length === 0 ? (
                <div className="bg-white rounded-2xl p-5 text-center text-xs text-gray-400 shadow-sm">
                  실행 기록이 없습니다
                </div>
              ) : (
                <div className="space-y-2">
                  {history.map(rec => (
                    <div key={rec.id} className="bg-white rounded-2xl px-4 py-3 shadow-sm flex items-start gap-3">
                      <div className={`w-8 h-8 rounded-xl shrink-0 flex items-center justify-center ${
                        rec.status === 'success' ? 'bg-green-100' : 'bg-red-100'
                      }`}>
                        {rec.status === 'success'
                          ? <CheckCircle size={15} className="text-green-600" />
                          : <XCircle size={15} className="text-red-500" />}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <p className="text-xs font-bold text-gray-800 truncate">{rec.jobLabel}</p>
                          <span className="text-[10px] text-gray-400 shrink-0">{fmtDuration(rec.durationSec)}</span>
                        </div>
                        <p className={`text-[11px] mt-0.5 truncate ${rec.status === 'success' ? 'text-green-600' : 'text-red-500'}`}>
                          {rec.result}
                        </p>
                        <p className="text-[10px] text-gray-400 mt-0.5">{fmtDateTime(rec.startedAt)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

// ─── 전체 현황 탭 ──────────────────────────────────────────────────────────────

function OverviewTab({ onSwitchTab, onSwitchToPlaces }: { onSwitchTab: (tab: TabType) => void; onSwitchToPlaces: (section: PlacesSection) => void }) {
  const [stats, setStats] = useState<BatchStatsDto | null>(null);
  const [noImageCount, setNoImageCount] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [quickStatus, setQuickStatus] = useState<Record<string, 'idle' | 'running' | 'done' | 'error'>>({});

  useEffect(() => {
    Promise.all([
      adminApi.getBatchStats(),
      adminApi.getNoImagePlaces(),
    ]).then(([s, noImg]) => {
      setStats(s);
      setNoImageCount(noImg.length);
    }).catch(() => {}).finally(() => setIsLoading(false));
  }, []);

  const quickRun = async (key: string, fn: () => Promise<any>) => {
    setQuickStatus(s => ({ ...s, [key]: 'running' }));
    try {
      await fn();
      setQuickStatus(s => ({ ...s, [key]: 'done' }));
    } catch {
      setQuickStatus(s => ({ ...s, [key]: 'error' }));
    }
  };

  // 실제 사용자 노출 수 = ACTIVE - 이미지없는ACTIVE (이미지없는장소는 공개 차단됨)
  const realPublic = stats !== null && noImageCount !== null
    ? stats.active - noImageCount
    : null;
  const activeRate = stats && stats.total > 0 && realPublic !== null
    ? Math.round((realPublic / stats.total) * 100)
    : 0;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-5">

      {/* ── 장소 DB 현황 ─────────────────────────────────────── */}
      <div>
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider px-1 mb-3 flex items-center gap-1.5">
          <Database size={13} /> 장소 DB 현황
        </h3>

        {isLoading ? (
          <div className="bg-white rounded-2xl p-8 flex items-center justify-center shadow-sm">
            <Loader size={22} className="text-primary animate-spin" />
          </div>
        ) : (
          <>
            {/* 메인 수치 */}
            <div className="bg-gradient-to-br from-primary to-secondary rounded-2xl p-5 text-white shadow-md mb-3">
              <div className="flex items-end justify-between">
                <div>
                  <p className="text-xs opacity-80 mb-1">전체 장소</p>
                  <p className="text-4xl font-black">{stats?.total.toLocaleString() ?? '–'}</p>
                  <p className="text-xs opacity-70 mt-1">공개율 {activeRate}%</p>
                </div>
                <Database size={48} className="opacity-20" />
              </div>
              {/* 게이지 바 */}
              <div className="mt-4 h-2 bg-white/20 rounded-full overflow-hidden">
                <div
                  className="h-full bg-white rounded-full transition-all"
                  style={{ width: `${activeRate}%` }}
                />
              </div>
            </div>

            {/* 상태별 카드 4개 — 모두 클릭 시 해당 섹션으로 이동 */}
            <div className="grid grid-cols-2 gap-3">
              {/* 사용자 노출 중 → 등록 장소 수정 */}
              <button
                onClick={() => onSwitchToPlaces('editActive')}
                className="bg-white rounded-2xl p-4 shadow-sm text-left transition-all active:scale-[0.97] hover:shadow-md"
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-7 h-7 rounded-lg bg-green-100 flex items-center justify-center">
                    <CheckCircle size={14} className="text-green-600" />
                  </div>
                  <span className="text-xs font-bold text-gray-600">사용자 노출 중</span>
                  <ArrowRight size={11} className="text-gray-300 ml-auto" />
                </div>
                <p className="text-2xl font-black text-gray-900">{realPublic !== null ? realPublic.toLocaleString() : '–'}</p>
                <p className="text-[10px] text-gray-400 mt-0.5">ACTIVE {stats?.active.toLocaleString() ?? '–'}건 중 이미지 있는 장소</p>
              </button>

              {/* 수집 대기 → 수집 대기 섹션 */}
              <button
                onClick={() => onSwitchToPlaces('pending')}
                className={`rounded-2xl p-4 shadow-sm text-left transition-all active:scale-[0.97] hover:shadow-md ${
                  (stats?.pending ?? 0) > 0 ? 'bg-amber-50 border-2 border-amber-300' : 'bg-white'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-7 h-7 rounded-lg bg-amber-100 flex items-center justify-center">
                    <AlertTriangle size={14} className="text-amber-600" />
                  </div>
                  <span className="text-xs font-bold text-gray-600">수집 대기</span>
                  <ArrowRight size={11} className={`ml-auto ${(stats?.pending ?? 0) > 0 ? 'text-amber-400' : 'text-gray-300'}`} />
                </div>
                <p className={`text-2xl font-black ${(stats?.pending ?? 0) > 0 ? 'text-amber-600' : 'text-gray-900'}`}>
                  {stats?.pending.toLocaleString() ?? '–'}
                </p>
              </button>

              {/* 거절 → 거절된 장소 복구 섹션 */}
              <button
                onClick={() => onSwitchToPlaces('rejected')}
                className="bg-white rounded-2xl p-4 shadow-sm text-left transition-all active:scale-[0.97] hover:shadow-md"
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-7 h-7 rounded-lg bg-red-100 flex items-center justify-center">
                    <XCircle size={14} className="text-red-500" />
                  </div>
                  <span className="text-xs font-bold text-gray-600">거절 (REJECTED)</span>
                  <ArrowRight size={11} className="text-gray-300 ml-auto" />
                </div>
                <p className="text-2xl font-black text-gray-900">{stats?.rejected.toLocaleString() ?? '–'}</p>
              </button>

              {/* 이미지 없음 → 이미지 없는 장소 섹션 */}
              <button
                onClick={() => onSwitchToPlaces('noImage')}
                className={`rounded-2xl p-4 shadow-sm text-left transition-all active:scale-[0.97] hover:shadow-md ${(noImageCount ?? 0) > 0 ? 'bg-orange-50 border border-orange-200' : 'bg-white'}`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-7 h-7 rounded-lg bg-orange-100 flex items-center justify-center">
                    <ImageOff size={14} className="text-orange-500" />
                  </div>
                  <span className="text-xs font-bold text-gray-600">이미지 없음</span>
                  <ArrowRight size={11} className={`ml-auto ${(noImageCount ?? 0) > 0 ? 'text-orange-300' : 'text-gray-300'}`} />
                </div>
                <p className={`text-2xl font-black ${(noImageCount ?? 0) > 0 ? 'text-orange-600' : 'text-gray-900'}`}>
                  {noImageCount?.toLocaleString() ?? '–'}
                </p>
              </button>
            </div>
          </>
        )}
      </div>

      {/* ── 알림 배너 ────────────────────────────────────────── */}
      {!isLoading && (stats?.pending ?? 0) > 0 && (
        <button
          onClick={() => onSwitchToPlaces('pending')}
          className="w-full bg-amber-50 border border-amber-200 rounded-2xl px-4 py-3.5 flex items-center gap-3 text-left active:scale-[0.98] transition-all"
        >
          <AlertTriangle size={18} className="text-amber-500 shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-bold text-amber-800">수집 대기 장소 {stats!.pending}건</p>
            <p className="text-xs text-amber-600">탭하여 수집 대기 목록으로 이동</p>
          </div>
          <ArrowRight size={16} className="text-amber-400 shrink-0" />
        </button>
      )}

      {/* ── 빠른 실행 ────────────────────────────────────────── */}
      <div>
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider px-1 mb-3 flex items-center gap-1.5">
          <Zap size={13} /> 빠른 실행
        </h3>
        <div className="grid grid-cols-2 gap-3">
          {[
            {
              key: 'gemini',
              label: 'Gemini 이미지 검증',
              desc: 'Vision API 품질 검증',
              icon: Sparkles,
              color: 'bg-purple-50 border-purple-200 text-purple-700',
              fn: adminApi.runValidateImagesBatch,
            },
            {
              key: 'ai-rating',
              label: 'AI 별점 재계산',
              desc: '전체 aiRating 업데이트',
              icon: Star,
              color: 'bg-blue-50 border-blue-200 text-blue-700',
              fn: adminApi.runAiRatingBatch,
            },
            {
              key: 'places-tab',
              label: '장소 검토하기',
              desc: `대기 ${stats?.pending ?? '–'}건`,
              icon: MapPin,
              color: 'bg-green-50 border-green-200 text-green-700',
              fn: null,
              onClick: () => onSwitchToPlaces('pending'),
            },
            {
              key: 'batch-tab',
              label: '배치 관리',
              desc: '파이프라인 V2 실행',
              icon: Settings,
              color: 'bg-gray-50 border-gray-200 text-gray-700',
              fn: null,
              onClick: () => onSwitchTab('batch'),
            },
          ].map(item => {
            const s = quickStatus[item.key] ?? 'idle';
            return (
              <button
                key={item.key}
                onClick={item.onClick ?? (() => item.fn && quickRun(item.key, item.fn))}
                disabled={s === 'running'}
                className={`border rounded-2xl p-4 text-left transition-all active:scale-[0.97] disabled:opacity-60 ${item.color}`}
              >
                <div className="flex items-center justify-between mb-2">
                  {s === 'running' ? (
                    <Loader size={18} className="animate-spin" />
                  ) : s === 'done' ? (
                    <CheckCircle size={18} />
                  ) : s === 'error' ? (
                    <XCircle size={18} />
                  ) : (
                    <item.icon size={18} />
                  )}
                  {item.fn && s === 'idle' && <Play size={12} className="opacity-50" />}
                  {item.onClick && <ArrowRight size={12} className="opacity-50" />}
                </div>
                <p className="text-xs font-bold leading-tight">{item.label}</p>
                <p className="text-[10px] opacity-70 mt-0.5">{s === 'done' ? '완료' : s === 'error' ? '오류 발생' : item.desc}</p>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── 커뮤니티 현황 (팀원B 연동 후 활성화) ─────────────── */}
      <div>
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider px-1 mb-3 flex items-center gap-1.5">
          <Activity size={13} /> 커뮤니티 현황
          <span className="ml-auto text-[10px] bg-gray-200 text-gray-500 px-2 py-0.5 rounded-full font-medium">팀원B 연동 후 활성화</span>
        </h3>
        <div className="grid grid-cols-3 gap-2">
          {[
            { label: '게시글', icon: ImageIcon },
            { label: '좋아요', icon: Heart },
            { label: '댓글', icon: MessageCircle },
            { label: 'DM', icon: Send },
            { label: '신고', icon: AlertTriangle },
            { label: '숨김', icon: EyeOff },
          ].map((item, i) => (
            <div key={i} className="bg-white rounded-2xl p-3 text-center shadow-sm opacity-40">
              <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-1.5">
                <item.icon size={14} className="text-gray-400" />
              </div>
              <div className="text-base font-bold text-gray-400">–</div>
              <div className="text-[10px] text-gray-400">{item.label}</div>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}


function PostsTab({ posts }: { posts: FeedPost[] }) {
  const { toggleHidePost, deletePost } = useFeedStore();
  const [sortBy, setSortBy] = useState<SortType>('latest');
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedPost, setExpandedPost] = useState<number | null>(null);

  const filteredPosts = posts
    .filter(p => 
      searchTerm === '' ||
      p.user.includes(searchTerm) ||
      p.content.includes(searchTerm)
    )
    .sort((a, b) => {
      switch (sortBy) {
        case 'likes': return b.likes - a.likes;
        case 'comments': return b.comments - a.comments;
        case 'dms': return b.dms - a.dms;
        case 'reported': return (b.isReported ? 1 : 0) - (a.isReported ? 1 : 0);
        default: return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      {/* Search & Filter */}
      <div className="flex gap-2">
        <div className="flex-1 bg-white rounded-xl flex items-center gap-2 px-3 shadow-sm">
          <Search size={16} className="text-gray-400" />
          <input
            type="text"
            placeholder="사용자명 또는 내용 검색..."
            className="flex-1 py-2.5 text-sm outline-none bg-transparent placeholder:text-gray-400"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as SortType)}
          className="bg-white rounded-xl px-3 py-2.5 text-xs font-bold text-gray-600 shadow-sm outline-none"
        >
          <option value="latest">최신순</option>
          <option value="likes">좋아요순</option>
          <option value="comments">댓글순</option>
          <option value="dms">DM순</option>
          <option value="reported">신고순</option>
        </select>
      </div>

      <div className="text-xs text-gray-500 font-medium">총 {filteredPosts.length}개 게시글</div>

      {/* Post List */}
      {filteredPosts.map(post => (
        <div
          key={post.id}
          className={`bg-white rounded-2xl overflow-hidden shadow-sm border ${
            post.isReported ? 'border-red-200' : post.isHidden ? 'border-gray-300 opacity-60' : 'border-transparent'
          }`}
        >
          <div
            className="flex gap-3 p-3 cursor-pointer"
            onClick={() => setExpandedPost(expandedPost === post.id ? null : post.id)}
          >
            <div className="w-14 h-14 rounded-xl overflow-hidden bg-gray-100 shrink-0 relative">
              <img src={post.img} alt="" className="w-full h-full object-cover" />
              {post.isHidden && (
                <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                  <EyeOff size={14} className="text-white" />
                </div>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between mb-0.5">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-gray-800">{post.user}</span>
                  {post.isReported && (
                    <span className="text-[10px] bg-red-100 text-red-600 px-1.5 py-0.5 rounded-full font-bold">신고됨</span>
                  )}
                  {post.isHidden && (
                    <span className="text-[10px] bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded-full font-bold">숨김</span>
                  )}
                </div>
                {expandedPost === post.id ? <ChevronUp size={14} className="text-gray-400" /> : <ChevronDown size={14} className="text-gray-400" />}
              </div>
              <p className="text-[11px] text-gray-600 truncate">{post.content}</p>
              <div className="flex items-center gap-3 mt-1">
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <Heart size={10} className={post.likes > 0 ? 'text-primary' : ''} /> {post.likes}
                </span>
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <MessageCircle size={10} /> {post.comments}
                </span>
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <Send size={10} /> {post.dms}
                </span>
                <span className="text-[10px] text-gray-400 ml-auto">{post.time}</span>
              </div>
            </div>
          </div>

          <AnimatePresence>
            {expandedPost === post.id && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="overflow-hidden"
              >
                <div className="px-3 pb-3 border-t border-gray-100 pt-3">
                  {/* Liked By */}
                  {post.likedBy.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">좋아요 ({post.likedBy.length})</h5>
                      <div className="flex flex-wrap gap-1">
                        {post.likedBy.map(u => (
                          <span key={u} className="text-[10px] bg-pink-50 text-primary px-2 py-0.5 rounded-full">{u}</span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Recent Comments */}
                  {post.commentList.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">최근 댓글 ({post.commentList.length})</h5>
                      <div className="space-y-1">
                        {post.commentList.slice(-3).map(c => (
                          <div key={c.id} className="bg-gray-50 rounded-lg px-2.5 py-1.5">
                            <span className="text-[11px] font-bold text-gray-700">{c.user}</span>
                            <span className="text-[11px] text-gray-600 ml-1">{c.content}</span>
                            <span className="text-[10px] text-gray-400 ml-1">{c.time}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* DM summary */}
                  {post.dmList.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">DM ({post.dmList.length})</h5>
                      <div className="space-y-1">
                        {post.dmList.slice(-3).map(d => (
                          <div key={d.id} className={`rounded-lg px-2.5 py-1.5 flex items-center gap-1 ${d.isRead ? 'bg-gray-50' : 'bg-blue-50'}`}>
                            {!d.isRead && <div className="w-1.5 h-1.5 rounded-full bg-blue-500 shrink-0" />}
                            <span className="text-[11px] font-bold text-gray-700">{d.from}</span>
                            <span className="text-[11px] text-gray-500">→</span>
                            <span className="text-[11px] font-bold text-gray-700">{d.to}</span>
                            <span className="text-[11px] text-gray-600 ml-1 truncate">{d.content}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Admin Actions */}
                  <div className="flex gap-2 pt-2 border-t border-gray-100">
                    <button
                      onClick={() => toggleHidePost(post.id)}
                      className={`flex-1 text-xs font-bold py-2 rounded-xl transition-spring flex items-center justify-center gap-1 ${
                        post.isHidden
                          ? 'bg-green-50 text-green-600 hover:bg-green-100'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {post.isHidden ? <><Eye size={14} /> 공개</> : <><EyeOff size={14} /> 숨김</>}
                    </button>
                    <button
                      onClick={() => {
                        if (confirm('정말 이 게시글을 삭제하시겠습니까?')) {
                          deletePost(post.id);
                        }
                      }}
                      className="flex-1 text-xs font-bold py-2 rounded-xl bg-red-50 text-red-600 hover:bg-red-100 transition-spring flex items-center justify-center gap-1"
                    >
                      <Trash2 size={14} /> 삭제
                    </button>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      ))}
    </motion.div>
  );
}

function CommentsTab({ posts }: { posts: FeedPost[] }) {
  const allComments = posts
    .flatMap(p => p.commentList.map(c => ({ ...c, postId: p.id, postUser: p.user, postImg: p.img })))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-gray-800">전체 댓글</h3>
        <span className="text-xs text-gray-500 font-medium">{allComments.length}개</span>
      </div>

      {allComments.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <MessageCircle size={32} className="mx-auto mb-2 opacity-30" />
          <p className="text-sm">아직 댓글이 없습니다.</p>
        </div>
      ) : (
        allComments.map(c => (
          <div key={`${c.postId}-${c.id}`} className="bg-white rounded-2xl p-3 shadow-sm">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-xl overflow-hidden bg-gray-100 shrink-0">
                <img src={c.postImg} alt="" className="w-full h-full object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-0.5">
                  <span className="text-[10px] text-gray-400">on</span>
                  <span className="text-[10px] font-bold text-gray-500">{c.postUser}의 게시글</span>
                </div>
                <div className="text-sm">
                  <span className="font-bold text-gray-800 mr-1">{c.user}</span>
                  <span className="text-gray-700">{c.content}</span>
                </div>
                <span className="text-[10px] text-gray-400">{c.time}</span>
              </div>
            </div>
          </div>
        ))
      )}
    </motion.div>
  );
}

// ─── 장소 검토 탭 ──────────────────────────────────────────────────────────────

function PlacesReviewTab({ initialSection }: { initialSection?: PlacesSection | null }) {
  const fetchPlaces = useAppStore(s => s.fetchPlaces);
  const [places, setPlaces] = React.useState<PendingPlaceDto[]>([]);
  const [rejected, setRejected] = React.useState<PendingPlaceDto[]>([]);
  const [noImage, setNoImage] = React.useState<PendingPlaceDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [editingId, setEditingId] = React.useState<number | null>(null);
  const [editForm, setEditForm] = React.useState({ title: '', address: '', lat: '', lng: '' });
  const [actionLoading, setActionLoading] = React.useState<number | null>(null);
  const [bulkLoading, setBulkLoading] = React.useState(false);
  const [placeSubTab, setPlaceSubTab] = React.useState<'pending' | 'noImage' | 'rejected' | 'active' | 'create'>('pending');
  // 신규 장소 추가 — 3단계 흐름
  const EMPTY_CREATE = { title: '', category: 'PLACE', address: '', phone: '', homepage: '', imageUrl: '', description: '' };
  const [createForm, setCreateForm] = React.useState(EMPTY_CREATE);
  const [createStep, setCreateStep] = React.useState<'form' | 'analyzing' | 'preview' | 'done'>('form');
  const [createSaving, setCreateSaving] = React.useState(false);
  const [analyzeResult, setAnalyzeResult] = React.useState<{
    lat: number | null; lng: number | null; geocodeSuccess: boolean;
    aiRating: number; blogCount: number;
    blogPositiveTags: string | null; blogNegativeTags: string | null;
    naverVerified: boolean;
  } | null>(null);
  const [createSuccess, setCreateSuccess] = React.useState<string | null>(null);
  // 인라인 편집 (이미지없음 + 거절 섹션 공용)
  type InlineEditForm = { title: string; address: string; phone: string; homepage: string; imageUrl: string };
  const emptyEdit = (): InlineEditForm => ({ title: '', address: '', phone: '', homepage: '', imageUrl: '' });
  const [inlineEdits, setInlineEdits] = React.useState<Record<number, InlineEditForm>>({});
  const [expandedEditIds, setExpandedEditIds] = React.useState<Set<number>>(new Set());
  const getEdit = (id: number): InlineEditForm => inlineEdits[id] ?? emptyEdit();
  const setEditField = (id: number, field: keyof InlineEditForm, value: string) =>
    setInlineEdits(prev => ({ ...prev, [id]: { ...getEdit(id), [field]: value } }));
  const toggleExpandEdit = (id: number) =>
    setExpandedEditIds(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s; });
  // 체크박스 선택 상태
  const [selectedPending, setSelectedPending] = React.useState<Set<number>>(new Set());
  const [selectedRejected, setSelectedRejected] = React.useState<Set<number>>(new Set());
  // 장소 수정 섹션
  const [allActive, setAllActive] = React.useState<PendingPlaceDto[]>([]);
  const [allActiveLoading, setAllActiveLoading] = React.useState(false);
  const [editSearch, setEditSearch] = React.useState('');
  const [noImageSearch, setNoImageSearch] = React.useState('');
  const [rejectedSearch, setRejectedSearch] = React.useState('');
  const [pendingCategoryFilter, setPendingCategoryFilter] = React.useState('');
  const [noImageCategoryFilter, setNoImageCategoryFilter] = React.useState('');
  const [rejectedCategoryFilter, setRejectedCategoryFilter] = React.useState('');
  const [editCategoryFilter, setEditCategoryFilter] = React.useState('');
  const [editingPlaceId, setEditingPlaceId] = React.useState<number | null>(null);
  const [editPlaceForm, setEditPlaceForm] = React.useState({ title: '', address: '', phone: '', homepage: '', imageUrl: '' });
  const [editPageSize, setEditPageSize] = React.useState(30);
  const [showDupPreview, setShowDupPreview] = React.useState(false);
  const [showMuseumPreview, setShowMuseumPreview] = React.useState(false);
  const [resolvedDupIds, setResolvedDupIds] = React.useState<Set<number>>(new Set());
  const [editImageFilter, setEditImageFilter] = React.useState<'all' | 'hasImage' | 'noImage' | 'duplicate'>('hasImage');

  React.useEffect(() => {
    Promise.all([
      adminApi.getPendingPlaces(),
      adminApi.getRejectedPlaces(),
      adminApi.getNoImagePlaces(),
    ]).then(([pending, rej, noImg]) => {
      setPlaces(pending);
      setRejected(rej);
      setNoImage(noImg);
    }).finally(() => setLoading(false));
  }, []);

  // initialSection에 따라 서브탭 자동 이동
  React.useEffect(() => {
    if (!initialSection) return;
    if (initialSection === 'noImage')    setPlaceSubTab('noImage');
    if (initialSection === 'rejected')   setPlaceSubTab('rejected');
    if (initialSection === 'create')     setPlaceSubTab('create');
    if (initialSection === 'editActive') {
      setPlaceSubTab('active');
      setAllActiveLoading(true);
      adminApi.getAllActivePlaces()
        .then(data => setAllActive(data))
        .finally(() => setAllActiveLoading(false));
    }
  }, [initialSection]);

  // 전체관리 탭 전환 시 데이터 로드
  const switchToActive = async () => {
    setPlaceSubTab('active');
    if (allActive.length === 0) {
      setAllActiveLoading(true);
      try {
        const data = await adminApi.getAllActivePlaces();
        setAllActive(data);
      } finally {
        setAllActiveLoading(false);
      }
    }
  };

  const parsedReason = (raw: string | null) => {
    if (!raw) return null;
    try { return JSON.parse(raw); } catch { return null; }
  };

  const handleApprove = async (id: number, coords?: { lat: number; lng: number }) => {
    setActionLoading(id);
    try {
      await adminApi.approvePlace(id, coords);
      setPlaces(prev => prev.filter(p => p.id !== id));
      setRejected(prev => prev.filter(p => p.id !== id));
    } finally {
      setActionLoading(null);
      setEditingId(null);
    }
  };

  const handleReject = async (id: number) => {
    if (!confirm('이 장소를 거절하시겠습니까?')) return;
    setActionLoading(id);
    try {
      await adminApi.rejectPlace(id);
      const target = [...places, ...noImage, ...allActive].find(p => p.id === id);
      const removeById = (prev: PendingPlaceDto[]) => prev.filter(p => p.id !== id);
      setPlaces(removeById);
      setNoImage(removeById);
      setAllActive(removeById);
      if (target) setRejected(prev => [target, ...prev]);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRestoreApprove = async (id: number) => {
    setActionLoading(id);
    try {
      await adminApi.approvePlace(id);
      setRejected(prev => prev.filter(p => p.id !== id));
      setInlineEdits(prev => { const n = { ...prev }; delete n[id]; return n; });
      setExpandedEditIds(prev => { const s = new Set(prev); s.delete(id); return s; });
    } finally {
      setActionLoading(null);
    }
  };

  const handleEditSave = async (id: number) => {
    setActionLoading(id);
    try {
      const payload: Record<string, string> = {};
      if (editPlaceForm.title.trim())    payload.title    = editPlaceForm.title.trim();
      if (editPlaceForm.address.trim())  payload.address  = editPlaceForm.address.trim();
      if (editPlaceForm.phone.trim())    payload.phone    = editPlaceForm.phone.trim();
      if (editPlaceForm.homepage.trim()) payload.homepage = editPlaceForm.homepage.trim();
      payload.imageUrl = editPlaceForm.imageUrl.trim(); // 빈 문자열이면 이미지 삭제
      const updated = await adminApi.editPlace(id, payload);
      setAllActive(prev => prev.map(p => p.id === updated.id ? updated : p));
      setEditingPlaceId(null);
      // 중복 의심 필터에서 저장 완료 시 해당 항목 중복 해제
      if (editImageFilter === 'duplicate') {
        setResolvedDupIds(prev => new Set(prev).add(id));
      }
      // Detail 페이지에서 즉시 반영되도록 전역 store 갱신
      fetchPlaces();
    } finally {
      setActionLoading(null);
    }
  };

  // 인라인 필드 저장 (이미지없음 + 거절 공용)
  const handleInlineSave = async (id: number) => {
    const form = getEdit(id);
    const payload: Record<string, string> = {};
    if (form.title.trim())    payload.title    = form.title.trim();
    if (form.address.trim())  payload.address  = form.address.trim();
    if (form.phone.trim())    payload.phone    = form.phone.trim();
    if (form.homepage.trim()) payload.homepage = form.homepage.trim();
    if (form.imageUrl.trim()) payload.imageUrl = form.imageUrl.trim();
    if (Object.keys(payload).length === 0) return;
    setActionLoading(id);
    try {
      await adminApi.editPlace(id, payload);
      // 저장 후 서버에서 noImage 목록 재조회 (이미지 있는 항목이 남아있는 문제 방지)
      const freshNoImage = await adminApi.getNoImagePlaces();
      setNoImage(freshNoImage);
      setRejected(prev => prev.map(p => p.id === id ? { ...p, ...payload } : p));
      setInlineEdits(prev => { const n = { ...prev }; delete n[id]; return n; });
      setExpandedEditIds(prev => { const s = new Set(prev); s.delete(id); return s; });
      // Detail 페이지에서 즉시 반영되도록 전역 store 갱신
      fetchPlaces();
    } finally {
      setActionLoading(null);
    }
  };

  // ── 체크박스 헬퍼 ──────────────────────────────────────────────────────────
  const togglePending = (id: number) =>
    setSelectedPending(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s; });

  const toggleAllPending = () =>
    setSelectedPending(prev => prev.size === places.length ? new Set() : new Set(places.map(p => p.id)));

  const toggleRejected = (id: number) =>
    setSelectedRejected(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s; });

  const toggleAllRejected = () =>
    setSelectedRejected(prev => prev.size === rejected.length ? new Set() : new Set(rejected.map(p => p.id)));

  // ── 일괄 액션 ──────────────────────────────────────────────────────────────
  const handleBulkApprove = async () => {
    if (selectedPending.size === 0) return;
    setBulkLoading(true);
    try {
      await Promise.all([...selectedPending].map(id => adminApi.approvePlace(id)));
      setPlaces(prev => prev.filter(p => !selectedPending.has(p.id)));
      setSelectedPending(new Set());
    } finally {
      setBulkLoading(false);
    }
  };

  const handleBulkReject = async () => {
    if (selectedPending.size === 0) return;
    if (!confirm(`선택한 ${selectedPending.size}건을 일괄 거절하시겠습니까?`)) return;
    setBulkLoading(true);
    try {
      await Promise.all([...selectedPending].map(id => adminApi.rejectPlace(id)));
      setPlaces(prev => prev.filter(p => !selectedPending.has(p.id)));
      setSelectedPending(new Set());
    } finally {
      setBulkLoading(false);
    }
  };

  const handleBulkRestore = async () => {
    if (selectedRejected.size === 0) return;
    setBulkLoading(true);
    try {
      await Promise.all([...selectedRejected].map(id => adminApi.approvePlace(id)));
      setRejected(prev => prev.filter(p => !selectedRejected.has(p.id)));
      setSelectedRejected(new Set());
    } finally {
      setBulkLoading(false);
    }
  };

  const handleManualApprove = async (id: number) => {
    setActionLoading(id);
    try {
      await adminApi.manualApprovePlace(id, {
        title: editForm.title || undefined,
        address: editForm.address || undefined,
        lat: editForm.lat ? parseFloat(editForm.lat) : undefined,
        lng: editForm.lng ? parseFloat(editForm.lng) : undefined,
      });
      setPlaces(prev => prev.filter(p => p.id !== id));
    } finally {
      setActionLoading(null);
      setEditingId(null);
    }
  };

  if (loading) {
    return (
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center justify-center py-20">
        <Loader size={24} className="text-primary animate-spin" />
      </motion.div>
    );
  }

  // ── 서브탭 색상 헬퍼 ────────────────────────────────────────────────────────
  const tabColor = (id: string, active: boolean) => {
    if (!active) return 'border-transparent text-gray-400';
    return id === 'pending'  ? 'border-amber-500 text-amber-600'
         : id === 'noImage'  ? 'border-blue-500 text-blue-600'
         : id === 'rejected' ? 'border-red-500 text-red-600'
         : id === 'create'   ? 'border-green-500 text-green-600'
         :                     'border-indigo-500 text-indigo-600';
  };
  const badgeColor = (id: string, active: boolean) =>
    !active ? 'bg-gray-100 text-gray-500'
    : id === 'pending'  ? 'bg-amber-100 text-amber-700'
    : id === 'noImage'  ? 'bg-blue-100 text-blue-700'
    : id === 'rejected' ? 'bg-red-100 text-red-700'
    : id === 'create'   ? 'bg-green-100 text-green-700'
    :                     'bg-indigo-100 text-indigo-700';

  const PLACE_TABS = [
    { id: 'pending',  label: '수집대기',  count: places.length },
    { id: 'noImage',  label: '이미지없음', count: noImage.length },
    { id: 'rejected', label: '거절됨',    count: rejected.length },
    { id: 'active',   label: '전체관리',  count: allActive.length },
    { id: 'create',   label: '신규추가',  count: null },
  ] as const;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex flex-col">

      {/* ── 서브탭 네비게이션 ── */}
      <div className="flex border-b border-gray-100 bg-white sticky top-[49px] z-10">
        {PLACE_TABS.map(tab => {
          const isActive = placeSubTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => tab.id === 'active' ? switchToActive() : setPlaceSubTab(tab.id as any)}
              className={`flex-1 py-2.5 text-[11px] font-bold flex flex-col items-center gap-0.5 border-b-2 transition-spring ${tabColor(tab.id, isActive)}`}
            >
              {tab.label}
              {tab.count !== null && (
                <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${badgeColor(tab.id, isActive)}`}>
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      <div className="p-4 pb-24 space-y-3">

      {/* ══════════════════════ 수집대기 탭 ══════════════════════ */}
      {placeSubTab === 'pending' && (<>
      {/* ── PENDING 헤더 + 전체선택 + 일괄 액션 ── */}
      <div className="flex items-center justify-between">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={places.length > 0 && selectedPending.size === places.length}
            onChange={toggleAllPending}
            className="w-4 h-4 accent-amber-500 cursor-pointer"
          />
          <h3 className="text-sm font-bold text-gray-800 flex items-center gap-1">
            <MapPin size={16} className="text-amber-500" /> 수집 대기 장소
          </h3>
        </label>
        <span className="text-xs text-gray-500 font-medium">
          {selectedPending.size > 0 ? `${selectedPending.size}건 선택 /` : ''} {places.length}건
        </span>
      </div>

      {/* 카테고리 필터 */}
      <select
        value={pendingCategoryFilter}
        onChange={e => setPendingCategoryFilter(e.target.value)}
        className="w-full bg-white border border-gray-200 rounded-xl px-3 py-2 text-xs font-bold text-gray-600 outline-none"
      >
        <option value="">전체 카테고리</option>
        {Array.from(new Set(places.map(p => p.category).filter(Boolean))).sort().map(cat => (
          <option key={cat} value={cat}>{cat}</option>
        ))}
      </select>

      {/* 일괄 액션 버튼 — 선택 시 표시 */}
      {selectedPending.size > 0 && (
        <div className="flex gap-2">
          <button
            onClick={handleBulkApprove}
            disabled={bulkLoading}
            className="flex-1 py-2 rounded-xl text-xs font-bold bg-green-500 text-white flex items-center justify-center gap-1 disabled:opacity-50"
          >
            {bulkLoading ? <Loader size={12} className="animate-spin" /> : <Check size={12} />}
            일괄 승인 ({selectedPending.size})
          </button>
          <button
            onClick={handleBulkReject}
            disabled={bulkLoading}
            className="flex-1 py-2 rounded-xl text-xs font-bold bg-red-500 text-white flex items-center justify-center gap-1 disabled:opacity-50"
          >
            {bulkLoading ? <Loader size={12} className="animate-spin" /> : <X size={12} />}
            일괄 거절 ({selectedPending.size})
          </button>
        </div>
      )}

      {places.filter(p => !pendingCategoryFilter || p.category === pendingCategoryFilter).length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <CheckCircle size={32} className="mx-auto mb-2 opacity-30" />
          <p className="text-sm">{pendingCategoryFilter ? '해당 카테고리의 장소가 없습니다.' : '검토 대기 중인 장소가 없습니다.'}</p>
        </div>
      ) : (
        places.filter(p => !pendingCategoryFilter || p.category === pendingCategoryFilter).map(place => {
          const reason = parsedReason(place.pendingReason);
          const isEditing = editingId === place.id;
          const isActing = actionLoading === place.id;

          return (
            <div key={place.id} className={`bg-white rounded-2xl shadow-sm overflow-hidden border transition-all ${selectedPending.has(place.id) ? 'border-amber-400 ring-1 ring-amber-300' : 'border-amber-100'}`}>
              {/* 이미지 + 기본정보 */}
              <div className="flex gap-3 p-3">
                {/* 체크박스 */}
                <div className="flex items-start pt-1">
                  <input
                    type="checkbox"
                    checked={selectedPending.has(place.id)}
                    onChange={() => togglePending(place.id)}
                    className="w-4 h-4 accent-amber-500 cursor-pointer"
                  />
                </div>
                <div className="w-16 h-16 rounded-xl overflow-hidden bg-gray-100 shrink-0">
                  {place.imageUrl
                    ? <img src={place.imageUrl} alt="" className="w-full h-full object-cover" />
                    : <div className="w-full h-full flex items-center justify-center"><ImageOff size={20} className="text-gray-300" /></div>
                  }
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-1">
                    <p className="text-sm font-bold text-gray-800 truncate">{place.title}</p>
                    <span className="text-[10px] bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded-full font-bold shrink-0">보류</span>
                  </div>
                  <p className="text-[11px] text-gray-500 mt-0.5 truncate">{place.address}</p>
                  <p className="text-[11px] text-gray-400">{place.category}</p>
                  {reason && (
                    <div className="mt-1 text-[10px] text-amber-700 bg-amber-50 rounded-lg px-2 py-1 space-y-0.5">
                      <p>유사도 <span className="font-bold">{reason.similarity}%</span></p>
                      <p>원본: <span className="font-bold">{reason.sourceTitle}</span> → 카카오: <span className="font-bold">{reason.kakaoTitle}</span></p>
                    </div>
                  )}
                </div>
              </div>

              {/* 수동 편집 폼 */}
              {isEditing && (
                <div className="px-3 pb-3 border-t border-gray-100 pt-3 space-y-2">
                  <p className="text-[11px] font-bold text-gray-500">수동 수정 (빈칸은 기존값 유지)</p>
                  {[
                    { label: '상호명', key: 'title', placeholder: place.title },
                    { label: '주소', key: 'address', placeholder: place.address },
                    { label: '위도', key: 'lat', placeholder: String(place.latitude) },
                    { label: '경도', key: 'lng', placeholder: String(place.longitude) },
                  ].map(f => (
                    <div key={f.key} className="flex items-center gap-2">
                      <span className="text-[10px] text-gray-500 w-10 shrink-0">{f.label}</span>
                      <input
                        type="text"
                        placeholder={f.placeholder}
                        value={editForm[f.key as keyof typeof editForm]}
                        onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                        className="flex-1 border border-gray-200 rounded-lg px-2 py-1 text-xs outline-none focus:border-primary"
                      />
                    </div>
                  ))}
                  <div className="flex gap-2 pt-1">
                    <button
                      onClick={() => handleManualApprove(place.id)}
                      disabled={isActing}
                      className="flex-1 py-2 rounded-xl text-xs font-bold bg-primary text-white flex items-center justify-center gap-1"
                    >
                      {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 수정 후 승인
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="py-2 px-4 rounded-xl text-xs font-bold bg-gray-100 text-gray-600"
                    >
                      취소
                    </button>
                  </div>
                </div>
              )}

              {/* 액션 버튼 */}
              {!isEditing && (
                <div className="flex gap-2 px-3 pb-3 border-t border-gray-100 pt-2.5">
                  <a
                    href={place.kakaoMapUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 px-3 py-2 rounded-xl text-xs font-bold bg-yellow-50 text-yellow-700"
                  >
                    <ExternalLink size={12} /> 지도확인
                  </a>
                  <button
                    onClick={() => handleApprove(place.id)}
                    disabled={isActing}
                    className="flex-1 py-2 rounded-xl text-xs font-bold bg-green-50 text-green-700 flex items-center justify-center gap-1"
                  >
                    {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 승인
                  </button>
                  <button
                    onClick={() => {
                      setEditingId(place.id);
                      setEditForm({ title: '', address: '', lat: '', lng: '' });
                    }}
                    className="py-2 px-3 rounded-xl text-xs font-bold bg-blue-50 text-blue-700 flex items-center gap-1"
                  >
                    <Edit3 size={12} /> 수정
                  </button>
                  <button
                    onClick={() => handleReject(place.id)}
                    disabled={isActing}
                    className="py-2 px-3 rounded-xl text-xs font-bold bg-red-50 text-red-600 flex items-center gap-1"
                  >
                    <X size={12} /> 거절
                  </button>
                </div>
              )}
            </div>
          );
        })
      )}

      </>)}

      {/* ══════════════════════ 이미지없음 탭 ══════════════════════ */}
      {placeSubTab === 'noImage' && (<>
        <div className="flex gap-2">
          <div className="flex-1 flex items-center gap-2 bg-white border border-gray-200 rounded-xl px-3">
            <Search size={14} className="text-gray-400 shrink-0" />
            <input
              type="text"
              placeholder="상호명 또는 주소 검색..."
              value={noImageSearch}
              onChange={e => setNoImageSearch(e.target.value)}
              className="flex-1 py-2.5 text-xs outline-none bg-transparent"
            />
            {noImageSearch && <button onClick={() => setNoImageSearch('')}><X size={13} className="text-gray-400" /></button>}
          </div>
          <select
            value={noImageCategoryFilter}
            onChange={e => setNoImageCategoryFilter(e.target.value)}
            className="bg-white border border-gray-200 rounded-xl px-2 py-2 text-xs font-bold text-gray-600 outline-none"
          >
            <option value="">전체</option>
            {Array.from(new Set(noImage.map(p => p.category).filter(Boolean))).sort().map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>
        {/* 박물관/뮤지엄 자동 거절 */}
        {(() => {
          const MUSEUM_KEYWORDS = ['박물관', '뮤지엄', '미술관', '갤러리', '전시관', '기념관', '역사관', 'museum', 'gallery'];
          const museumPlaces = noImage.filter(p =>
            MUSEUM_KEYWORDS.some(kw => p.title.toLowerCase().includes(kw.toLowerCase()))
          );
          if (museumPlaces.length === 0) return null;
          return (
            <div className="rounded-2xl border border-orange-200 overflow-hidden">
              <button
                onClick={() => setShowMuseumPreview(v => !v)}
                className="w-full py-2.5 text-xs font-bold bg-orange-50 text-orange-600 flex items-center justify-center gap-1.5 hover:bg-orange-100 transition-spring"
              >
                <X size={13} />
                박물관/뮤지엄 자동 거절 ({museumPlaces.length}건)
                {showMuseumPreview ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
              </button>
              {showMuseumPreview && (
                <div className="bg-white border-t border-orange-100 p-3 space-y-2">
                  <p className="text-[11px] text-gray-500 font-medium">
                    아래 장소는 <span className="text-orange-500 font-bold">박물관·뮤지엄 계열</span>로 반려동물 출입이 어려울 수 있습니다.
                  </p>
                  <div className="space-y-1.5 max-h-60 overflow-y-auto pr-1">
                    {museumPlaces.map(p => (
                      <div key={p.id} className="flex items-center gap-2 bg-orange-50 rounded-xl px-3 py-2">
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-bold text-gray-800 truncate">{p.title}</p>
                          <p className="text-[10px] text-gray-500 truncate">{p.address}</p>
                        </div>
                        <span className="font-mono text-[10px] text-gray-400 shrink-0">#{p.id}</span>
                      </div>
                    ))}
                  </div>
                  <button
                    onClick={async () => {
                      if (!confirm(`박물관/뮤지엄 계열 ${museumPlaces.length}건을 일괄 거절하시겠습니까?`)) return;
                      setShowMuseumPreview(false);
                      setBulkLoading(true);
                      const ids = new Set(museumPlaces.map(p => p.id));
                      // 낙관적 업데이트
                      setNoImage(prev => prev.filter(p => !ids.has(p.id)));
                      setPlaces(prev => prev.filter(p => !ids.has(p.id)));
                      setAllActive(prev => prev.filter(p => !ids.has(p.id)));
                      setRejected(prev => [...museumPlaces, ...prev]);
                      try {
                        const results = await Promise.allSettled(museumPlaces.map(p => adminApi.rejectPlace(p.id)));
                        const failed = museumPlaces.filter((_, i) => results[i].status === 'rejected');
                        if (failed.length > 0) {
                          // 실패 항목 롤백
                          const failedIds = new Set(failed.map(p => p.id));
                          setNoImage(prev => [...failed, ...prev]);
                          setRejected(prev => prev.filter(p => !failedIds.has(p.id)));
                          alert(`${failed.length}건 거절 실패:\n${failed.map(p => `· ${p.title}`).join('\n')}`);
                        }
                      } finally {
                        setBulkLoading(false);
                      }
                    }}
                    disabled={bulkLoading}
                    className="w-full py-2 rounded-xl text-xs font-bold bg-orange-500 text-white flex items-center justify-center gap-1.5 hover:bg-orange-600 transition-spring disabled:opacity-50"
                  >
                    {bulkLoading ? <Loader size={12} className="animate-spin" /> : <X size={12} />}
                    {museumPlaces.length}건 일괄 거절 확정
                  </button>
                </div>
              )}
            </div>
          );
        })()}

        {noImage.filter(p =>
          (!noImageSearch || p.title.includes(noImageSearch) || p.address.includes(noImageSearch)) &&
          (!noImageCategoryFilter || p.category === noImageCategoryFilter)
        ).length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <CheckCircle size={32} className="mx-auto mb-2 opacity-30" />
            <p className="text-sm">{noImageSearch || noImageCategoryFilter ? '검색 결과가 없습니다.' : '이미지 없는 장소가 없습니다.'}</p>
          </div>
        ) : (
          noImage.filter(p =>
            (!noImageSearch || p.title.includes(noImageSearch) || p.address.includes(noImageSearch)) &&
            (!noImageCategoryFilter || p.category === noImageCategoryFilter)
          ).map(place => {
                const isActing = actionLoading === place.id;
                const isExpanded = expandedEditIds.has(place.id);
                const edit = getEdit(place.id);
                const hasChanges = Object.values(edit).some(v => v.trim());
                return (
                  <div key={place.id} className="bg-white rounded-2xl shadow-sm border border-blue-100 overflow-hidden">
                    <div className="flex gap-3 p-3">
                      <div className="w-14 h-14 rounded-xl bg-blue-50 shrink-0 flex items-center justify-center overflow-hidden">
                        {edit.imageUrl.trim()
                          ? <img src={edit.imageUrl} alt="" className="w-full h-full object-cover" onError={e => (e.currentTarget.style.display = 'none')} />
                          : <ImageOff size={20} className="text-blue-300" />
                        }
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-bold text-gray-800 truncate">{edit.title || place.title}</p>
                        <p className="text-[11px] text-gray-500 mt-0.5 truncate">{edit.address || place.address}</p>
                        <p className="text-[11px] text-gray-400">{place.category}</p>
                      </div>
                      <button
                        onClick={() => toggleExpandEdit(place.id)}
                        className={`self-start p-1.5 rounded-lg transition-spring ${isExpanded ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-500'}`}
                      >
                        <Edit3 size={13} />
                      </button>
                    </div>

                    {isExpanded && (
                      <div className="px-3 pb-3 border-t border-gray-100 pt-3 space-y-2">
                        <p className="text-[11px] font-bold text-gray-500 mb-1">정보 수정 (빈칸은 기존값 유지)</p>
                        {([
                          { field: 'title',    label: '명칭',    placeholder: place.title,   type: 'text' },
                          { field: 'address',  label: '주소',    placeholder: place.address,  type: 'text' },
                          { field: 'phone',    label: '전화번호', placeholder: place.phone ?? '미등록', type: 'tel' },
                          { field: 'homepage', label: '홈페이지', placeholder: place.homepage ?? '미등록', type: 'url' },
                          { field: 'imageUrl', label: '이미지',  placeholder: '이미지 URL 입력', type: 'url' },
                        ] as { field: keyof InlineEditForm; label: string; placeholder: string; type: string }[]).map(f => (
                          <div key={f.field} className="flex items-center gap-2">
                            <span className="text-[10px] text-gray-500 w-12 shrink-0">{f.label}</span>
                            <input
                              type={f.type}
                              placeholder={f.placeholder}
                              value={edit[f.field]}
                              onChange={e => setEditField(place.id, f.field, e.target.value)}
                              className="flex-1 border border-gray-200 rounded-lg px-2 py-1.5 text-xs outline-none focus:border-primary"
                            />
                          </div>
                        ))}
                        <div className="flex gap-2 pt-1">
                          <a href={place.kakaoMapUrl} target="_blank" rel="noreferrer"
                            className="py-2 px-3 rounded-xl text-xs font-bold bg-yellow-50 text-yellow-700 flex items-center gap-1">
                            <ExternalLink size={11} /> 지도
                          </a>
                          <button
                            onClick={() => handleInlineSave(place.id)}
                            disabled={isActing || !hasChanges}
                            className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-500 text-white flex items-center justify-center gap-1 disabled:opacity-40"
                          >
                            {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />}
                            {edit.imageUrl.trim() ? '이미지 저장 (공개 전환)' : '저장'}
                          </button>
                          <button onClick={() => toggleExpandEdit(place.id)}
                            className="py-2 px-3 rounded-xl text-xs font-bold bg-gray-100 text-gray-600">
                            취소
                          </button>
                        </div>
                      </div>
                    )}

                    {!isExpanded && (
                      <div className="px-3 pb-3 flex gap-2 border-t border-gray-100 pt-2">
                        <a href={place.kakaoMapUrl} target="_blank" rel="noreferrer"
                          className="py-2 px-3 rounded-xl text-xs font-bold bg-yellow-50 text-yellow-700 flex items-center gap-1">
                          <ExternalLink size={11} /> 지도
                        </a>
                        <button
                          onClick={() => toggleExpandEdit(place.id)}
                          className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-50 text-blue-700 flex items-center justify-center gap-1"
                        >
                          <Edit3 size={12} /> 정보 수정
                        </button>
                        <button
                          onClick={() => handleReject(place.id)}
                          disabled={isActing}
                          className="py-2 px-3 rounded-xl text-xs font-bold bg-red-50 text-red-600 flex items-center gap-1"
                        >
                          <X size={12} /> 거절
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
      </>)}

      {/* ══════════════════════ 거절됨 탭 ══════════════════════ */}
      {placeSubTab === 'rejected' && (<>
        <div className="flex gap-2">
          <div className="flex-1 flex items-center gap-2 bg-white border border-gray-200 rounded-xl px-3">
            <Search size={14} className="text-gray-400 shrink-0" />
            <input
              type="text"
              placeholder="상호명 또는 주소 검색..."
              value={rejectedSearch}
              onChange={e => setRejectedSearch(e.target.value)}
              className="flex-1 py-2.5 text-xs outline-none bg-transparent"
            />
            {rejectedSearch && <button onClick={() => setRejectedSearch('')}><X size={13} className="text-gray-400" /></button>}
          </div>
          <select
            value={rejectedCategoryFilter}
            onChange={e => setRejectedCategoryFilter(e.target.value)}
            className="bg-white border border-gray-200 rounded-xl px-2 py-2 text-xs font-bold text-gray-600 outline-none"
          >
            <option value="">전체</option>
            {Array.from(new Set(rejected.map(p => p.category).filter(Boolean))).sort().map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>
            {/* 전체선택 + 일괄 복구 */}
            {rejected.length > 0 && (
              <div className="flex items-center justify-between px-1">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedRejected.size === rejected.length}
                    onChange={toggleAllRejected}
                    className="w-4 h-4 accent-red-500 cursor-pointer"
                  />
                  <span className="text-xs text-gray-600 font-medium">전체선택</span>
                </label>
                {selectedRejected.size > 0 && (
                  <button
                    onClick={handleBulkRestore}
                    disabled={bulkLoading}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-green-500 text-white flex items-center gap-1 disabled:opacity-50"
                  >
                    {bulkLoading ? <Loader size={11} className="animate-spin" /> : <Check size={11} />}
                    일괄 복구 ({selectedRejected.size})
                  </button>
                )}
              </div>
            )}

            {rejected.filter(p =>
              (!rejectedSearch || p.title.includes(rejectedSearch) || p.address.includes(rejectedSearch)) &&
              (!rejectedCategoryFilter || p.category === rejectedCategoryFilter)
            ).length === 0 ? (
              <p className="text-center text-xs text-gray-400 py-6">{rejectedSearch || rejectedCategoryFilter ? '검색 결과가 없습니다.' : '거절된 장소가 없습니다.'}</p>
            ) : (
              rejected.filter(p =>
                (!rejectedSearch || p.title.includes(rejectedSearch) || p.address.includes(rejectedSearch)) &&
                (!rejectedCategoryFilter || p.category === rejectedCategoryFilter)
              ).map(place => {
                const isActing = actionLoading === place.id;
                const isExpanded = expandedEditIds.has(place.id);
                const edit = getEdit(place.id);
                const hasChanges = Object.values(edit).some(v => v.trim());
                return (
                  <div key={place.id} className={`bg-white rounded-2xl shadow-sm overflow-hidden border transition-all ${selectedRejected.has(place.id) ? 'border-red-400 ring-1 ring-red-200' : 'border-red-100'}`}>
                    <div className="flex gap-3 p-3">
                      <div className="flex items-start pt-1">
                        <input type="checkbox" checked={selectedRejected.has(place.id)}
                          onChange={() => toggleRejected(place.id)}
                          className="w-4 h-4 accent-red-500 cursor-pointer" />
                      </div>
                      <div className="w-14 h-14 rounded-xl overflow-hidden bg-gray-100 shrink-0 flex items-center justify-center">
                        {(edit.imageUrl.trim() || place.imageUrl)
                          ? <img src={edit.imageUrl.trim() || place.imageUrl!} alt="" className="w-full h-full object-cover" onError={e => (e.currentTarget.style.display = 'none')} />
                          : <ImageOff size={18} className="text-gray-300" />
                        }
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-1">
                          <p className="text-sm font-bold text-gray-800 truncate">{edit.title || place.title}</p>
                          <span className="text-[10px] bg-red-100 text-red-600 px-1.5 py-0.5 rounded-full font-bold shrink-0">거절</span>
                        </div>
                        <p className="text-[11px] text-gray-500 mt-0.5 truncate">{edit.address || place.address}</p>
                        <p className="text-[11px] text-gray-400">{place.category}</p>
                      </div>
                      <button
                        onClick={() => toggleExpandEdit(place.id)}
                        className={`self-start p-1.5 rounded-lg transition-spring ${isExpanded ? 'bg-red-100 text-red-600' : 'bg-gray-100 text-gray-500'}`}
                      >
                        <Edit3 size={13} />
                      </button>
                    </div>

                    {isExpanded && (
                      <div className="px-3 pb-3 border-t border-gray-100 pt-3 space-y-2">
                        <p className="text-[11px] font-bold text-gray-500 mb-1">정보 수정 (빈칸은 기존값 유지)</p>
                        {([
                          { field: 'title',    label: '명칭',    placeholder: place.title,   type: 'text' },
                          { field: 'address',  label: '주소',    placeholder: place.address,  type: 'text' },
                          { field: 'phone',    label: '전화번호', placeholder: place.phone ?? '미등록', type: 'tel' },
                          { field: 'homepage', label: '홈페이지', placeholder: place.homepage ?? '미등록', type: 'url' },
                          { field: 'imageUrl', label: '이미지',  placeholder: place.imageUrl ?? '이미지 URL 입력', type: 'url' },
                        ] as { field: keyof InlineEditForm; label: string; placeholder: string; type: string }[]).map(f => (
                          <div key={f.field} className="flex items-center gap-2">
                            <span className="text-[10px] text-gray-500 w-12 shrink-0">{f.label}</span>
                            <input
                              type={f.type}
                              placeholder={f.placeholder}
                              value={edit[f.field]}
                              onChange={e => setEditField(place.id, f.field, e.target.value)}
                              className="flex-1 border border-gray-200 rounded-lg px-2 py-1.5 text-xs outline-none focus:border-primary"
                            />
                          </div>
                        ))}
                        <div className="flex gap-2 pt-1">
                          <a href={place.kakaoMapUrl} target="_blank" rel="noreferrer"
                            className="py-2 px-3 rounded-xl text-xs font-bold bg-yellow-50 text-yellow-700 flex items-center gap-1">
                            <ExternalLink size={11} /> 지도
                          </a>
                          {hasChanges && (
                            <button
                              onClick={() => handleInlineSave(place.id)}
                              disabled={isActing}
                              className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-50 text-blue-700 flex items-center justify-center gap-1 disabled:opacity-50"
                            >
                              {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 저장
                            </button>
                          )}
                          <button
                            onClick={() => handleRestoreApprove(place.id)}
                            disabled={isActing}
                            className="flex-1 py-2 rounded-xl text-xs font-bold bg-green-50 text-green-700 flex items-center justify-center gap-1 disabled:opacity-50"
                          >
                            {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 복구 승인
                          </button>
                        </div>
                      </div>
                    )}

                    {!isExpanded && (
                      <div className="flex gap-2 px-3 pb-3 border-t border-gray-100 pt-2">
                        <a href={place.kakaoMapUrl} target="_blank" rel="noreferrer"
                          className="py-2 px-3 rounded-xl text-xs font-bold bg-yellow-50 text-yellow-700 flex items-center gap-1">
                          <ExternalLink size={11} /> 지도
                        </a>
                        <button
                          onClick={() => toggleExpandEdit(place.id)}
                          className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-50 text-blue-700 flex items-center justify-center gap-1"
                        >
                          <Edit3 size={12} /> 정보 수정
                        </button>
                        <button
                          onClick={() => handleRestoreApprove(place.id)}
                          disabled={isActing}
                          className="flex-1 py-2 rounded-xl text-xs font-bold bg-green-50 text-green-700 flex items-center justify-center gap-1"
                        >
                          {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 복구 승인
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
      </>)}

      {/* ══════════════════════ 전체관리 탭 ══════════════════════ */}
      {placeSubTab === 'active' && (
        <div className="space-y-2">
          {allActiveLoading ? (
            <div className="flex justify-center py-8"><Loader size={20} className="text-indigo-400 animate-spin" /></div>
          ) : (
            <>
              {/* 중복 자동 정리 버튼 */}
                {(() => {
                  const titleMap = allActive.filter(p => !resolvedDupIds.has(p.id)).reduce<Record<string, PendingPlaceDto[]>>((acc, p) => {
                    (acc[p.title] = acc[p.title] ?? []).push(p);
                    return acc;
                  }, {});
                  // 중복 그룹 중 이미지 없는 것 (그룹에 이미지 있는 버전이 존재하는 경우만)
                  const toDelete = Object.values(titleMap)
                    .filter(group => group.length > 1 && group.some(p => p.imageUrl))
                    .flatMap(group => group.filter(p => !p.imageUrl));
                  if (toDelete.length === 0) return null;
                  return (
                    <div className="rounded-2xl border border-red-200 overflow-hidden">
                      {/* 토글 버튼 */}
                      <button
                        onClick={() => setShowDupPreview(v => !v)}
                        className="w-full py-2.5 text-xs font-bold bg-red-50 text-red-600 flex items-center justify-center gap-1.5 hover:bg-red-100 transition-spring"
                      >
                        <Trash2 size={13} />
                        이미지 없는 중복 자동 정리 ({toDelete.length}건)
                        {showDupPreview ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
                      </button>

                      {/* 프리뷰 패널 */}
                      {showDupPreview && (
                        <div className="bg-white border-t border-red-100 p-3 space-y-2">
                          <p className="text-[11px] text-gray-500 font-medium">
                            아래 장소는 <span className="text-red-500 font-bold">이미지가 없고</span>, 동일 명칭의 이미지 있는 버전이 존재합니다.
                          </p>
                          <div className="space-y-1.5 max-h-60 overflow-y-auto pr-1">
                            {toDelete.map(p => (
                              <div key={p.id} className="flex items-center gap-2 bg-red-50 rounded-xl px-3 py-2">
                                <div className="flex-1 min-w-0">
                                  <p className="text-xs font-bold text-gray-800 truncate">{p.title}</p>
                                  <p className="text-[10px] text-gray-500 truncate">{p.address}</p>
                                </div>
                                <span className="font-mono text-[10px] text-gray-400 shrink-0">#{p.id}</span>
                              </div>
                            ))}
                          </div>
                          <button
                            onClick={async () => {
                              setShowDupPreview(false);
                              setAllActiveLoading(true);
                              // 낙관적 업데이트
                              const ids = new Set(toDelete.map(p => p.id));
                              const removeDeleted = (prev: PendingPlaceDto[]) => prev.filter(p => !ids.has(p.id));
                              setAllActive(removeDeleted);
                              setPlaces(removeDeleted);
                              setRejected(removeDeleted);
                              setNoImage(removeDeleted);
                              try {
                                const results = await Promise.allSettled(toDelete.map(p => adminApi.deletePlace(p.id)));
                                const failed = toDelete.filter((_, i) => results[i].status === 'rejected');
                                if (failed.length > 0) {
                                  // 실패 항목만 롤백
                                  setAllActive(prev => [...prev, ...failed]);
                                  setNoImage(prev => [...prev, ...failed]);
                                  alert(`${failed.length}건 삭제 실패:\n${failed.map(p => `· ${p.title} (#${p.id})`).join('\n')}`);
                                }
                              } finally {
                                setAllActiveLoading(false);
                              }
                            }}
                            className="w-full py-2 rounded-xl text-xs font-bold bg-red-500 text-white flex items-center justify-center gap-1.5 hover:bg-red-600 transition-spring"
                          >
                            <Trash2 size={12} /> {toDelete.length}건 전체 삭제 확정
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })()}

                {/* 검색 + 이미지 필터 + 카테고리 필터 */}
                <div className="flex gap-2">
                  <div className="flex-1 flex items-center gap-2 bg-white border border-gray-200 rounded-xl px-3">
                    <Search size={14} className="text-gray-400 shrink-0" />
                    <input
                      type="text"
                      placeholder="상호명 또는 주소 검색..."
                      value={editSearch}
                      onChange={e => { setEditSearch(e.target.value); setEditPageSize(30); }}
                      className="flex-1 py-2.5 text-xs outline-none bg-transparent"
                    />
                  </div>
                  <select
                    value={editCategoryFilter}
                    onChange={e => { setEditCategoryFilter(e.target.value); setEditPageSize(30); }}
                    className="bg-white border border-gray-200 rounded-xl px-2 py-2 text-xs font-bold text-gray-600 outline-none"
                  >
                    <option value="">전체</option>
                    {Array.from(new Set(allActive.map(p => p.category).filter(Boolean))).sort().map(cat => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                  <select
                    value={editImageFilter}
                    onChange={e => { setEditImageFilter(e.target.value as typeof editImageFilter); setEditPageSize(30); }}
                    className="bg-white border border-gray-200 rounded-xl px-2 py-2 text-xs font-bold text-gray-600 outline-none"
                  >
                    <option value="all">전체</option>
                    <option value="hasImage">이미지 있음</option>
                    <option value="noImage">이미지 없음</option>
                    <option value="duplicate">중복 의심</option>
                  </select>
                </div>

                {/* 장소 목록 */}
                {(() => {
                  // 중복 의심 필터: 동일 title 2개 이상 (해제된 항목 제외)
                  const activeForDup = allActive.filter(p => !resolvedDupIds.has(p.id));
                  const titleCounts = activeForDup.reduce<Record<string, number>>((acc, p) => {
                    acc[p.title] = (acc[p.title] ?? 0) + 1;
                    return acc;
                  }, {});
                  const duplicateTitles = new Set(Object.entries(titleCounts).filter(([, c]) => c > 1).map(([t]) => t));

                  return allActive
                    .filter(p => {
                      const matchSearch = !editSearch || p.title.includes(editSearch) || p.address.includes(editSearch);
                      const matchImage =
                        editImageFilter === 'all' ? true
                        : editImageFilter === 'hasImage' ? !!p.imageUrl
                        : editImageFilter === 'noImage' ? !p.imageUrl
                        : duplicateTitles.has(p.title); // 'duplicate'
                      const matchCategory = !editCategoryFilter || p.category === editCategoryFilter;
                      return matchSearch && matchImage && matchCategory;
                    })
                    .slice(0, editPageSize)
                    .map(place => {
                      const isEditing = editingPlaceId === place.id;
                      const isActing = actionLoading === place.id;
                      const isDuplicate = duplicateTitles.has(place.title);
                      return (
                      <div key={place.id} className={`bg-white rounded-2xl shadow-sm border overflow-hidden transition-all ${isEditing ? 'border-indigo-300' : isDuplicate && editImageFilter === 'duplicate' ? 'border-red-300' : 'border-gray-100'}`}>
                        <div className="flex gap-3 p-3">
                          <div className="w-12 h-12 rounded-xl overflow-hidden bg-gray-100 shrink-0 flex items-center justify-center">
                            {place.imageUrl
                              ? <img src={place.imageUrl} alt="" className="w-full h-full object-cover" onError={e => { e.currentTarget.style.display = 'none'; e.currentTarget.parentElement!.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>'; }} />
                              : <ImageOff size={16} className="text-gray-300" />}
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <p className="text-sm font-bold text-gray-800 truncate">{place.title}</p>
                              {!place.imageUrl && <span className="text-[9px] bg-orange-100 text-orange-600 px-1 py-0.5 rounded font-bold shrink-0">이미지없음</span>}
                              {isDuplicate && <span className="text-[9px] bg-red-100 text-red-600 px-1 py-0.5 rounded font-bold shrink-0">중복</span>}
                            </div>
                            <p className="text-[11px] text-gray-500 truncate">{place.address}</p>
                            <p className="text-[11px] text-gray-400">{place.category} <span className="text-gray-300">·</span> <span className="font-mono text-gray-300">#{place.id}</span></p>
                          </div>
                          <div className="flex flex-col gap-1 shrink-0">
                            <button
                              onClick={() => {
                                if (isEditing) { setEditingPlaceId(null); return; }
                                setEditingPlaceId(place.id);
                                setEditPlaceForm({ title: place.title, address: place.address, phone: place.phone ?? '', homepage: place.homepage ?? '', imageUrl: place.imageUrl ?? '' });
                              }}
                              className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1 ${isEditing ? 'bg-gray-100 text-gray-500' : 'bg-indigo-50 text-indigo-700'}`}
                            >
                              {isEditing ? <X size={12} /> : <Edit3 size={12} />}
                              {isEditing ? '취소' : '수정'}
                            </button>
                            {editImageFilter === 'duplicate' && !isEditing && (
                              <button
                                onClick={async () => {
                                  if (!window.confirm(`"${place.title}" (#${place.id})을 영구 삭제하시겠습니까?`)) return;
                                  setActionLoading(place.id);
                                  // 낙관적 업데이트 — 먼저 UI에서 제거
                                  const removeById = (prev: PendingPlaceDto[]) => prev.filter(p => p.id !== place.id);
                                  setAllActive(removeById);
                                  setPlaces(removeById);
                                  setRejected(removeById);
                                  setNoImage(removeById);
                                  try {
                                    await adminApi.deletePlace(place.id);
                                  } catch (err) {
                                    // 실패 시 롤백
                                    setAllActive(prev => [...prev, place]);
                                    setNoImage(prev => [...prev, place]);
                                    alert(`삭제 실패 (#${place.id}): 백엔드 응답 오류`);
                                    console.error('deletePlace error:', err);
                                  } finally {
                                    setActionLoading(null);
                                  }
                                }}
                                disabled={isActing}
                                className="px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1 bg-red-50 text-red-600 disabled:opacity-40"
                              >
                                {isActing ? <Loader size={12} className="animate-spin" /> : <Trash2 size={12} />}
                                삭제
                              </button>
                            )}
                          </div>
                        </div>

                        {isEditing && (
                          <div className="px-3 pb-3 border-t border-gray-100 pt-3 space-y-2">
                            {([
                              { label: '상호명', key: 'title',    placeholder: place.title },
                              { label: '주소',   key: 'address',  placeholder: place.address },
                              { label: '전화',   key: 'phone',    placeholder: place.phone ?? '없음' },
                              { label: '홈페이지', key: 'homepage', placeholder: place.homepage ?? '없음' },
                              { label: '이미지', key: 'imageUrl', placeholder: place.imageUrl ?? '없음' },
                            ] as { label: string; key: keyof typeof editPlaceForm; placeholder: string }[]).map(f => (
                              <div key={f.key} className="flex items-center gap-2">
                                <span className="text-[10px] text-gray-500 w-12 shrink-0">{f.label}</span>
                                <input
                                  type="text"
                                  placeholder={f.placeholder}
                                  value={editPlaceForm[f.key]}
                                  onChange={e => setEditPlaceForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                                  className="flex-1 border border-gray-200 rounded-lg px-2 py-1.5 text-xs outline-none focus:border-indigo-400"
                                />
                              </div>
                            ))}
                            <button
                              onClick={() => handleEditSave(place.id)}
                              disabled={isActing}
                              className="w-full py-2 rounded-xl text-xs font-bold bg-indigo-500 text-white flex items-center justify-center gap-1 disabled:opacity-50"
                            >
                              {isActing ? <Loader size={12} className="animate-spin" /> : <Check size={12} />} 저장
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  });
                })()}

                {(() => {
                  const titleCounts2 = allActive.reduce<Record<string, number>>((acc, p) => { acc[p.title] = (acc[p.title] ?? 0) + 1; return acc; }, {});
                  const dupTitles2 = new Set(Object.entries(titleCounts2).filter(([, c]) => c > 1).map(([t]) => t));
                  const filtered = allActive.filter(p => {
                    const matchSearch = !editSearch || p.title.includes(editSearch) || p.address.includes(editSearch);
                    const matchImage =
                      editImageFilter === 'all' ? true
                      : editImageFilter === 'hasImage' ? !!p.imageUrl
                      : editImageFilter === 'noImage' ? !p.imageUrl
                      : dupTitles2.has(p.title);
                    const matchCategory = !editCategoryFilter || p.category === editCategoryFilter;
                    return matchSearch && matchImage && matchCategory;
                  });
                  if (filtered.length === 0) return <p className="text-center text-xs text-gray-400 py-6">검색 결과가 없습니다.</p>;
                  if (filtered.length > editPageSize) return (
                    <button
                      onClick={() => setEditPageSize(v => v + 30)}
                      className="w-full py-2.5 rounded-xl text-xs font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 transition-spring"
                    >
                      더 보기 ({editPageSize}/{filtered.length}건 표시)
                    </button>
                  );
                  return null;
                })()}
            </>
          )}
        </div>
      )}

      {/* ══════════════════════ 신규추가 탭 ══════════════════════ */}
      {placeSubTab === 'create' && (
        <div className="space-y-4">

          {/* ── 단계 표시 바 ── */}
          <div className="flex items-center gap-0">
            {(['form','preview','done'] as const).map((s, i) => {
              const labels = ['① 기본정보', '② AI 분석', '③ 등록완료'];
              const isDone = createStep === 'done' ? true : createStep === 'preview' ? i < 2 : i < 1;
              const isActive = (createStep === 'form' || createStep === 'analyzing') ? i === 0
                             : createStep === 'preview' ? i === 1
                             : i === 2;
              return (
                <React.Fragment key={s}>
                  <div className={`flex-1 text-center py-1.5 text-[10px] font-bold rounded-full transition-spring ${
                    isDone || isActive ? 'bg-green-500 text-white' : 'bg-gray-100 text-gray-400'
                  }`}>
                    {labels[i]}
                  </div>
                  {i < 2 && <div className={`w-4 h-0.5 shrink-0 ${isDone ? 'bg-green-400' : 'bg-gray-200'}`} />}
                </React.Fragment>
              );
            })}
          </div>

          {/* ──────── STEP 1: 기본정보 입력 ──────── */}
          {(createStep === 'form' || createStep === 'analyzing') && (
            <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100 space-y-3">
              <div className="flex items-center gap-2 mb-1">
                <MapPin size={14} className="text-green-600" />
                <p className="text-xs font-bold text-gray-700">기본 정보 입력</p>
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">장소명 <span className="text-red-400">*</span></label>
                <input value={createForm.title} onChange={e => setCreateForm(p => ({ ...p, title: e.target.value }))}
                  placeholder="예: 멍냥카페 성수점"
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400" />
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">카테고리 <span className="text-red-400">*</span></label>
                <select value={createForm.category} onChange={e => setCreateForm(p => ({ ...p, category: e.target.value }))}
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400 bg-white">
                  <option value="PLACE">PLACE (관광·놀이)</option>
                  <option value="STAY">STAY (숙박)</option>
                  <option value="DINING">DINING (카페·식당)</option>
                </select>
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">주소 <span className="text-red-400">*</span></label>
                <input value={createForm.address} onChange={e => setCreateForm(p => ({ ...p, address: e.target.value }))}
                  placeholder="예: 서울 성동구 성수동1가 13-19"
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400" />
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">전화번호</label>
                <input value={createForm.phone} onChange={e => setCreateForm(p => ({ ...p, phone: e.target.value }))}
                  placeholder="예: 02-1234-5678"
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400" />
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">홈페이지 URL</label>
                <input value={createForm.homepage} onChange={e => setCreateForm(p => ({ ...p, homepage: e.target.value }))}
                  placeholder="예: https://example.com"
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400" />
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">이미지 URL</label>
                <input value={createForm.imageUrl} onChange={e => setCreateForm(p => ({ ...p, imageUrl: e.target.value }))}
                  placeholder="예: https://cdn.example.com/img.jpg"
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400" />
                {createForm.imageUrl && (
                  <img src={createForm.imageUrl} alt="미리보기"
                    className="mt-2 w-full h-32 object-cover rounded-xl border border-gray-100"
                    onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                )}
              </div>

              <div>
                <label className="text-[11px] font-bold text-gray-500 mb-1 block">장소 설명</label>
                <textarea value={createForm.description} onChange={e => setCreateForm(p => ({ ...p, description: e.target.value }))}
                  placeholder="장소에 대한 간단한 설명을 입력하세요." rows={3}
                  className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-green-400 resize-none" />
              </div>

              {/* AI 보강 분석 실행 버튼 */}
              <button
                disabled={createStep === 'analyzing' || !createForm.title.trim() || !createForm.address.trim() || !createForm.category}
                onClick={async () => {
                  setCreateStep('analyzing');
                  setAnalyzeResult(null);
                  try {
                    const result = await adminApi.analyzePlacePreview({
                      title:       createForm.title.trim(),
                      address:     createForm.address.trim(),
                      phone:       createForm.phone.trim()    || undefined,
                      homepage:    createForm.homepage.trim() || undefined,
                      imageUrl:    createForm.imageUrl.trim() || undefined,
                      description: createForm.description.trim() || undefined,
                    });
                    setAnalyzeResult(result);
                    setCreateStep('preview');
                  } catch (err: any) {
                    const msg = err?.response
                      ? `서버 오류 (${err.response.status})`
                      : 'BE 서버에 연결할 수 없습니다.\n서버가 실행 중인지 확인하세요.';
                    alert(`AI 분석 실패\n${msg}`);
                    setCreateStep('form');
                  }
                }}
                className="w-full py-3 rounded-xl text-sm font-bold bg-indigo-500 text-white flex items-center justify-center gap-2 disabled:opacity-40 transition-spring hover:bg-indigo-600"
              >
                {createStep === 'analyzing'
                  ? <><Loader size={14} className="animate-spin" /> Naver 분석 중... (10~20초)</>
                  : <><Sparkles size={14} /> AI 보강 분석 실행</>}
              </button>
              <p className="text-[10px] text-gray-400 text-center">Naver 블로그 API로 운영 여부·화제성·감성 점수를 분석합니다</p>
            </div>
          )}

          {/* ──────── STEP 2: AI 분석 결과 미리보기 ──────── */}
          {createStep === 'preview' && analyzeResult && (
            <div className="space-y-3">
              {/* 분석 결과 카드 */}
              <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100 space-y-3">
                <div className="flex items-center gap-2">
                  <Sparkles size={14} className="text-indigo-500" />
                  <p className="text-xs font-bold text-gray-700">AI 분석 결과</p>
                  <span className={`ml-auto text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    analyzeResult.naverVerified ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'
                  }`}>
                    Naver {analyzeResult.naverVerified ? '운영 확인' : '미검색'}
                  </span>
                </div>

                {/* AI 별점 */}
                <div className="flex items-center justify-between p-3 bg-indigo-50 rounded-xl">
                  <span className="text-xs font-bold text-indigo-700">예상 AI 별점</span>
                  <div className="flex items-center gap-1">
                    <Star size={14} className="text-amber-400 fill-amber-400" />
                    <span className="text-lg font-black text-indigo-800">{analyzeResult.aiRating?.toFixed(1) ?? '—'}</span>
                    <span className="text-[11px] text-indigo-400 font-medium">/ 5.0</span>
                  </div>
                </div>

                {/* 좌표 */}
                <div className="flex items-center justify-between text-xs">
                  <span className="text-gray-500">자동 획득 좌표 (Kakao)</span>
                  {analyzeResult.geocodeSuccess
                    ? <span className="font-bold text-gray-700 font-mono text-[11px]">{analyzeResult.lat?.toFixed(5)}, {analyzeResult.lng?.toFixed(5)}</span>
                    : <span className="text-red-500 font-bold text-[11px]">주소 변환 실패 — 수동 입력 필요</span>}
                </div>

                {/* 블로그 수 */}
                <div className="flex items-center justify-between text-xs">
                  <span className="text-gray-500">네이버 블로그 검색 수</span>
                  <span className="font-bold text-gray-700">{analyzeResult.blogCount.toLocaleString()}건</span>
                </div>

                {/* 좌표 수동 입력 (Geocoding 실패 시) */}
                {!analyzeResult.geocodeSuccess && (
                  <div className="grid grid-cols-2 gap-2 p-3 bg-red-50 rounded-xl border border-red-200">
                    <div>
                      <label className="text-[10px] font-bold text-red-500 mb-1 block">위도 (lat) *</label>
                      <input
                        placeholder="37.5446" type="number" step="any"
                        id="manual-lat"
                        className="w-full border border-red-200 rounded-lg px-2 py-1.5 text-xs outline-none focus:border-red-400"
                      />
                    </div>
                    <div>
                      <label className="text-[10px] font-bold text-red-500 mb-1 block">경도 (lng) *</label>
                      <input
                        placeholder="127.0558" type="number" step="any"
                        id="manual-lng"
                        className="w-full border border-red-200 rounded-lg px-2 py-1.5 text-xs outline-none focus:border-red-400"
                      />
                    </div>
                  </div>
                )}

                {/* 긍정 태그 */}
                {analyzeResult.blogPositiveTags && (
                  <div>
                    <p className="text-[10px] font-bold text-green-600 mb-1">긍정 키워드</p>
                    <div className="flex flex-wrap gap-1">
                      {analyzeResult.blogPositiveTags.split(',').map(t => (
                        <span key={t} className="text-[10px] bg-green-50 text-green-700 px-2 py-0.5 rounded-full font-medium">{t}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* 부정 태그 */}
                {analyzeResult.blogNegativeTags && (
                  <div>
                    <p className="text-[10px] font-bold text-red-500 mb-1">부정 키워드</p>
                    <div className="flex flex-wrap gap-1">
                      {analyzeResult.blogNegativeTags.split(',').map(t => (
                        <span key={t} className="text-[10px] bg-red-50 text-red-600 px-2 py-0.5 rounded-full font-medium">{t}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* 등록 정보 요약 */}
                <div className="pt-2 border-t border-gray-100 space-y-1">
                  <p className="text-[10px] font-bold text-gray-400 mb-1">등록 예정 정보</p>
                  <p className="text-xs text-gray-700 font-bold">{createForm.title}</p>
                  <p className="text-[11px] text-gray-500">{createForm.address}</p>
                  <p className="text-[11px] text-gray-400">{createForm.category} · {analyzeResult.geocodeSuccess ? `${analyzeResult.lat?.toFixed(4)}, ${analyzeResult.lng?.toFixed(4)}` : '좌표 수동 입력'}</p>
                </div>
              </div>

              {/* 버튼 2개 */}
              <div className="flex gap-2">
                <button
                  onClick={() => { setCreateStep('form'); setAnalyzeResult(null); }}
                  className="flex-1 py-2.5 rounded-xl text-xs font-bold bg-gray-100 text-gray-600 flex items-center justify-center gap-1"
                >
                  <ArrowLeft size={12} /> 수정
                </button>
                <button
                  disabled={createSaving}
                  onClick={async () => {
                    let lat = analyzeResult.lat;
                    let lng = analyzeResult.lng;
                    if (!analyzeResult.geocodeSuccess) {
                      const latEl = document.getElementById('manual-lat') as HTMLInputElement;
                      const lngEl = document.getElementById('manual-lng') as HTMLInputElement;
                      lat = latEl ? parseFloat(latEl.value) : NaN as any;
                      lng = lngEl ? parseFloat(lngEl.value) : NaN as any;
                    }
                    if (lat == null || lng == null || isNaN(lat) || isNaN(lng)) {
                      alert('좌표를 입력해 주세요.');
                      return;
                    }
                    setCreateSaving(true);
                    try {
                      const created = await adminApi.createPlace({
                        title:       createForm.title.trim(),
                        category:    createForm.category,
                        address:     createForm.address.trim(),
                        lat:         lat!,
                        lng:         lng!,
                        phone:       createForm.phone.trim()       || undefined,
                        homepage:    createForm.homepage.trim()    || undefined,
                        imageUrl:    createForm.imageUrl.trim()    || undefined,
                        description: createForm.description.trim() || undefined,
                        aiRating:    analyzeResult.aiRating,
                      });
                      setCreateSuccess(`"${created.title}" 등록 완료 (ID: ${created.id} · AI별점 ${analyzeResult.aiRating?.toFixed(1)})`);
                      setCreateForm(EMPTY_CREATE);
                      setAnalyzeResult(null);
                      setCreateStep('done');
                      fetchPlaces();
                    } catch {
                      alert('등록 중 오류가 발생했습니다.');
                    } finally {
                      setCreateSaving(false);
                    }
                  }}
                  className="flex-[2] py-2.5 rounded-xl text-xs font-bold bg-green-500 text-white flex items-center justify-center gap-1 disabled:opacity-50"
                >
                  {createSaving
                    ? <><Loader size={12} className="animate-spin" /> 등록 중...</>
                    : <><CheckCircle size={12} /> DB 등록 확정</>}
                </button>
              </div>
            </div>
          )}

          {/* ──────── STEP 3: 등록 완료 ──────── */}
          {createStep === 'done' && createSuccess && (
            <div className="space-y-3">
              <div className="flex flex-col items-center gap-3 p-6 bg-green-50 border border-green-200 rounded-2xl">
                <CheckCircle size={32} className="text-green-500" />
                <p className="text-sm font-bold text-green-800 text-center">{createSuccess}</p>
                <p className="text-[11px] text-green-600">일반 사용자에게 즉시 노출됩니다.</p>
              </div>
              <button
                onClick={() => { setCreateStep('form'); setCreateSuccess(null); }}
                className="w-full py-2.5 rounded-xl text-sm font-bold bg-green-500 text-white flex items-center justify-center gap-2"
              >
                <Plus size={14} /> 다음 장소 추가
              </button>
            </div>
          )}

        </div>
      )}

      </div>
    </motion.div>
  );
}

function DMsTab({ posts }: { posts: FeedPost[] }) {
  const { markDMRead } = useFeedStore();
  const allDMs = posts
    .flatMap(p => p.dmList.map(d => ({ ...d, postId: p.id, postUser: p.user })))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const unreadCount = allDMs.filter(d => !d.isRead).length;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-gray-800">전체 DM</h3>
        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <span className="text-[10px] bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-bold">
              읽지않음 {unreadCount}
            </span>
          )}
          <span className="text-xs text-gray-500 font-medium">{allDMs.length}개</span>
        </div>
      </div>

      {allDMs.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <Mail size={32} className="mx-auto mb-2 opacity-30" />
          <p className="text-sm">아직 DM이 없습니다.</p>
        </div>
      ) : (
        allDMs.map(d => (
          <div
            key={`${d.postId}-${d.id}`}
            className={`bg-white rounded-2xl p-3 shadow-sm border transition-spring ${
              d.isRead ? 'border-transparent' : 'border-blue-200 bg-blue-50/30'
            }`}
          >
            <div className="flex items-start gap-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                d.isRead ? 'bg-gray-100' : 'bg-blue-100'
              }`}>
                <Mail size={14} className={d.isRead ? 'text-gray-400' : 'text-blue-600'} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-0.5">
                  <span className="text-xs font-bold text-gray-800">{d.from}</span>
                  <span className="text-[10px] text-gray-400">→</span>
                  <span className="text-xs font-bold text-gray-800">{d.to}</span>
                  {!d.isRead && <div className="w-1.5 h-1.5 rounded-full bg-blue-500 ml-1" />}
                </div>
                <p className="text-sm text-gray-700">{d.content}</p>
                <div className="flex items-center justify-between mt-1">
                  <span className="text-[10px] text-gray-400">{d.time}</span>
                  {!d.isRead && (
                    <button
                      onClick={() => markDMRead(d.postId, d.id)}
                      className="text-[10px] text-blue-600 font-bold hover:underline"
                    >
                      읽음 처리
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))
      )}
    </motion.div>
  );
}