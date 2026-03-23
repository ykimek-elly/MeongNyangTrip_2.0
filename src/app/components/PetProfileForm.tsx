import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, PawPrint, Check, Star, MapPin, Bell, BellOff, Zap, Ghost, AlertCircle, Trees, Building2, Tent } from 'lucide-react';
import type { PetInfo } from '../store/useAppStore';

// ── 시도 → 시군구 데이터 ──────────────────────────────────────────────
const SIDO_LIST = [
  '서울', '경기', '인천', '강원',
  '충북', '충남', '대전', '세종',
  '전북', '전남', '광주', '경북',
  '경남', '대구', '부산', '울산', '제주',
];

const SIDO_DISTRICTS: Record<string, string[]> = {
  '서울': ['강남구','강동구','강북구','강서구','관악구','광진구','구로구','금천구','노원구','도봉구','동대문구','동작구','마포구','서대문구','서초구','성동구','성북구','송파구','양천구','영등포구','용산구','은평구','종로구','중구','중랑구'],
  '경기': ['가평군','고양시','과천시','광명시','광주시','구리시','군포시','김포시','남양주시','동두천시','부천시','성남시','수원시','시흥시','안산시','안성시','안양시','양주시','양평군','여주시','연천군','오산시','용인시','의왕시','의정부시','이천시','파주시','평택시','포천시','하남시','화성시'],
  '인천': ['강화군','계양구','남동구','동구','미추홀구','부평구','서구','연수구','옹진군','중구'],
  '강원': ['강릉시','고성군','동해시','삼척시','속초시','양구군','양양군','영월군','원주시','인제군','정선군','철원군','춘천시','태백시','평창군','홍천군','화천군','횡성군'],
  '충북': ['괴산군','단양군','보은군','영동군','옥천군','음성군','제천시','증평군','진천군','청주시','충주시'],
  '충남': ['계룡시','공주시','금산군','논산시','당진시','보령시','부여군','서산시','서천군','아산시','예산군','천안시','청양군','태안군','홍성군'],
  '대전': ['대덕구','동구','서구','유성구','중구'],
  '세종': ['세종시'],
  '전북': ['고창군','군산시','김제시','남원시','무주군','부안군','순창군','완주군','익산시','임실군','장수군','전주시','정읍시','진안군'],
  '전남': ['강진군','고흥군','곡성군','광양시','구례군','나주시','담양군','목포시','무안군','보성군','순천시','신안군','여수시','영광군','영암군','완도군','장성군','장흥군','진도군','함평군','해남군','화순군'],
  '광주': ['광산구','남구','동구','북구','서구'],
  '경북': ['경산시','경주시','고령군','구미시','군위군','김천시','문경시','봉화군','상주시','성주군','안동시','영덕군','영양군','영주시','영천시','예천군','울릉군','울진군','의성군','청도군','청송군','칠곡군','포항시'],
  '경남': ['거제시','거창군','고성군','김해시','남해군','밀양시','사천시','산청군','양산시','의령군','진주시','창녕군','창원시','통영시','하동군','함안군','함양군','합천군'],
  '대구': ['남구','달서구','달성군','동구','북구','서구','수성구','중구'],
  '부산': ['강서구','금정구','기장군','남구','동구','동래구','부산진구','북구','사상구','사하구','서구','수영구','연제구','영도구','중구','해운대구'],
  '울산': ['남구','동구','북구','울주군','중구'],
  '제주': ['서귀포시','제주시'],
};

// ── 활동 반경 ────────────────────────────────────────────────────────
const RADIUS_STEPS: { value: 5 | 15 | 30; label: string; desc: string }[] = [
  { value: 5,  label: '5km',   desc: '가까운 거리' },
  { value: 15, label: '15km',  desc: '중간 거리' },
  { value: 30, label: '먼거리', desc: '넓은 범위' },
];

