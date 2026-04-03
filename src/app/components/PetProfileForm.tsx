import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, PawPrint, Check, Star, Bell, BellOff, Zap, Ghost, AlertCircle, Trees, Building2, Tent } from 'lucide-react';
import type { PetInfo } from '../store/useAppStore';

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

export function PetProfileForm({ initialData, hasExistingPets, onSubmit, onClose }: PetProfileFormProps) {
  const isEdit = !!initialData;

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
  const [personality, setPersonality]     = useState(initialData?.personality || '');
  const [atmospheres, setAtmospheres]     = useState<string[]>(
    initialData?.preferredPlace ? initialData.preferredPlace.split(',').map(s => s.trim()).filter(Boolean) : []
  );
  const [notifyEnabled, setNotifyEnabled] = useState(initialData?.notifyEnabled ?? true);

  const showRepresentativeToggle = !isEdit && !!hasExistingPets;

  const canProceed      = name.trim() && type && breed && (breed !== '기타' || customBreed.trim());
  const canStep2Submit  = canProceed && !!age;
  const canSubmit       = canStep2Submit;

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
      personality: personality || undefined,
      preferredPlace: atmospheres.length > 0 ? atmospheres.join(',') : undefined,
      notifyEnabled,
    });
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-end sm:items-center justify-center">
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
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-spring text-sm"
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
                        className={`flex items-center justify-center gap-2 p-3.5 rounded-2xl border-2 transition-spring ${
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
                        className={`px-3.5 py-2 rounded-full text-sm transition-spring ${
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
                        className="w-full px-4 py-3 rounded-2xl border-2 border-primary/40 bg-primary/5 text-sm text-gray-800 placeholder-gray-400 outline-none focus:border-primary transition-spring"
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
                        className={`flex items-center justify-center gap-1.5 p-3 rounded-2xl border-2 transition-spring ${
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
                  className={`w-full py-3.5 rounded-2xl font-bold transition-spring ${
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
                  className="text-sm text-gray-500 hover:text-primary transition-spring flex items-center gap-1"
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
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-spring text-sm"
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
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl outline-none focus:border-primary transition-spring text-sm"
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
                        className={`p-3 rounded-2xl border-2 transition-spring text-center ${
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
                        className={`p-3 rounded-2xl border-2 transition-spring text-center ${
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
                    className={`w-full flex items-center justify-between p-3.5 rounded-2xl border-2 transition-spring ${
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
                  className={`w-full py-3.5 rounded-2xl font-bold transition-spring ${
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
                  className="text-sm text-gray-500 hover:text-primary transition-spring flex items-center gap-1"
                >
                  ← 상세 정보로 돌아가기
                </button>

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
                          className={`flex flex-col items-center gap-2 p-3.5 rounded-2xl border-2 transition-spring ${
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
                  <label className="text-sm font-bold text-gray-700 mb-1 block">
                    선호하는 장소 분위기는? <span className="text-xs font-normal text-gray-400">중복선택가능</span>
                  </label>
                  <div className="space-y-2">
                    {ATMOSPHERES.map(({ id, Icon, label, desc, color, bg }) => {
                      const selected = atmospheres.includes(id);
                      return (
                        <button
                          key={id}
                          onClick={() =>
                            setAtmospheres(prev =>
                              prev.includes(id) ? prev.filter(a => a !== id) : [...prev, id]
                            )
                          }
                          className={`w-full flex items-center p-3.5 rounded-2xl border-2 transition-spring ${
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
                  className={`flex items-center justify-between p-3.5 rounded-2xl border-2 cursor-pointer transition-spring ${
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
                  <div className={`w-11 h-6 rounded-full transition-spring relative ${notifyEnabled ? 'bg-primary' : 'bg-gray-300'}`}>
                    <div className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-spring ${notifyEnabled ? 'left-5' : 'left-0.5'}`} />
                  </div>
                </div>

                {/* 등록/수정 버튼 */}
                <button
                  onClick={handleSubmit}
                  disabled={!canSubmit}
                  className={`w-full py-3.5 rounded-2xl font-bold flex items-center justify-center gap-2 transition-spring ${
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
