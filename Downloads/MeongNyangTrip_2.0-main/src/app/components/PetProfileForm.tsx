import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, PawPrint, Check, Star } from 'lucide-react';
import type { PetInfo } from '../store/useAppStore';

interface PetProfileFormProps {
  /** 수정 모드일 때 기존 pet 정보 */
  initialData?: PetInfo | null;
  /** 기존 등록 동물이 있을 때 true → 대표 동물 설정 토글 표시 */
  hasExistingPets?: boolean;
  onSubmit: (pet: PetInfo) => void;
  onClose: () => void;
}

const PET_TYPES = [
  { id: '강아지' as const, emoji: '🐶', label: '강아지' },
  { id: '고양이' as const, emoji: '🐱', label: '고양이' },
];

const SIZES = [
  { id: 'SMALL' as const, label: '소형', desc: '~10kg' },
  { id: 'MEDIUM' as const, label: '중형', desc: '10~25kg' },
  { id: 'LARGE' as const, label: '대형', desc: '25kg~' },
];

const ACTIVITIES = [
  { id: 'LOW' as const, emoji: '🐢', label: '적음' },
  { id: 'NORMAL' as const, emoji: '🐕', label: '보통' },
  { id: 'HIGH' as const, emoji: '⚡', label: '많음' },
];

const GENDERS = [
  { id: '남아' as const, label: '남아', emoji: '♂️' },
  { id: '여아' as const, label: '여아', emoji: '♀️' },
];

// 품종 목록 (타입별)
const BREEDS: Record<string, string[]> = {
  '강아지': ['푸들', '말티즈', '치와와', '시바견', '골든리트리버', '래브라도', '포메라니안', '비숑', '웰시코기', '진돗개', '기타'],
  '고양이': ['코리안숏헤어', '페르시안', '스코티시폴드', '러시안블루', '브리티시숏헤어', '먼치킨', '랙돌', '벵갈', '기타'],
};

export function PetProfileForm({ initialData, hasExistingPets, onSubmit, onClose }: PetProfileFormProps) {
  const isEdit = !!initialData;

  const [name, setName] = useState(initialData?.name || '');
  const [type, setType] = useState<'강아지' | '고양이'>(initialData?.type || '강아지');
  const [breed, setBreed] = useState(initialData?.breed || '');
  const [gender, setGender] = useState<'남아' | '여아'>(initialData?.gender || '남아');
  const [size, setSize] = useState<'SMALL' | 'MEDIUM' | 'LARGE'>(initialData?.size || 'MEDIUM');
  const [activity, setActivity] = useState<'LOW' | 'NORMAL' | 'HIGH'>(initialData?.activity || 'NORMAL');
  const [age, setAge] = useState(initialData?.age?.toString() || '');
  const [weight, setWeight] = useState(initialData?.weight?.toString() || '');
  const [step, setStep] = useState(1); // 2단계 폼 (1: 기본 정보, 2: 상세 정보)
  const [setAsRepresentative, setSetAsRepresentative] = useState(false);

  const showRepresentativeToggle = !isEdit && !!hasExistingPets;

  const canProceed = name.trim() && type && breed;
  const canSubmit = canProceed && age;

  const handleSubmit = () => {
    if (!canSubmit) return;
    onSubmit({
      name: name.trim(),
      type,
      breed,
      gender,
      size,
      activity,
      age: parseInt(age),
      weight: weight ? parseFloat(weight) : undefined,
      isRepresentative: setAsRepresentative,
    });
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center">
      {/* 배경 오버레이 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* 폼 바텀시트 */}
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
            {step === 1 ? (
              <motion.div
                key="step1"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="space-y-5"
              >
                {/* 이름 */}
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

                {/* 종류 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">종류 *</label>
                  <div className="grid grid-cols-2 gap-2">
                    {PET_TYPES.map((t) => (
                      <button
                        key={t.id}
                        onClick={() => { setType(t.id); setBreed(''); }}
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

                {/* 품종 */}
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
                </div>

                {/* 성별 */}
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

                {/* 다음 버튼 */}
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
            ) : (
              <motion.div
                key="step2"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="space-y-5"
              >
                {/* 단계 표시 */}
                <button
                  onClick={() => setStep(1)}
                  className="text-sm text-gray-500 hover:text-primary transition-colors flex items-center gap-1"
                >
                  ← 기본 정보로 돌아가기
                </button>

                {/* 나이 */}
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

                {/* 체중 */}
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">체중 (kg) <span className="text-gray-400 font-normal">선택</span></label>
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

                {/* 크기 */}
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

                {/* 활동량 */}
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

                {/* 대표 동물 설정 토글 — 추가 등록 시에만 표시 */}
                {showRepresentativeToggle && (
                  <button
                    type="button"
                    onClick={() => setSetAsRepresentative(prev => !prev)}
                    className={`w-full flex items-center justify-between p-3.5 rounded-2xl border-2 transition-all ${
                      setAsRepresentative
                        ? 'border-primary bg-primary/5'
                        : 'border-gray-100 bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Star
                        size={16}
                        className={setAsRepresentative ? 'text-primary fill-primary' : 'text-gray-400'}
                      />
                      <span className={`text-sm font-bold ${setAsRepresentative ? 'text-primary' : 'text-gray-600'}`}>
                        대표 동물로 설정
                      </span>
                    </div>
                    <span className="text-xs text-gray-400">알림 수신 기준</span>
                  </button>
                )}

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
