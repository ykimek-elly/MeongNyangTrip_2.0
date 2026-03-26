import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore, type PetInfo } from '../store/useAppStore';
import { PawPrint, Heart, MapPin, Sparkles, Bell, Dog, Cat } from 'lucide-react';
import { PetProfileForm } from '../components/PetProfileForm';

interface OnboardingProps {
  onNavigate: (page: string) => void;
}

// 온보딩 단계: welcome → complete
const PHASES = ['welcome', 'complete'] as const;
type Phase = typeof PHASES[number];

export function Onboarding({ onNavigate }: OnboardingProps) {
  const { username, pets, addPet, completeOnboarding } = useAppStore();
  const pet = pets.find(p => p.isRepresentative) ?? pets[0] ?? null;

  const [phase, setPhase] = useState<Phase>('welcome');
  const [showPetForm, setShowPetForm] = useState(false);

  const handlePetSubmit = (petData: PetInfo) => {
    addPet(petData);
    setShowPetForm(false);
    setPhase('complete');
  };

  const handleComplete = (destination: string) => {
    completeOnboarding();
    onNavigate(destination);
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* 상단 진행 표시 */}
      <header className="px-6 pt-6 pb-2">
        <div className="flex gap-2">
          {PHASES.map((p, idx) => (
            <div
              key={p}
              className={`h-1.5 rounded-full flex-1 transition-spring duration-500 ${idx <= PHASES.indexOf(phase) ? 'bg-primary' : 'bg-gray-200'
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
                <Sparkles size={52} className="text-primary" />
              </motion.div>

              <h1 className="text-2xl font-bold text-gray-900 mb-3">
                {username}님, 환영합니다!
              </h1>
              <p className="text-sm text-gray-500 leading-relaxed mb-10">
                우리 아이를 등록하면<br />
                <span className="font-bold text-primary">AI 맞춤 산책 코스</span>와 <span className="font-bold text-primary">맞춤 알림 서비스</span>를<br />
                바로 시작할 수 있어요!
              </p>

              <div className="space-y-3 mb-10 text-left">
                {[
                  { icon: Sparkles, color: 'text-amber-500', bg: 'bg-amber-50', text: 'AI가 분석하는 맞춤 산책 코스' },
                  { icon: Heart, color: 'text-pink-500', bg: 'bg-pink-50', text: '반려동물 맞춤 콘텐츠 + 커뮤니티' },
                  { icon: MapPin, color: 'text-blue-500', bg: 'bg-blue-50', text: '반려동물 동반 가능 장소 + 맞춤 알림' },
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
                  onClick={() => setShowPetForm(true)}
                  className="w-full py-4 bg-primary text-white font-bold rounded-2xl shadow-md hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.98] transition-spring flex items-center justify-center gap-2"
                >
                  <PawPrint size={20} />
                  반려동물 등록하기
                </button>
                <button
                  onClick={() => handleComplete('home')}
                  className="w-full py-3 text-sm text-gray-400 hover:text-gray-600 transition-spring"
                >
                  나중에 할게요
                </button>
              </div>
            </motion.div>
          )}


          {/* ───── 3단계: 완료 ───── */}
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
                <PawPrint size={52} className="text-green-500" />
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
                  <div className="inline-flex items-center gap-2 bg-primary/10 px-4 py-2 rounded-full mx-auto mb-3">
                    {pet.type === '강아지' ? <Dog size={18} className="text-primary" /> : <Cat size={18} className="text-primary" />}
                    <span className="text-sm font-bold text-primary">
                      {pet.name} · {pet.breed} · {pet.age}살
                    </span>
                  </div>
                )}

                {pet?.notifyEnabled && pet.region && (
                  <div className="inline-flex items-center gap-1.5 bg-blue-50 text-blue-600 text-xs font-bold px-3 py-1.5 rounded-full mx-auto mb-4">
                    <Bell size={12} />
                    {pet.region} 지역 맞춤 알림 설정 완료
                  </div>
                )}

                <p className="text-sm text-gray-500 leading-relaxed mb-8">
                  {pet
                    ? `${pet.name}에게 딱 맞는 산책 코스와\n맞춤 장소를 지금 바로 확인해보세요!`
                    : '멍냥트립의 다양한 서비스를 둘러보세요!\n마이페이지에서 언제든 반려동물을 등록할 수 있어요.'}
                </p>
              </motion.div>

              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.5 }}
                className="space-y-3 mt-auto"
              >
                {pet && (
                  <button
                    onClick={() => handleComplete('ai-walk-guide')}
                    className="w-full py-3.5 bg-amber-500 text-white font-bold rounded-2xl shadow-md hover:bg-amber-600 active:scale-[0.97] transition-spring flex items-center justify-center gap-2"
                  >
                    <Sparkles size={18} />
                    AI 산책 가이드 보러가기
                  </button>
                )}
                <button
                  onClick={() => handleComplete('home')}
                  className={`w-full py-3.5 rounded-2xl font-bold transition-spring active:scale-[0.98] ${pet ? 'bg-gray-100 text-gray-600 hover:bg-gray-200' : 'bg-primary text-white shadow-md hover:bg-primary/90'
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