// ── 성향 ─────────────────────────────────────────────────────────────
const PERSONALITIES = [
  { id: '활동적', Icon: Zap,          color: 'text-amber-500',  bg: 'bg-amber-50',  activeBg: 'bg-amber-500'  },
  { id: '소심함', Icon: Ghost,         color: 'text-purple-500', bg: 'bg-purple-50', activeBg: 'bg-purple-500' },
  { id: '예민함', Icon: AlertCircle,   color: 'text-blue-500',   bg: 'bg-blue-50',   activeBg: 'bg-blue-500'   },
];

// ── 장소 분위기 ──────────────────────────────────────────────────────
const ATMOSPHERES = [
  { id: '자연/숲',  Icon: Trees,     label: '자연/숲',   desc: '풀냄새 맡으며 힐링해요',    color: 'text-green-500',  bg: 'bg-green-50'  },
  { id: '도심/카페', Icon: Building2, label: '도심/카페', desc: '세련된 공간에서 쉬어요',     color: 'text-gray-500',   bg: 'bg-gray-100'  },
  { id: '운동장',   Icon: Tent,      label: '운동장',    desc: '신나게 뛰어놀아요',          color: 'text-orange-500', bg: 'bg-orange-50' },
];


interface PetProfileFormProps {
  initialData?: PetInfo | null;
  hasExistingPets?: boolean;
  onSubmit: (pet: PetInfo) => void;
  onClose: () => void;
}

const PET_TYPES = [
  { id: '강아지' as const, emoji: '🐶', label: '강아지' },
  { id: '고양이' as const, emoji: '🐱', label: '고양이' },
];

const SIZES = [
  { id: 'SMALL'  as const, label: '소형', desc: '~10kg'   },
  { id: 'MEDIUM' as const, label: '중형', desc: '10~25kg' },
  { id: 'LARGE'  as const, label: '대형', desc: '25kg~'   },
];

const ACTIVITIES = [
  { id: 'LOW'    as const, emoji: '🐢', label: '적음' },
  { id: 'NORMAL' as const, emoji: '🐕', label: '보통' },
  { id: 'HIGH'   as const, emoji: '⚡', label: '많음' },
];

const GENDERS = [
  { id: '남아' as const, label: '남아', emoji: '♂️' },
  { id: '여아' as const, label: '여아', emoji: '♀️' },
];

const BREEDS: Record<string, string[]> = {
  '강아지': ['푸들', '말티즈', '치와와', '시바견', '골든리트리버', '래브라도', '포메라니안', '비숑', '웰시코기', '진돗개', '기타'],
  '고양이': ['코리안숏헤어', '페르시안', '스코티시폴드', '러시안블루', '브리티시숏헤어', '먼치킨', '랙돌', '벵갈', '기타'],
};

// 저장된 region 문자열("서울 강남구")을 시도/시군구로 분리
function parseRegion(region?: string) {
  if (!region) return { sido: '', district: '' };
  const parts = region.trim().split(' ');
  return { sido: parts[0] || '', district: parts[1] || '' };
}

