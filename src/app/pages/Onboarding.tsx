import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore, type PetInfo } from '../store/useAppStore';
import { PawPrint, Heart, MapPin, Sparkles, Bell, Phone, Dog, Cat, User } from 'lucide-react';
import { authApi } from '../api/authApi';
import { PetProfileForm } from '../components/PetProfileForm';
import regionCoordinates from '../../../exports/region-coordinates.json';

const SIDO_LIST = Object.keys(regionCoordinates) as (keyof typeof regionCoordinates)[];
const DEFAULT_LAT = 37.5172;
const DEFAULT_LNG = 127.0473;

interface OnboardingProps {
  onNavigate: (page: string) => void;
}

const PHASES = ['welcome', 'complete'] as const;
type Phase = typeof PHASES[number];

export function Onboarding({ onNavigate }: OnboardingProps) {
  const { username, pets, addPet, completeOnboarding, setUserRegion, updateProfile } = useAppStore();
  const pet = pets.find(p => p.isRepresentative) ?? pets[0] ?? null;

  const [phase, setPhase] = useState<Phase>('welcome');
  const [showPetForm, setShowPetForm] = useState(false);

  // 휴대폰 중복확인
  const [phone, setPhone] = useState('');
  const [phoneError, setPhoneError] = useState('');
  const [isCheckingPhone, setIsCheckingPhone] = useState(false);
  const [isPhoneChecked, setIsPhoneChecked] = useState(false);

  // 카톡 동의
  const [agreeKakao, setAgreeKakao] = useState(false);

  // 닉네임 (소셜 로그인 대응, 미입력시 자동생성)
  const [nickname, setNickname] = useState(username || '');

  // 활동 지역
  const [sido, setSido] = useState('');
  const [district, setDistrict] = useState('');
  const [activityRadius, setActivityRadius] = useState<5 | 15 | 30>(15);

  const formatPhone = (v: string) => {
    const digits = v.replace(/\D/g, '').slice(0, 11);
    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  };
  const phoneDigits = phone.replace(/\D/g, '');
  const isPhoneValid = phoneDigits.length >= 10;

  const handleCheckPhone = async () => {
    if (!isPhoneValid || isCheckingPhone) return;
    setIsCheckingPhone(true);
    setPhoneError('');
    setIsPhoneChecked(false);
    try {
      const available = await authApi.checkPhone(phoneDigits);
      if (available) {
        setIsPhoneChecked(true);
      } else {
        setPhoneError('이미 사용 중인 휴대폰 번호입니다.');
      }
    } catch {
      setPhoneError('확인에 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsCheckingPhone(false);
    }
  };

  const districts = sido
    ? Object.keys((regionCoordinates as Record<string, Record<string, { lat: number; lng: number }>>)[sido] ?? {})
    : [];
  const selectedCoords = sido && district
    ? (regionCoordinates as Record<string, Record<string, { lat: number; lng: number }>>)[sido]?.[district]
    : null;

  const isRegionValid = !!sido;
  const canProceed = isRegionValid && isPhoneChecked && agreeKakao;

  const handlePetSubmit = (petData: PetInfo) => {
    addPet(petData);
    setShowPetForm(false);
    setPhase('complete');
  };

  const handleComplete = async (destination: string) => {
    let finalNickname = nickname.trim();
    if (!finalNickname) {
      finalNickname = `멍냥이_${Math.floor(Math.random() * 9000) + 1000}`;
    }

    if (finalNickname !== username) {
      try {
        await authApi.updateProfile(finalNickname);
        updateProfile({ username: finalNickname });
      } catch (err) {
        console.error('닉네임 저장 실패:', err);
      }
    }

    if (isPhoneChecked && isPhoneValid) {
      authApi.savePhone(phoneDigits).catch(() => {});
    }
    const regionText = sido && district ? `${sido} ${district}` : sido || '';
    authApi.saveLocation(
      selectedCoords?.lat ?? DEFAULT_LAT,
      selectedCoords?.lng ?? DEFAULT_LNG,
      activityRadius,
      regionText
    ).catch(() => {});
    setUserRegion(sido, district, activityRadius);
    completeOnboarding();
    onNavigate(destination);
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <header className="px-6 pt-6 pb-2">
        <div className="flex gap-2">
          {PHASES.map((p, idx) => (
            <div
              key={p}
              className={`h-1.5 rounded-full flex-1 transition-spring duration-500 ${
                idx <= PHASES.indexOf(phase) ? 'bg-primary' : 'bg-gray-200'
              }`}
            />
          ))}
        </div>
      </header>

      <main className="flex-1 px-6 py-6 flex flex-col overflow-y-auto">
        <AnimatePresence mode="wait">

          {/* ───── 웰컴 단계 ───── */}
          {phase === 'welcome' && (
            <motion.div
              key="welcome"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -30 }}
              className="flex flex-col"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', damping: 12, stiffness: 200, delay: 0.2 }}
                className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6"
              >
                <Sparkles size={44} className="text-primary" />
              </motion.div>

              <h1 className="text-2xl font-bold text-gray-900 mb-2 text-center">
                {username}님, 환영합니다!
              </h1>
              <p className="text-sm text-gray-500 leading-relaxed mb-6 text-center">
                <span className="font-bold text-primary">AI 맞춤 산책 코스</span>와{' '}
                <span className="font-bold text-primary">맞춤 알림 서비스</span>를<br />
                바로 시작할 수 있어요!
              </p>

              <div className="space-y-2 mb-6">
                {[
                  { icon: Sparkles, color: 'text-amber-500', bg: 'bg-amber-50', text: 'AI가 분석하는 맞춤 산책 코스' },
                  { icon: Heart, color: 'text-pink-500', bg: 'bg-pink-50', text: '반려동물 맞춤 콘텐츠 + 커뮤니티' },
                  { icon: MapPin, color: 'text-blue-500', bg: 'bg-blue-50', text: '반려동물 동반 가능 장소 + 맞춤 알림' },
                ].map((item, idx) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.35 + idx * 0.12 }}
                    className="flex items-center gap-3 p-3 bg-gray-50 rounded-2xl"
                  >
                    <div className={`w-9 h-9 ${item.bg} rounded-xl flex items-center justify-center shrink-0`}>
                      <item.icon size={18} className={item.color} />
                    </div>
                    <span className="text-sm font-bold text-gray-700">{item.text}</span>
                  </motion.div>
                ))}
              </div>

              {/* ── 휴대폰 문자 인증 ── */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 }}
                className="p-4 bg-primary/5 rounded-2xl border border-primary/20 mb-3"
              >
                <div className="flex items-center gap-2 mb-3">
                  <User size={16} className="text-primary" />
                  <span className="text-sm font-bold text-gray-800">닉네임 <span className="text-destructive">(필수)</span></span>
                </div>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="사용할 닉네임을 입력하세요 (미입력시 자동 생성)"
                  className="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl outline-none transition-spring text-sm focus:border-primary mb-5"
                />

                <div className="flex items-center gap-2 mb-3">
                  <Phone size={16} className="text-primary" />
                  <span className="text-sm font-bold text-gray-800">휴대폰 번호 <span className="text-destructive">(필수)</span></span>
                </div>

                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Phone size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input
                      type="tel"
                      value={phone}
                      onChange={(e) => {
                        setPhone(formatPhone(e.target.value));
                        setIsPhoneChecked(false);
                        setPhoneError('');
                      }}
                      disabled={isPhoneChecked}
                      placeholder="010-0000-0000"
                      className={`w-full pl-9 pr-3 py-2.5 bg-white border rounded-xl outline-none transition-spring text-sm ${
                        isPhoneChecked ? 'border-green-300 bg-green-50 text-green-700' : phoneError ? 'border-destructive' : 'border-gray-200 focus:border-primary'
                      }`}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={handleCheckPhone}
                    disabled={!isPhoneValid || isCheckingPhone || isPhoneChecked}
                    className={`shrink-0 px-3 py-2.5 rounded-xl text-xs font-bold transition-spring ${
                      isPhoneValid && !isCheckingPhone && !isPhoneChecked
                        ? 'bg-primary text-white active:scale-95'
                        : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    }`}
                  >
                    {isCheckingPhone ? '확인 중…' : '중복확인'}
                  </button>
                </div>

                {phoneError && <p className="text-xs text-destructive mt-1.5 ml-1">{phoneError}</p>}
                {isPhoneChecked && (
                  <p className="text-xs text-green-600 font-bold mt-1.5 ml-1">✓ 사용 가능한 번호예요</p>
                )}

                {/* 카톡 알림 동의 */}
                <label className="flex items-start gap-3 cursor-pointer mt-3 p-3 bg-yellow-50 rounded-xl border border-yellow-200">
                  <input
                    type="checkbox"
                    checked={agreeKakao}
                    onChange={(e) => setAgreeKakao(e.target.checked)}
                    className="w-5 h-5 mt-0.5 rounded accent-yellow-400 shrink-0"
                  />
                  <span className="text-xs text-gray-600 flex items-center gap-1 flex-1 min-w-0">
                    <span><span className="font-bold text-gray-800">카카오톡 알림 수신</span>에 동의합니다 <span className="text-destructive">(필수)</span></span>
                    <span className="text-gray-400 ml-auto shrink-0">날씨·맞춤 장소·이벤트 알림</span>
                  </span>
                </label>
              </motion.div>

              {/* ── 활동 지역 선택 ── */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.85 }}
                className="p-4 bg-gray-50 rounded-2xl border border-gray-100 mb-4"
              >
                <div className="flex items-center gap-2 mb-3">
                  <MapPin size={16} className="text-primary" />
                  <span className="text-sm font-bold text-gray-800">
                    활동 지역 <span className="text-destructive">(필수)</span>
                  </span>
                  <span className="text-[11px] text-gray-400 ml-auto">미선택 시 서울 강남구 기본</span>
                </div>
                <div className="flex gap-2">
                  <select
                    value={sido}
                    onChange={e => { setSido(e.target.value); setDistrict(''); }}
                    className="flex-1 px-3 py-2.5 bg-white border border-gray-200 rounded-xl text-sm text-gray-700 outline-none focus:border-primary transition-spring"
                  >
                    <option value="">시·도 선택</option>
                    {SIDO_LIST.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                  <select
                    value={district}
                    onChange={e => setDistrict(e.target.value)}
                    disabled={!sido}
                    className="flex-1 px-3 py-2.5 bg-white border border-gray-200 rounded-xl text-sm text-gray-700 outline-none focus:border-primary transition-spring disabled:bg-gray-50 disabled:text-gray-300"
                  >
                    <option value="">시·군·구 선택</option>
                    {districts.map(d => <option key={d} value={d}>{d}</option>)}
                  </select>
                </div>

                {/* 활동 반경 */}
                <div className="mt-3 pt-3 border-t border-gray-200">
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-xs font-bold text-gray-700">활동 반경</p>
                    <span className="text-[11px] text-gray-400">선택한 지역 기준으로 장소를 추천해드려요</span>
                  </div>
                  <div className="flex rounded-2xl overflow-hidden border-2 border-gray-100 h-14">
                    {([
                      { value: 5  as const, label: '5km',   desc: '가까운 거리' },
                      { value: 15 as const, label: '15km',  desc: '중간 거리' },
                      { value: 30 as const, label: '먼거리', desc: '넓은 범위' },
                    ]).map((rs, idx) => {
                      const filled = idx <= [5, 15, 30].indexOf(activityRadius);
                      return (
                        <button
                          key={rs.value}
                          type="button"
                          onClick={() => setActivityRadius(rs.value)}
                          className={`flex-1 flex flex-col items-center justify-center gap-0.5 transition-spring active:opacity-80 ${
                            filled ? 'bg-primary' : 'bg-white'
                          } ${idx > 0 ? 'border-l-2 border-gray-100' : ''}`}
                        >
                          <span className={`text-xs font-bold ${filled ? 'text-white' : 'text-gray-600'}`}>{rs.label}</span>
                          <span className={`text-[9px] ${filled ? 'text-white/80' : 'text-gray-400'}`}>{rs.desc}</span>
                        </button>
                      );
                    })}
                  </div>
                  <div className="flex justify-between mt-1 px-1">
                    <span className="text-[9px] text-gray-400">집 근처</span>
                    <span className="text-[9px] text-gray-400">광역 탐색</span>
                  </div>
                </div>
              </motion.div>

              <div className="space-y-3">
                <button
                  onClick={() => canProceed && setShowPetForm(true)}
                  disabled={!canProceed}
                  className={`w-full py-4 font-bold rounded-2xl shadow-md flex items-center justify-center gap-2 transition-spring ${
                    canProceed
                      ? 'bg-primary text-white hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.98]'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                  }`}
                >
                  <PawPrint size={20} />
                  반려동물 등록하기
                </button>
                <button
                  onClick={() => canProceed && handleComplete('home')}
                  disabled={!canProceed}
                  className={`w-full py-3 text-sm transition-spring ${
                    canProceed
                      ? 'text-gray-400 hover:text-gray-600'
                      : 'text-gray-300 cursor-not-allowed'
                  }`}
                >
                  나중에 반려동물 등록할게요
                </button>
              </div>

              <div className="h-8" />
            </motion.div>
          )}

          {/* ───── 완료 단계 ───── */}
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
                  className={`w-full py-3.5 rounded-2xl font-bold transition-spring active:scale-[0.98] ${
                    pet ? 'bg-gray-100 text-gray-600 hover:bg-gray-200' : 'bg-primary text-white shadow-md hover:bg-primary/90'
                  }`}
                >
                  홈으로 가기
                </button>
              </motion.div>
            </motion.div>
          )}

        </AnimatePresence>
      </main>

      {/* 반려동물 등록 폼 */}
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
