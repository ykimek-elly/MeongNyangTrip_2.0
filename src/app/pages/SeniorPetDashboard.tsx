import React, { useState, useMemo } from 'react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { 
  generateChecklist, 
  generateHealthMetrics, 
  generateMedications, 
  generateCareTips,
  getAgeGroupLabel,
  getCheckupCycleLabel,
  type ChecklistItem
} from '../data/pet-care-helpers';
import { 
  ArrowLeft, 
  Heart,
  Pill,
  Calendar,
  Bell,
  TrendingUp,
  TrendingDown,
  Minus,
  Activity,
  Stethoscope,
  CheckCircle2,
  Circle,
  Clock,
  MapPin,
  Plus,
  ChevronRight,
  AlertCircle,
  LogIn,
  PawPrint,
  Info
} from 'lucide-react';

interface SeniorPetDashboardProps {
  onNavigate: (page: string, params?: any) => void;
}

const UPCOMING_SCHEDULE = [
  { id: 1, title: "정기 건강검진", date: "2026-03-15", location: "서울동물병원", type: "checkup" },
  { id: 2, title: "예방접종", date: "2026-04-01", location: "반려동물클리닉", type: "vaccine" },
  { id: 3, title: "치과 검진", date: "2026-03-20", location: "펫치과", type: "dental" },
];

const SENIOR_PLACES = [
  { 
    id: 1, 
    name: "노견 전용 물놀이터", 
    description: "얕은 수심, 미끄럼 방지 바닥",
    distance: "1.2km",
    rating: 4.8,
    img: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=400&q=80"
  },
  { 
    id: 2, 
    name: "펫 케어센터", 
    description: "물리치료, 재활 프로그램",
    distance: "850m",
    rating: 4.9,
    img: "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400&q=80"
  },
];

