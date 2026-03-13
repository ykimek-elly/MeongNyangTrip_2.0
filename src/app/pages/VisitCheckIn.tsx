import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft,
  Award,
  MapPin,
  Calendar,
  Camera,
  Check,
  Clock,
  TrendingUp,
  LocateFixed,
  ImagePlus,
  AlertCircle,
} from 'lucide-react';

interface VisitCheckInProps {
  onNavigate: (page: string, params?: any) => void;
}

const VISIT_HISTORY = [
  {
    id: 1,
    place: "한강공원 반려동물 놀이터",
    date: "2026-03-04",
    badge: "🏃 활동왕",
    img: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=400&q=80"
  },
  {
    id: 2,
    place: "멍스테이 글램핑",
    date: "2026-03-01",
    badge: "⛺ 캠핑러버",
    img: "https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=400&q=80"
  },
  {
    id: 3,
    place: "우드무드 카페",
    date: "2026-02-28",
    badge: "☕ 카페탐험가",
    img: "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400&q=80"
  },
];

const BADGES = [
  { id: 1, name: "첫 방문", icon: "🎉", unlocked: true, description: "첫 장소 방문 완료" },
  { id: 2, name: "연속 방문 7일", icon: "🔥", unlocked: true, description: "7일 연속 체크인" },
  { id: 3, name: "장소 탐험가", icon: "🗺️", unlocked: true, description: "10곳 이상 방문" },
  { id: 4, name: "사진 수집가", icon: "📸", unlocked: false, description: "30장 이상 업로드" },
  { id: 5, name: "리뷰 마스터", icon: "✍️", unlocked: false, description: "20개 리뷰 작성" },
  { id: 6, name: "인기스타", icon: "⭐", unlocked: false, description: "좋아요 100개 받기" },
];

