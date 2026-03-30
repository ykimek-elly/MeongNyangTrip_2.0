import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore, type PetInfo } from '../store/useAppStore';
import { PawPrint, Heart, MapPin, Sparkles, Bell, Dog, Cat, Phone } from 'lucide-react';
import { authApi } from '../api/authApi';
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
  const [phone, setPhone] = useState('');

  const formatPhone = (v: string) => {
    const digits = v.replace(/\D/g, '').slice(0, 11);
    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  };

  const phoneDigits = phone.replace(/\D/g, '');
  const isPhoneValid = phoneDigits.length >= 10;

  const handlePetSubmit = (petData: PetInfo) => {
    addPet(petData);
    setShowPetForm(false);
    setPhase('complete');
  };

  const handleComplete = (destination: string) => {
    if (isPhoneValid) {
      authApi.savePhone(phoneDigits).catch(() => {});
    }
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

              {/* 카톡 알림 수신 번호 */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 }}
                className="mt-6 p-4 bg-primary/5 rounded-2xl border border-primary/20"
              >
                <div className="flex items-center gap-2 mb-3">
                  <Bell size={16} className="text-primary" />
                  <span className="text-sm font-bold text-gray-800">카톡 알림 수신 번호</span>
                  <span className="text-[11px] text-primary font-medium bg-primary/10 px-2 py-0.5 rounded-full ml-auto">필수</span>
                </div>
                <div className="relative">
                  <Phone size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(formatPhone(e.target.value))}
                    placeholder="010-0000-0000"
                    className="w-full pl-10 pr-4 py-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-primary focus:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] transition-spring text-sm"
                  />
                </div>
                <p className="text-[11px] text-gray-400 mt-2 ml-1">날씨·맞춤 장소·이벤트 알림을 받을 번호를 입력해주세요</p>
              </motion.div>

              <div className="space-y-3 mt-4">
                <button
                  onClick={() => isPhoneValid && setShowPetForm(true)}
                  disabled={!isPhoneValid}
                  className={`w-full py-4 font-bold rounded-2xl shadow-md flex items-center justify-center gap-2 transition-spring ${
                    isPhoneValid
                      ? 'bg-primary text-white hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.98]'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                  }`}
                >
                  <PawPrint size={20} />
                  반려동물 등록하기
                </button>
                <button
                  onClick={() => isPhoneValid && handleComplete('home')}
                  disabled={!isPhoneValid}
                  className={`w-full py-3 text-sm transition-spring ${
                    isPhoneValid
                      ? 'text-gray-400 hover:text-gray-600'
                      : 'text-gray-300 cursor-not-allowed'
                  }`}
                >
                  번호만 등록하고 나중에 할게요
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