export function SeniorPetDashboard({ onNavigate }: SeniorPetDashboardProps) {
  const { isLoggedIn, pet } = useAppStore();

  // pet 기반 동적 데이터 생성 (메모이제이션)
  const initialChecklist = useMemo(() => pet ? generateChecklist(pet) : [], [pet]);
  const healthMetrics = useMemo(() => pet ? generateHealthMetrics(pet) : [], [pet]);
  const medications = useMemo(() => pet ? generateMedications(pet) : [], [pet]);
  const careTips = useMemo(() => pet ? generateCareTips(pet) : [], [pet]);

  const [checklist, setChecklist] = useState<ChecklistItem[]>(initialChecklist);

  // pet 변경 시 체크리스트 재생성
  React.useEffect(() => {
    if (pet) {
      setChecklist(generateChecklist(pet));
    }
  }, [pet]);

  const toggleTask = (taskId: number) => {
    setChecklist(prev => 
      prev.map(task => 
        task.id === taskId ? { ...task, completed: !task.completed } : task
      )
    );
  };

  const completedCount = checklist.filter(t => t.completed).length;
  const progress = checklist.length > 0 ? (completedCount / checklist.length) * 100 : 0;

  // 반려동물 표시 정보 생성
  const petDisplay = pet ? {
    emoji: pet.type === '강아지' ? '🐶' : '🐱',
    name: pet.name,
    detail: `${pet.breed} · ${pet.age}살 · ${pet.gender}`,
    sizeLabel: pet.size === 'SMALL' ? '소형' : pet.size === 'MEDIUM' ? '중형' : '대형',
    ageGroupLabel: getAgeGroupLabel(pet.age),
    checkupLabel: getCheckupCycleLabel(pet.age),
  } : null;

  // 트렌드 아이콘 선택 헬퍼
  const TrendIcon = ({ trend }: { trend: string }) => {
    if (trend === 'up') return <TrendingUp size={12} />;
    if (trend === 'down') return <TrendingDown size={12} />;
    return <Minus size={12} />;
  };

  // 비로그인 또는 반려동물 미등록 시 안내 화면
  if (!isLoggedIn || !pet) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-amber-50 to-white pb-24">
        <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
          <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full">
            <ArrowLeft size={22} />
          </button>
          <div className="flex items-center gap-2 ml-2">
            <Heart className="text-amber-500 fill-amber-100" size={20} />
            <h1 className="font-bold text-lg">펫 케어 시스템</h1>
          </div>
        </header>

        <div className="flex flex-col items-center justify-center px-6 pt-20 text-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="w-24 h-24 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <PawPrint size={48} className="text-amber-500" />
            </div>

            <h2 className="text-xl font-bold text-gray-800 mb-2">
              {!isLoggedIn ? '로그인이 필요합니다' : '반려동물을 등록해 주세요'}
            </h2>
            <p className="text-sm text-gray-500 mb-8 leading-relaxed whitespace-pre-line">
              {!isLoggedIn 
                ? '펫 케어 시스템은 회원 전용 서비스입니다.\n로그인 후 우리 아이 맞춤 건강 관리를 시작하세요!'
                : '마이페이지에서 반려동물 정보를 등록하면\n나이·크기 맞춤 건강 관리 플랜을 제공해 드립니다.'}
            </p>

            <button
              onClick={() => onNavigate(!isLoggedIn ? 'login' : 'mypage')}
              className="flex items-center gap-2 mx-auto bg-amber-500 text-white px-8 py-3.5 rounded-2xl font-bold shadow-lg hover:bg-amber-600 active:scale-[0.98] transition-all"
            >
              {!isLoggedIn ? <LogIn size={18} /> : <Plus size={18} />}
              {!isLoggedIn ? '로그인하기' : '반려동물 등록하러 가기'}
            </button>

            {!isLoggedIn && (
              <button
                onClick={() => onNavigate('signup')}
                className="mt-3 text-sm text-gray-400 hover:text-gray-600 transition-colors"
              >
                아직 회원이 아니신가요? <span className="font-bold text-amber-500">회원가입</span>
              </button>
            )}
          </motion.div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-amber-50 to-white pb-24">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={22} />
        </button>
        <div className="flex items-center gap-2 ml-2">
          <Heart className="text-amber-500 fill-amber-100" size={20} />
          <h1 className="font-bold text-lg">펫 케어 시스템</h1>
        </div>
      </header>

      <div className="px-5 py-6 space-y-5">
        {/* 반려동물 프로필 카드 — 스토어 데이터 연동 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-br from-amber-400 to-orange-400 rounded-3xl p-6 text-white shadow-lg"
        >
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="text-sm opacity-90 mb-1">우리 가족</div>
              <h2 className="text-2xl font-bold">{petDisplay!.name}</h2>
              <div className="text-sm opacity-90 mt-1">{petDisplay!.detail}</div>
              <div className="flex items-center gap-2 mt-2 flex-wrap">
                <span className="bg-white/20 text-xs px-2.5 py-0.5 rounded-full">{petDisplay!.sizeLabel}</span>
                <span className="bg-white/20 text-xs px-2.5 py-0.5 rounded-full">{petDisplay!.ageGroupLabel}</span>
                {pet.weight && (
                  <span className="bg-white/20 text-xs px-2.5 py-0.5 rounded-full">{pet.weight}kg</span>
                )}
              </div>
            </div>
            <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
              <span className="text-3xl">{petDisplay!.emoji}</span>
            </div>
          </div>
          
          <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3">
            <div className="text-xs opacity-90 mb-1">오늘의 건강 관리</div>
            <div className="flex items-center justify-between">
              <div className="text-2xl font-bold">{completedCount}/{checklist.length}</div>
              <div className="text-sm font-bold">{Math.round(progress)}% 완료</div>
            </div>
            <div className="w-full bg-white/30 rounded-full h-2 mt-2 overflow-hidden">
              <div 
                className="bg-white h-full rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        </motion.div>

        {/* 나이 기반 안내 배너 */}
        <div className="bg-blue-50 border border-blue-100 rounded-2xl p-4 flex items-start gap-3">
          <Info size={18} className="text-blue-500 shrink-0 mt-0.5" />
          <div>
            <h4 className="text-sm font-bold text-blue-800">{petDisplay!.name}의 {petDisplay!.ageGroupLabel} 맞춤 플랜</h4>
            <p className="text-xs text-blue-700 mt-1 leading-relaxed">
              {pet.age}살 {petDisplay!.sizeLabel}견 기준으로 체크리스트와 건강 지표가 자동 생성되었습니다. 
              권장 검진 주기: <span className="font-bold">{petDisplay!.checkupLabel}</span>
            </p>
          </div>
        </div>

        {/* 건강 지표 — 동적 생성 */}
        <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
          <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2">
            <Activity className="text-amber-500" size={20} />
            건강 지표
          </h3>
          <div className="grid grid-cols-2 gap-3">
            {healthMetrics.map((metric, idx) => (
              <div key={idx} className="bg-gray-50 rounded-xl p-3">
                <div className="text-xs text-gray-500 mb-1">{metric.label}</div>
                <div className="font-bold text-gray-900 mb-1">{metric.value}</div>
                <div className={`text-xs flex items-center gap-1 ${
                  metric.status === 'good' ? 'text-green-600' : 'text-amber-600'
                }`}>
                  <TrendIcon trend={metric.trend} />
                  {metric.change}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 일일 체크리스트 — 동적 생성 */}
        <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-gray-800 flex items-center gap-2">
              <CheckCircle2 className="text-amber-500" size={20} />
              오늘의 체크리스트
            </h3>
            <span className="text-xs text-gray-400">{petDisplay!.ageGroupLabel} 맞춤</span>
          </div>
          <div className="space-y-2">
            {checklist.map((task) => (
              <div
                key={task.id}
                onClick={() => toggleTask(task.id)}
                className={`flex items-center gap-3 p-3 rounded-xl transition-all cursor-pointer ${
                  task.completed 
                    ? 'bg-green-50 border border-green-100' 
                    : 'bg-gray-50 border border-gray-100 hover:bg-gray-100'
                }`}
              >
                <div className={`flex-shrink-0 ${task.completed ? 'text-green-500' : 'text-gray-300'}`}>
                  {task.completed ? <CheckCircle2 size={24} /> : <Circle size={24} />}
                </div>
                <div className="flex-1">
                  <div className={`font-bold text-sm ${task.completed ? 'text-green-700 line-through' : 'text-gray-900'}`}>
                    {task.task}
                  </div>
                  <div className="text-xs text-gray-500 mt-0.5 flex items-center gap-1">
                    <Clock size={10} />
                    {task.time}
                  </div>
                </div>
                <div className={`text-xs px-2 py-1 rounded-full ${
                  task.type === 'medicine' ? 'bg-purple-100 text-purple-700' :
                  task.type === 'care' ? 'bg-blue-100 text-blue-700' :
                  'bg-amber-100 text-amber-700'
                }`}>
                  {task.type === 'medicine' ? '💊 약' : task.type === 'care' ? '🩹 케어' : '💧 건강'}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 복약 알림 — 동적 생성 */}
        <div className="bg-purple-50 rounded-3xl p-5 border border-purple-100">
          <h3 className="font-bold text-purple-900 mb-3 flex items-center gap-2">
            <Pill className="text-purple-600" size={20} />
            복약 알림
            <span className="text-xs font-normal text-purple-400 ml-auto">{medications.length}개</span>
          </h3>
          <div className="space-y-2">
            {medications.map((med, idx) => (
              <div key={idx} className="bg-white rounded-xl p-3 flex items-center justify-between">
                <div>
                  <div className="font-bold text-sm text-gray-900">{med.name}</div>
                  <div className="text-xs text-gray-500 mt-0.5">{med.schedule} · {med.dose}</div>
                </div>
                <Bell className="text-purple-600" size={18} />
              </div>
            ))}
          </div>
        </div>

        {/* 예정된 일정 */}
        <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
          <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
            <Calendar className="text-amber-500" size={20} />
            예정된 일정
          </h3>
          <div className="space-y-2">
            {UPCOMING_SCHEDULE.map((schedule) => (
              <div
                key={schedule.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    schedule.type === 'checkup' ? 'bg-blue-100' :
                    schedule.type === 'vaccine' ? 'bg-green-100' :
                    'bg-purple-100'
                  }`}>
                    <Stethoscope className={
                      schedule.type === 'checkup' ? 'text-blue-600' :
                      schedule.type === 'vaccine' ? 'text-green-600' :
                      'text-purple-600'
                    } size={18} />
                  </div>
                  <div>
                    <div className="font-bold text-sm text-gray-900">{schedule.title}</div>
                    <div className="text-xs text-gray-500 mt-0.5 flex items-center gap-1">
                      <Calendar size={10} />
                      {schedule.date}
                      <span className="mx-1">·</span>
                      <MapPin size={10} />
                      {schedule.location}
                    </div>
                  </div>
                </div>
                <ChevronRight className="text-gray-400" size={18} />
              </div>
            ))}
          </div>
        </div>

        {/* 노인 친화적 장소 */}
        <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-gray-800 flex items-center gap-2">
              <MapPin className="text-amber-500" size={20} />
              펫 케어 추천 장소
            </h3>
            <button 
              onClick={() => onNavigate('list')}
              className="text-primary text-sm font-bold"
            >
              더보기
            </button>
          </div>
          <div className="space-y-3">
            {SENIOR_PLACES.map((place) => (
              <div
                key={place.id}
                onClick={() => onNavigate('detail', { id: place.id })}
                className="flex gap-3 cursor-pointer hover:bg-gray-50 p-2 rounded-xl transition-colors"
              >
                <img
                  src={place.img}
                  alt={place.name}
                  className="w-20 h-20 rounded-xl object-cover bg-gray-100"
                />
                <div className="flex-1">
                  <div className="font-bold text-sm text-gray-900">{place.name}</div>
                  <div className="text-xs text-gray-500 mt-1 leading-relaxed">{place.description}</div>
                  <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
                    <span className="text-amber-500">⭐ {place.rating}</span>
                    <span>·</span>
                    <span>{place.distance}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 펫 케어 관리 팁 — 동적 생성 */}
        <div className="bg-amber-50 rounded-3xl p-5 border border-amber-100">
          <h3 className="font-bold text-amber-900 mb-3 flex items-center gap-2">
            <AlertCircle className="text-amber-600" size={20} />
            {petDisplay!.name}의 {petDisplay!.ageGroupLabel} 케어 팁
          </h3>
          <div className="space-y-2 text-sm text-amber-900">
            {careTips.map((tip, idx) => (
              <div key={idx} className="flex items-start gap-2">
                <span className="text-amber-600">•</span>
                <span>{tip.text}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}