export function PetProfileForm({ initialData, hasExistingPets, onSubmit, onClose }: PetProfileFormProps) {
  const isEdit = !!initialData;
  const initRegion = parseRegion(initialData?.region);

  const [name, setName]       = useState(initialData?.name || '');
  const [type, setType]       = useState<'강아지' | '고양이'>(initialData?.type || '강아지');
  // 품종 목록에 없으면 기타 선택 + customBreed에 실제 값
  const initBreed = initialData?.breed
    ? (BREEDS[initialData.type || '강아지']?.includes(initialData.breed) ? initialData.breed : '기타')
    : '';
  const initCustomBreed = initialData?.breed
    ? (BREEDS[initialData.type || '강아지']?.includes(initialData.breed) ? '' : initialData.breed)
    : '';
  const [breed, setBreed]     = useState(initBreed);
  const [customBreed, setCustomBreed] = useState(initCustomBreed);
  const [gender, setGender]   = useState<'남아' | '여아'>(initialData?.gender || '남아');
  const [size, setSize]       = useState<'SMALL' | 'MEDIUM' | 'LARGE'>(initialData?.size || 'MEDIUM');
  const [activity, setActivity] = useState<'LOW' | 'NORMAL' | 'HIGH'>(initialData?.activity || 'NORMAL');
  const [age, setAge]         = useState(initialData?.age?.toString() || '');
  const [weight, setWeight]   = useState(initialData?.weight?.toString() || '');
  const [step, setStep]       = useState(1);
  const [setAsRepresentative, setSetAsRepresentative] = useState(false);

  // step 3 state
  const [sido, setSido]                   = useState(initRegion.sido);
  const [district, setDistrict]           = useState(initRegion.district);
  const [activityRadius, setActivityRadius] = useState<5 | 15 | 30>(initialData?.activityRadius ?? 15);
  const [personality, setPersonality]     = useState(initialData?.personality || '');
  const [atmosphere, setAtmosphere]       = useState(initialData?.preferredPlace || '');
  const [notifyEnabled, setNotifyEnabled] = useState(initialData?.notifyEnabled ?? true);

  const showRepresentativeToggle = !isEdit && !!hasExistingPets;

  const canProceed      = name.trim() && type && breed && (breed !== '기타' || customBreed.trim());
  const canStep2Submit  = canProceed && !!age;
  const canSubmit       = canStep2Submit && !!sido;

  const handleSidoChange = (newSido: string) => {
    setSido(newSido);
    setDistrict(''); // 시도 변경 시 시군구 초기화
  };

  const handleSubmit = () => {
    if (!canSubmit) return;
    onSubmit({
      name: name.trim(),
      type,
      breed: breed === '기타' ? customBreed.trim() : breed,
      gender,
      size,
      activity,
      age: parseInt(age),
      weight: weight ? parseFloat(weight) : undefined,
      isRepresentative: setAsRepresentative,
      region: sido + (district ? ` ${district}` : ''),
      activityRadius,
      personality: personality || undefined,
      preferredPlace: atmosphere || undefined,
      notifyEnabled,
    });
  };

  // 활동 반경 바: 누적 채움 (선택된 인덱스 이하는 모두 primary 색)
  const selectedRadiusIdx = RADIUS_STEPS.findIndex(s => s.value === activityRadius);

  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />

      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
        className="relative z-10 w-full max-w-[600px] bg-white rounded-t-3xl sm:rounded-3xl max-h-[90vh] overflow-y-auto"
      >
        {/* 헤더 */}
        <div className="sticky top-0 bg-white z-10 flex items-center justify-between px-5 pt-5 pb-3 border-b border-gray-100">
          <h2 className="text-lg font-bold text-gray-900">
            {isEdit ? '반려동물 정보 수정' : '반려동물 등록'}
          </h2>
          <button onClick={onClose} className="p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100">
            <X size={20} />
          </button>
        </div>

        <div className="px-5 py-5 space-y-5">
          <AnimatePresence mode="wait">

            {/* ── STEP 1: 기본 정보 ── */}
            {step === 1 ? (
              <motion.div
                key="step1"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="space-y-5"
              >
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">이름 *</label>
                  <input
                    type="text"
                    placeholder="반려동물 이름"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-colors text-sm"
                    maxLength={20}
                  />
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">종류 *</label>
                  <div className="grid grid-cols-2 gap-2">
                    {PET_TYPES.map((t) => (
                      <button
                        key={t.id}
                        onClick={() => { setType(t.id); setBreed(''); setCustomBreed(''); }}
                        className={`flex items-center justify-center gap-2 p-3.5 rounded-2xl border-2 transition-all ${
                          type === t.id
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-gray-100 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        <span className="text-xl">{t.emoji}</span>
                        <span className="font-bold text-sm">{t.label}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">품종 *</label>
                  <div className="flex flex-wrap gap-2">
                    {(BREEDS[type] || []).map((b) => (
                      <button
                        key={b}
                        onClick={() => setBreed(b)}
                        className={`px-3.5 py-2 rounded-full text-sm transition-all ${
                          breed === b
                            ? 'bg-primary text-white font-bold shadow-md'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        }`}
                      >
                        {b}
                      </button>
                    ))}
                  </div>
                  {breed === '기타' && (
                    <motion.div
                      initial={{ opacity: 0, y: -6 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="mt-2"
                    >
                      <input
                        type="text"
                        value={customBreed}
                        onChange={e => setCustomBreed(e.target.value)}
                        placeholder="품종을 직접 입력해주세요"
                        className="w-full px-4 py-3 rounded-2xl border-2 border-primary/40 bg-primary/5 text-sm text-gray-800 placeholder-gray-400 outline-none focus:border-primary transition-colors"
                        autoFocus
                      />
                    </motion.div>
                  )}
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">성별</label>
                  <div className="grid grid-cols-2 gap-2">
                    {GENDERS.map((g) => (
                      <button
                        key={g.id}
                        onClick={() => setGender(g.id)}
                        className={`flex items-center justify-center gap-1.5 p-3 rounded-2xl border-2 transition-all ${
                          gender === g.id
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-gray-100 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        <span>{g.emoji}</span>
                        <span className="font-bold text-sm">{g.label}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  onClick={() => canProceed && setStep(2)}
                  disabled={!canProceed}
                  className={`w-full py-3.5 rounded-2xl font-bold transition-all ${
                    canProceed
                      ? 'bg-primary text-white shadow-md active:scale-[0.98]'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  다음
                </button>
              </motion.div>

            ) : step === 2 ? (

              /* ── STEP 2: 상세 정보 ── */
              <motion.div
                key="step2"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="space-y-5"
              >
                <button
                  onClick={() => setStep(1)}
                  className="text-sm text-gray-500 hover:text-primary transition-colors flex items-center gap-1"
                >
                  ← 기본 정보로 돌아가기
                </button>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">나이 (살) *</label>
                  <input
                    type="number"
                    placeholder="예: 3"
                    value={age}
                    onChange={(e) => setAge(e.target.value)}
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-colors text-sm"
                    min={0}
                    max={30}
                  />
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">
                    체중 (kg) <span className="text-gray-400 font-normal">선택</span>
                  </label>
                  <input
                    type="number"
                    placeholder="예: 5.5"
                    value={weight}
                    onChange={(e) => setWeight(e.target.value)}
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-colors text-sm"
                    min={0}
                    max={100}
                    step={0.1}
                  />
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">크기</label>
                  <div className="grid grid-cols-3 gap-2">
                    {SIZES.map((s) => (
                      <button
                        key={s.id}
                        onClick={() => setSize(s.id)}
                        className={`p-3 rounded-2xl border-2 transition-all text-center ${
                          size === s.id
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-gray-100 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        <div className="font-bold text-sm">{s.label}</div>
                        <div className="text-xs mt-0.5 opacity-60">{s.desc}</div>
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">활동량</label>
                  <div className="grid grid-cols-3 gap-2">
                    {ACTIVITIES.map((a) => (
                      <button
                        key={a.id}
                        onClick={() => setActivity(a.id)}
                        className={`p-3 rounded-2xl border-2 transition-all text-center ${
                          activity === a.id
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-gray-100 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        <div className="text-xl mb-1">{a.emoji}</div>
                        <div className="font-bold text-sm">{a.label}</div>
                      </button>
                    ))}
                  </div>
                </div>

                {showRepresentativeToggle && (
                  <button
                    type="button"
                    onClick={() => setSetAsRepresentative(prev => !prev)}
                    className={`w-full flex items-center justify-between p-3.5 rounded-2xl border-2 transition-all ${
                      setAsRepresentative ? 'border-primary bg-primary/5' : 'border-gray-100 bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Star size={16} className={setAsRepresentative ? 'text-primary fill-primary' : 'text-gray-400'} />
                      <span className={`text-sm font-bold ${setAsRepresentative ? 'text-primary' : 'text-gray-600'}`}>
                        대표 동물로 설정
                      </span>
                    </div>
                    <span className="text-xs text-gray-400">알림 수신 기준</span>
                  </button>
                )}

                <button
                  onClick={() => canStep2Submit && setStep(3)}
                  disabled={!canStep2Submit}
                  className={`w-full py-3.5 rounded-2xl font-bold transition-all ${
                    canStep2Submit
                      ? 'bg-primary text-white shadow-md active:scale-[0.98]'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  다음
                </button>
              </motion.div>

            ) : (

              /* ── STEP 3: 지역 · 성향 · 분위기 · 카테고리 · 알림 ── */
              <motion.div
                key="step3"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="space-y-6"
              >
                <button
                  onClick={() => setStep(2)}
                  className="text-sm text-gray-500 hover:text-primary transition-colors flex items-center gap-1"
                >
                  ← 상세 정보로 돌아가기
                </button>

                {/* ① 활동 지역 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2.5 flex items-center gap-1.5">
                    <MapPin size={14} className="text-primary" /> 활동 지역 *
                  </label>

                  {/* 좌우 2단 목록 */}
                  <div className="flex gap-2 h-44 rounded-2xl overflow-hidden border-2 border-gray-100">
                    {/* 시·도 목록 */}
                    <ul className="w-1/3 overflow-y-auto border-r border-gray-100 bg-gray-50">
                      {SIDO_LIST.map(s => (
                        <li key={s}>
                          <button
                            type="button"
                            onClick={() => handleSidoChange(s)}
                            className={`w-full text-left px-3 py-2.5 text-sm transition-colors ${
                              sido === s
                                ? 'bg-primary text-white font-bold'
                                : 'text-gray-600 hover:bg-gray-100 active:bg-gray-200'
                            }`}
                          >
                            {s}
                          </button>
                        </li>
                      ))}
                    </ul>

                    {/* 시·군·구 목록 */}
                    <ul className="flex-1 overflow-y-auto bg-white">
                      {!sido ? (
                        <li className="flex items-center justify-center h-full text-xs text-gray-300">
                          시·도를 먼저 선택하세요
                        </li>
                      ) : (
                        (SIDO_DISTRICTS[sido] || []).map(d => (
                          <li key={d}>
                            <button
                              type="button"
                              onClick={() => setDistrict(prev => prev === d ? '' : d)}
                              className={`w-full text-left px-3 py-2.5 text-sm transition-colors flex items-center justify-between ${
                                district === d
                                  ? 'text-primary font-bold bg-primary/5'
                                  : 'text-gray-600 hover:bg-gray-50 active:bg-gray-100'
                              }`}
                            >
                              {d}
                              {district === d && <Check size={14} className="text-primary shrink-0" />}
                            </button>
                          </li>
                        ))
                      )}
                    </ul>
                  </div>

                  {sido && (
                    <p className="text-xs text-primary font-medium mt-1.5 ml-1">
                      📍 {sido}{district ? ` ${district}` : ''}
                    </p>
                  )}
                </div>

                {/* ② 활동 반경 — 누적 세그먼트 바 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-1 block">활동 반경</label>
                  <p className="text-xs text-gray-400 mb-2.5">등록 지역 기준으로 장소를 추천해드려요</p>
                  <div className="flex rounded-2xl overflow-hidden border-2 border-gray-100 h-14">
                    {RADIUS_STEPS.map((rs, idx) => {
                      const filled = idx <= selectedRadiusIdx;
                      return (
                        <button
                          key={rs.value}
                          onClick={() => setActivityRadius(rs.value)}
                          className={`flex-1 flex flex-col items-center justify-center gap-0.5 transition-all active:opacity-80 ${
                            filled ? 'bg-primary' : 'bg-white'
                          } ${idx > 0 ? 'border-l-2 border-gray-100' : ''}`}
                        >
                          <span className={`text-xs font-bold ${filled ? 'text-white' : 'text-gray-600'}`}>
                            {rs.label}
                          </span>
                          <span className={`text-[9px] ${filled ? 'text-white/80' : 'text-gray-400'}`}>
                            {rs.desc}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                  {/* 진행 힌트 */}
                  <div className="flex justify-between mt-1.5 px-1">
                    <span className="text-[9px] text-gray-400">집 근처</span>
                    <span className="text-[9px] text-gray-400">광역 탐색</span>
                  </div>
                </div>

                {/* ③ 우리 아이 성향 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2.5 block">
                    우리 아이 성향은? <span className="text-xs font-normal text-gray-400">선택</span>
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {PERSONALITIES.map(({ id, Icon, color, bg, activeBg }) => {
                      const selected = personality === id;
                      return (
                        <button
                          key={id}
                          onClick={() => setPersonality(selected ? '' : id)}
                          className={`flex flex-col items-center gap-2 p-3.5 rounded-2xl border-2 transition-all ${
                            selected ? 'border-primary bg-primary/5' : 'border-gray-100 bg-white hover:bg-gray-50'
                          }`}
                        >
                          <div className={`w-10 h-10 rounded-full flex items-center justify-center ${selected ? activeBg : bg}`}>
                            <Icon size={20} className={selected ? 'text-white' : color} />
                          </div>
                          <span className={`text-xs font-bold ${selected ? 'text-primary' : 'text-gray-700'}`}>{id}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* ④ 선호하는 장소 분위기 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2.5 block">
                    선호하는 장소 분위기는? <span className="text-xs font-normal text-gray-400">선택</span>
                  </label>
                  <div className="space-y-2">
                    {ATMOSPHERES.map(({ id, Icon, label, desc, color, bg }) => {
                      const selected = atmosphere === id;
                      return (
                        <button
                          key={id}
                          onClick={() => setAtmosphere(selected ? '' : id)}
                          className={`w-full flex items-center p-3.5 rounded-2xl border-2 transition-all ${
                            selected ? 'border-primary bg-primary/5' : 'border-gray-100 bg-white hover:bg-gray-50'
                          }`}
                        >
                          <div className={`w-10 h-10 rounded-xl flex items-center justify-center mr-3 shrink-0 ${selected ? 'bg-primary/15' : bg}`}>
                            <Icon size={20} className={selected ? 'text-primary' : color} />
                          </div>
                          <div className="text-left flex-1">
                            <div className={`text-sm font-bold ${selected ? 'text-primary' : 'text-gray-800'}`}>{label}</div>
                            <div className="text-[11px] text-gray-400">{desc}</div>
                          </div>
                          {selected && <Check size={16} className="text-primary shrink-0" />}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* ⑤ 알림 수신 */}
                <div
                  onClick={() => setNotifyEnabled(prev => !prev)}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border-2 cursor-pointer transition-all ${
                    notifyEnabled ? 'border-primary bg-primary/5' : 'border-gray-100 bg-gray-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${notifyEnabled ? 'bg-primary text-white' : 'bg-gray-200 text-gray-400'}`}>
                      {notifyEnabled ? <Bell size={18} /> : <BellOff size={18} />}
                    </div>
                    <div>
                      <div className={`text-sm font-bold ${notifyEnabled ? 'text-primary' : 'text-gray-500'}`}>
                        맞춤 알림 {notifyEnabled ? '수신 동의' : '수신 안 함'}
                      </div>
                      <div className="text-[11px] text-gray-400">날씨·장소·이벤트 알림</div>
                    </div>
                  </div>
                  <div className={`w-11 h-6 rounded-full transition-colors relative ${notifyEnabled ? 'bg-primary' : 'bg-gray-300'}`}>
                    <div className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all ${notifyEnabled ? 'left-5' : 'left-0.5'}`} />
                  </div>
                </div>

                {/* 등록/수정 버튼 */}
                <button
                  onClick={handleSubmit}
                  disabled={!canSubmit}
                  className={`w-full py-3.5 rounded-2xl font-bold flex items-center justify-center gap-2 transition-all ${
                    canSubmit
                      ? 'bg-primary text-white shadow-md active:scale-[0.98]'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  <Check size={18} />
                  {isEdit ? '수정 완료' : '등록 완료'}
                </button>
              </motion.div>
            )}

          </AnimatePresence>
        </div>
      </motion.div>
    </div>
  );
}