export function VisitCheckIn({ onNavigate }: VisitCheckInProps) {
  const [activeTab, setActiveTab] = useState<'checkin' | 'history'>('checkin');
  const [photoTaken, setPhotoTaken] = useState(false);
  const [locationStatus, setLocationStatus] = useState<'idle' | 'loading' | 'found' | 'error'>('idle');
  const [locationName, setLocationName] = useState('');
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const totalVisits = VISIT_HISTORY.length;
  const unlockedBadges = BADGES.filter(b => b.unlocked).length;

  const handleGetLocation = () => {
    setLocationStatus('loading');
    navigator.geolocation?.getCurrentPosition(
      () => {
        setLocationStatus('found');
        setLocationName('한강공원 반려동물 놀이터'); // TODO: 실제 역지오코딩 연동
      },
      () => setLocationStatus('error')
    );
  };

  const handleSubmit = () => {
    setShowSuccessModal(true);
    setTimeout(() => {
      setShowSuccessModal(false);
      setPhotoTaken(false);
      setLocationStatus('idle');
      setLocationName('');
    }, 2500);
  };

  const canSubmit = photoTaken && locationStatus === 'found';

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary/5 to-white pb-24">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={22} />
        </button>
        <div className="flex items-center gap-2 ml-2">
          <Camera className="text-primary" size={20} />
          <h1 className="font-bold text-lg">방문 인증</h1>
        </div>
      </header>

      {/* 통계 카드 */}
      <div className="px-5 pt-6 pb-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-br from-primary to-secondary rounded-3xl p-5 text-white shadow-lg"
        >
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3 text-center">
              <MapPin size={16} className="mx-auto mb-1" />
              <div className="text-2xl font-bold">{totalVisits}</div>
              <div className="text-xs opacity-80">방문</div>
            </div>
            <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3 text-center">
              <Award size={16} className="mx-auto mb-1" />
              <div className="text-2xl font-bold">{unlockedBadges}</div>
              <div className="text-xs opacity-80">뱃지</div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* 탭 */}
      <div className="sticky top-14 z-40 bg-white/95 backdrop-blur-sm border-b border-gray-100 px-5 flex gap-1">
        {[
          { id: 'checkin', label: '사진 인증', icon: Camera },
          { id: 'history', label: '방문기록', icon: Clock },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as 'checkin' | 'history')}
            className={`flex-1 py-3 text-sm font-bold transition-colors relative flex items-center justify-center gap-1 ${
              activeTab === tab.id ? 'text-primary' : 'text-gray-400'
            }`}
          >
            <tab.icon size={16} />
            {tab.label}
            {activeTab === tab.id && (
              <motion.div layoutId="tab-indicator" className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
            )}
          </button>
        ))}
      </div>

      {/* 콘텐츠 */}
      <div className="px-5 py-5">
        <AnimatePresence mode="wait">
          {activeTab === 'checkin' && (
            <motion.div
              key="checkin"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-5"
            >
              {/* Step 1: 위치 확인 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">1</span>
                  현재 위치 확인
                </h3>

                {locationStatus === 'idle' && (
                  <button
                    onClick={handleGetLocation}
                    className="w-full py-3.5 rounded-2xl border-2 border-primary/30 text-primary font-bold text-sm flex items-center justify-center gap-2 hover:bg-primary/5 transition-colors"
                  >
                    <LocateFixed size={18} />
                    위치 가져오기
                  </button>
                )}

                {locationStatus === 'loading' && (
                  <div className="w-full py-3.5 rounded-2xl bg-gray-50 text-gray-500 text-sm flex items-center justify-center gap-2">
                    <LocateFixed size={18} className="animate-pulse text-primary" />
                    위치를 확인하는 중...
                  </div>
                )}

                {locationStatus === 'found' && (
                  <div className="w-full py-3.5 px-4 rounded-2xl bg-green-50 border border-green-200 flex items-center gap-3">
                    <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center shrink-0">
                      <MapPin size={16} className="text-green-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-xs text-green-600 font-bold">위치 확인 완료</div>
                      <div className="text-sm font-bold text-gray-800 truncate">{locationName}</div>
                    </div>
                    <Check size={18} className="text-green-600 shrink-0" />
                  </div>
                )}

                {locationStatus === 'error' && (
                  <div className="space-y-2">
                    <div className="w-full py-3 px-4 rounded-2xl bg-red-50 border border-red-100 flex items-center gap-2 text-sm text-red-600">
                      <AlertCircle size={16} className="shrink-0" />
                      위치를 가져올 수 없어요. 권한을 확인해주세요.
                    </div>
                    <button
                      onClick={handleGetLocation}
                      className="w-full py-3 rounded-2xl border border-gray-200 text-gray-600 text-sm font-bold hover:bg-gray-50 transition-colors"
                    >
                      다시 시도
                    </button>
                  </div>
                )}
              </div>

              {/* Step 2: 사진 촬영 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">2</span>
                  방문 사진 촬영
                </h3>
                <button
                  onClick={() => setPhotoTaken(prev => !prev)}
                  className={`w-full h-36 rounded-2xl border-2 border-dashed flex flex-col items-center justify-center gap-2 transition-all ${
                    photoTaken
                      ? 'border-green-400 bg-green-50'
                      : 'border-gray-200 bg-gray-50 hover:border-primary/50 hover:bg-primary/5'
                  }`}
                >
                  {photoTaken ? (
                    <>
                      <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                        <Check size={20} className="text-green-600" />
                      </div>
                      <span className="text-sm font-bold text-green-700">사진 촬영 완료</span>
                      <span className="text-xs text-green-500">탭하면 다시 찍을 수 있어요</span>
                    </>
                  ) : (
                    <>
                      <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                        <ImagePlus size={20} className="text-primary" />
                      </div>
                      <span className="text-sm font-bold text-gray-700">사진 촬영하기</span>
                      <span className="text-xs text-gray-400">반려동물과 함께한 순간을 찍어주세요</span>
                    </>
                  )}
                </button>
              </div>

              {/* 인증 완료 버튼 */}
              <button
                onClick={handleSubmit}
                disabled={!canSubmit}
                className={`w-full py-4 rounded-2xl font-bold text-base transition-all ${
                  canSubmit
                    ? 'bg-primary text-white shadow-lg active:scale-[0.98]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                방문 인증 완료
              </button>

              {/* 획득한 뱃지 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <Award className="text-primary" size={20} />
                  획득한 뱃지
                </h3>
                <div className="grid grid-cols-3 gap-3">
                  {BADGES.filter(b => b.unlocked).map((badge) => (
                    <div
                      key={badge.id}
                      className="bg-gradient-to-br from-primary/10 to-secondary/10 rounded-2xl p-4 text-center border border-primary/20"
                    >
                      <div className="text-3xl mb-2">{badge.icon}</div>
                      <div className="text-xs font-bold text-gray-800">{badge.name}</div>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>
          )}

          {activeTab === 'history' && (
            <motion.div
              key="history"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-4"
            >
              {/* 이번 달 통계 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">이번 달 통계</h3>
                <div className="bg-blue-50 rounded-xl p-3">
                  <div className="text-xs text-blue-600 mb-1">방문 횟수</div>
                  <div className="text-2xl font-bold text-blue-700">{totalVisits}</div>
                  <div className="text-xs text-blue-600 mt-1 flex items-center gap-1">
                    <TrendingUp size={10} />
                    지난달 대비 +2
                  </div>
                </div>
              </div>

              {/* 방문 목록 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">최근 방문</h3>
                <div className="space-y-3">
                  {VISIT_HISTORY.map((visit) => (
                    <div
                      key={visit.id}
                      className="flex gap-3 p-3 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer"
                    >
                      <img
                        src={visit.img}
                        alt={visit.place}
                        className="w-16 h-16 rounded-xl object-cover"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-sm text-gray-900 truncate">{visit.place}</div>
                        <div className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                          <Calendar size={10} />
                          {visit.date}
                        </div>
                        <div className="mt-1.5">
                          <span className="text-xs">{visit.badge}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 전체 뱃지 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">전체 뱃지 ({unlockedBadges}/{BADGES.length})</h3>
                <div className="grid grid-cols-3 gap-3">
                  {BADGES.map((badge) => (
                    <div
                      key={badge.id}
                      className={`rounded-2xl p-4 text-center border-2 ${
                        badge.unlocked
                          ? 'bg-gradient-to-br from-primary/10 to-secondary/10 border-primary/20'
                          : 'bg-gray-50 border-gray-100 opacity-40'
                      }`}
                    >
                      <div className="text-3xl mb-2">{badge.icon}</div>
                      <div className="text-xs font-bold text-gray-800 mb-1">{badge.name}</div>
                      <div className="text-[10px] text-gray-500 leading-tight">{badge.description}</div>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* 인증 완료 모달 */}
      <AnimatePresence>
        {showSuccessModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center px-4 bg-black/60 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.5, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.5, opacity: 0 }}
              className="bg-white w-full max-w-sm rounded-3xl p-8 text-center"
            >
              <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Check className="text-green-600" size={40} />
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-2">인증 완료!</h3>
              <p className="text-gray-500 mb-4">{locationName || '방문 장소'}</p>
              <div className="bg-gradient-to-br from-primary to-secondary text-white rounded-2xl p-5">
                <div className="text-sm font-bold mb-1">🎉 방문이 기록되었어요!</div>
                <div className="text-xs opacity-80">사진과 위치 정보가 저장됐어요</div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
