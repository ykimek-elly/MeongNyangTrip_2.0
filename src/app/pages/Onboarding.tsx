import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore, type PetInfo } from '../store/useAppStore';
import {
  PawPrint,
  Zap,
  Ghost,
  AlertCircle,
  Trees,
  Building2,
  Tent,
  Check,
  ChevronRight,
  PartyPopper,
  Heart,
  MapPin,
  Sparkles,
} from 'lucide-react';
import { PetProfileForm } from '../components/PetProfileForm';
import { getAgeGroupLabel } from '../data/pet-care-helpers';

interface OnboardingProps {
  onNavigate: (page: string) => void;
}

// 취향 선택 타입
type Personality = 'ACTIVE' | 'SHY' | 'SENSITIVE';
type PreferredPlace = 'NATURE' | 'URBAN' | 'PLAYGROUND';

export function Onboarding({ onNavigate }: OnboardingProps) {
  const { username, pet, registerPet, completeOnboarding } = useAppStore();

  // 온보딩 단계: welcome → pet → preference → complete
  const [phase, setPhase] = useState<'welcome' | 'pet' | 'preference' | 'complete'>('welcome');
  const [showPetForm, setShowPetForm] = useState(false);
  const [personality, setPersonality] = useState<Personality | null>(null);
  const [preferredPlace, setPreferredPlace] = useState<PreferredPlace | null>(null);

  // 반려동물 등록 완료 핸들러
  const handlePetSubmit = (petData: PetInfo) => {
    registerPet(petData);
    setShowPetForm(false);
    setPhase('preference');
  };

  // 온보딩 완료
  const handleComplete = (destination: string) => {
    completeOnboarding();
    onNavigate(destination);
  };

  // 취향 선택 스킵 → 바로 완료
  const handleSkipPreference = () => {
    setPhase('complete');
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* 상단 진행 표시 */}
      <header className="px-6 pt-6 pb-2">
        <div className="flex gap-2">
          {['welcome', 'pet', 'preference', 'complete'].map((p, idx) => (
            <div
              key={p}
              className={`h-1.5 rounded-full flex-1 transition-all duration-500 ${
                idx <= ['welcome', 'pet', 'preference', 'complete'].indexOf(phase)
                  ? 'bg-primary'
                  : 'bg-gray-200'
              }`}
            />
          ))}
        </div>
      </header>

      <main className="flex-1 px-6 py-6 flex flex-col">
        <AnimatePresence mode="wait">
          {/* ───── 1단계: 웰컴 ───── */}
          {phase === 'welcome' && (
            <motion.div
              key="welcome"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -30 }}
              className="flex-1 flex flex-col justify-center text-center"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', damping: 12, stiffness: 200, delay: 0.2 }}
                className="w-28 h-28 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-8"
              >
                <span className="text-6xl">🎉</span>
              </motion.div>

              <h1 className="text-2xl font-bold text-gray-900 mb-3">
                {username}님, 환영합니다!
              </h1>
              <p className="text-sm text-gray-500 leading-relaxed mb-2">
                멍냥트립에 가입해 주셔서 감사해요.
              </p>
              <p className="text-sm text-gray-500 leading-relaxed mb-10">
                우리 아이를 등록하면<br />
                <span className="font-bold text-primary">AI 맞춤 산책 코스</span>와 <span className="font-bold text-primary">건강 케어 플랜</span>을<br />
                바로 시작할 수 있어요!
              </p>

              {/* 혜택 미리보기 */}
              <div className="space-y-3 mb-10 text-left">
                {[
                  { icon: Sparkles, color: 'text-amber-500', bg: 'bg-amber-50', text: 'AI가 분석하는 맞춤 산책 코스' },
                  { icon: Heart, color: 'text-pink-500', bg: 'bg-pink-50', text: '나이·크기별 건강 관리 체크리스트' },
                  { icon: MapPin, color: 'text-blue-500', bg: 'bg-blue-50', text: '반려동물 동반 가능 장소 추천' },
                ].map((item, idx) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.4 + idx * 0.15 }}
                    className="flex items-center gap-3 p-3 bg-gray-50 rounded-2xl"
                  >
                    <div className={`w-10 h-10 ${item.bg} rounded-xl flex items-center justify-center shrink-0`}>
                      <item.icon size={20} className={item.color} />
                    </div>
                    <span className="text-sm font-bold text-gray-700">{item.text}</span>
                  </motion.div>
                ))}
              </div>

              <div className="space-y-3 mt-auto">
                <button
                  onClick={() => setPhase('pet')}
                  className="w-full py-4 bg-primary text-white font-bold rounded-2xl shadow-lg hover:bg-primary/90 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
                >
                  <PawPrint size={20} />
                  반려동물 등록하기
                </button>
                <button
                  onClick={() => handleComplete('home')}
                  className="w-full py-3 text-sm text-gray-400 hover:text-gray-600 transition-colors"
                >
                  나중에 할게요
                </button>
              </div>
            </motion.div>
          )}

          {/* ───── 2단계: 반려동물 등록 ───── */}
          {phase === 'pet' && !showPetForm && (
            <motion.div
              key="pet-intro"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -30 }}
              className="flex-1 flex flex-col justify-center text-center"
            >
              <motion.div
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ type: 'spring', damping: 15 }}
                className="w-24 h-24 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-6"
              >
                <PawPrint size={48} className="text-amber-500" />
              </motion.div>

              <h2 className="text-xl font-bold text-gray-900 mb-2">우리 아이를 소개해주세요</h2>
              <p className="text-sm text-gray-500 mb-8 leading-relaxed">
                이름, 품종, 나이 등 기본 정보만 있으면<br />
                맞춤 서비스를 바로 시작할 수 있어요.
              </p>

              <div className="space-y-3 mt-auto">
                <button
                  onClick={() => setShowPetForm(true)}
                  className="w-full py-4 bg-primary text-white font-bold rounded-2xl shadow-lg hover:bg-primary/90 active:scale-[0.98] transition-all"
                >
                  등록 시작하기
                </button>
                <button
                  onClick={() => handleComplete('home')}
                  className="w-full py-3 text-sm text-gray-400 hover:text-gray-600 transition-colors"
                >
                  건너뛰기
                </button>
              </div>
            </motion.div>
          )}

          {/* ───── 3단계: 취향 선택 ───── */}
          {phase === 'preference' && (
            <motion.div
              key="preference"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -30 }}
              className="flex-1 flex flex-col"
            >
              <div className="mb-6">
                <h2 className="text-xl font-bold text-gray-900 mb-2">여행 취향을 알려주세요 ✨</h2>
                <p className="text-sm text-gray-500">
                  {pet?.name}와(과) 딱 맞는 장소를 추천해 드릴게요.
                </p>
              </div>

              {/* 반려동물 성향 */}
              <div className="mb-6">
                <label className="block text-sm font-bold text-gray-700 mb-3">우리 아이 성향은?</label>
                <div className="grid grid-cols-3 gap-3">
                  {([
                    { id: 'ACTIVE' as const, label: '활동적', icon: Zap, color: 'text-yellow-500' },
                    { id: 'SHY' as const, label: '소심함', icon: Ghost, color: 'text-purple-500' },
                    { id: 'SENSITIVE' as const, label: '예민함', icon: AlertCircle, color: 'text-blue-500' },
                  ]).map((item) => (
                    <button
                      key={item.id}
                      onClick={() => setPersonality(item.id)}
                      className={`relative flex flex-col items-center p-4 rounded-2xl border-2 transition-all ${
                        personality === item.id
                          ? 'border-primary bg-primary/5'
                          : 'border-gray-100 bg-white hover:bg-gray-50'
                      }`}
                    >
                      <div className={`p-3 rounded-full mb-2 ${personality === item.id ? 'bg-white shadow-sm' : 'bg-gray-100'}`}>
                        <item.icon className={item.color} size={24} />
                      </div>
                      <span className={`font-bold text-sm ${personality === item.id ? 'text-primary' : 'text-gray-600'}`}>
                        {item.label}
                      </span>
                      {personality === item.id && (
                        <div className="absolute top-2 right-2 text-primary">
                          <Check size={16} />
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* 선호 장소 */}
              <div className="mb-6">
                <label className="block text-sm font-bold text-gray-700 mb-3">선호하는 장소 분위기는?</label>
                <div className="space-y-3">
                  {([
                    { id: 'NATURE' as const, label: '자연/숲', desc: '풀냄새 맡으며 힐링해요', icon: Trees },
                    { id: 'URBAN' as const, label: '도심/카페', desc: '세련된 공간에서 쉬어요', icon: Building2 },
                    { id: 'PLAYGROUND' as const, label: '운동장', desc: '신나게 뛰어놀아요', icon: Tent },
                  ]).map((item) => (
                    <button
                      key={item.id}
                      onClick={() => setPreferredPlace(item.id)}
                      className={`w-full flex items-center p-4 rounded-2xl border-2 transition-all ${
                        preferredPlace === item.id
                          ? 'border-primary bg-primary/5'
                          : 'border-gray-100 bg-white hover:bg-gray-50'
                      }`}
                    >
                      <div className={`p-3 rounded-xl mr-4 ${preferredPlace === item.id ? 'bg-white text-primary shadow-sm' : 'bg-gray-100 text-gray-500'}`}>
                        <item.icon size={24} />
                      </div>
                      <div className="text-left flex-1">
                        <div className={`font-bold ${preferredPlace === item.id ? 'text-primary' : 'text-gray-800'}`}>
                          {item.label}
                        </div>
                        <div className="text-xs text-gray-400">{item.desc}</div>
                      </div>
                      {preferredPlace === item.id && (
                        <Check className="text-primary shrink-0" size={20} />
                      )}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-3 mt-auto pb-6">
                <button
                  onClick={() => setPhase('complete')}
                  disabled={!personality || !preferredPlace}
                  className={`w-full py-4 rounded-2xl font-bold transition-all active:scale-[0.98] ${
                    personality && preferredPlace
                      ? 'bg-primary text-white shadow-lg hover:bg-primary/90'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  다음
                </button>
                <button
                  onClick={handleSkipPreference}
                  className="w-full py-3 text-sm text-gray-400 hover:text-gray-600 transition-colors"
                >
                  건너뛰기
                </button>
              </div>
            </motion.div>
          )}

          {/* ───── 4단계: 완료 축하 ───── */}
          {phase === 'complete' && (
            <motion.div
              key="complete"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              className="flex-1 flex flex-col justify-center text-center"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', damping: 10, stiffness: 200, delay: 0.1 }}
                className="w-28 h-28 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6"
              >
                <span className="text-6xl">🐾</span>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
              >
                <h2 className="text-2xl font-bold text-gray-900 mb-3">
                  {pet ? `${pet.name}의 맞춤 플랜이\n준비됐어요!` : '준비 완료!'}
                </h2>

                {pet && (
                  <div className="inline-flex items-center gap-2 bg-primary/10 px-4 py-2 rounded-full mx-auto mb-4">
                    <span className="text-lg">{pet.type === '강아지' ? '🐶' : '🐱'}</span>
                    <span className="text-sm font-bold text-primary">
                      {pet.name} · {pet.breed} · {pet.age}살 ({getAgeGroupLabel(pet.age)})
                    </span>
                  </div>
                )}

                <p className="text-sm text-gray-500 leading-relaxed mb-8">
                  {pet
                    ? `${pet.name}에게 딱 맞는 산책 코스와\n건강 관리 플랜을 지금 바로 확인해보세요!`
                    : '멍냥트립의 다양한 서비스를 둘러보세요!\n마이페이지에서 언제든 반려동물을 등록할 수 있어요.'}
                </p>
              </motion.div>

              {/* 추천 장소 미리보기 (취향 선택한 경우) */}
              {personality && preferredPlace && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.5 }}
                  className="bg-gray-50 rounded-2xl p-4 mb-8 text-left"
                >
                  <h3 className="text-sm font-bold text-gray-700 mb-3 flex items-center gap-1.5">
                    <Sparkles size={16} className="text-primary" />
                    {pet?.name}에게 추천하는 장소
                  </h3>
                  <div className="space-y-2">
                    {[
                      { name: '반려견 숲속 캠핑장', tag: preferredPlace === 'NATURE' ? '자연' : preferredPlace === 'URBAN' ? '도심' : '야외' },
                      { name: '멍냥 펫 리조트', tag: '스테이' },
                      { name: '강아지 수영장 파크', tag: '플레이스' },
                    ].map((p, i) => (
                      <div key={i} className="flex items-center gap-3 p-2.5 bg-white rounded-xl">
                        <div className="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center text-primary">
                          <MapPin size={16} />
                        </div>
                        <div className="flex-1">
                          <div className="text-sm font-bold text-gray-800">{p.name}</div>
                          <div className="text-[10px] text-gray-400">{p.tag}</div>
                        </div>
                        <ChevronRight size={16} className="text-gray-300" />
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}

              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.6 }}
                className="space-y-3 mt-auto"
              >
                {pet && (
                  <button
                    onClick={() => handleComplete('senior-pet')}
                    className="w-full py-3.5 bg-amber-500 text-white font-bold rounded-2xl shadow-md hover:bg-amber-600 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
                  >
                    <Heart size={18} />
                    펫 케어 플랜 보러가기
                  </button>
                )}
                <button
                  onClick={() => handleComplete('home')}
                  className={`w-full py-3.5 rounded-2xl font-bold transition-all active:scale-[0.98] flex items-center justify-center gap-2 ${
                    pet
                      ? 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      : 'bg-primary text-white shadow-lg hover:bg-primary/90'
                  }`}
                >
                  홈으로 가기
                </button>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      {/* 반려동물 등록 폼 (풀스크린 바텀시트) */}
      <AnimatePresence>
        {showPetForm && (
          <PetProfileForm
            onClose={() => setShowPetForm(false)}
            onSubmit={handlePetSubmit}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
