import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  ArrowLeft, 
  QrCode,
  Award,
  MapPin,
  Calendar,
  Star,
  Trophy,
  Gift,
  Camera,
  Check,
  Clock,
  TrendingUp,
  Users,
  Zap
} from 'lucide-react';

interface VisitCheckInProps {
  onNavigate: (page: string, params?: any) => void;
}

const VISIT_HISTORY = [
  {
    id: 1,
    place: "한강공원 반려동물 놀이터",
    date: "2026-03-04",
    points: 50,
    badge: "🏃 활동왕",
    img: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=400&q=80"
  },
  {
    id: 2,
    place: "멍스테이 글램핑",
    date: "2026-03-01",
    points: 100,
    badge: "⛺ 캠핑러버",
    img: "https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=400&q=80"
  },
  {
    id: 3,
    place: "우드무드 카페",
    date: "2026-02-28",
    points: 30,
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

const REWARDS = [
  { id: 1, title: "스타벅스 아메리카노", points: 500, category: "카페", available: true },
  { id: 2, title: "반려동물 간식 세트", points: 800, category: "간식", available: true },
  { id: 3, title: "펫 호텔 10% 할인", points: 1000, category: "숙박", available: false },
  { id: 4, title: "펫 카페 무료 입장", points: 300, category: "카페", available: true },
];

export function VisitCheckIn({ onNavigate }: VisitCheckInProps) {
  const [activeTab, setActiveTab] = useState<'checkin' | 'history' | 'rewards'>('checkin');
  const [showScanModal, setShowScanModal] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const totalPoints = 850;
  const totalVisits = VISIT_HISTORY.length;
  const unlockedBadges = BADGES.filter(b => b.unlocked).length;

  const handleScan = () => {
    setShowScanModal(true);
    // Simulate scanning
    setTimeout(() => {
      setShowScanModal(false);
      setShowSuccessModal(true);
      setTimeout(() => setShowSuccessModal(false), 3000);
    }, 2000);
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary/5 to-white pb-24">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={22} />
        </button>
        <div className="flex items-center gap-2 ml-2">
          <Award className="text-primary" size={20} />
          <h1 className="font-bold text-lg">방문 인증</h1>
        </div>
      </header>

      {/* Stats Card */}
      <div className="px-5 pt-6 pb-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-br from-primary to-secondary rounded-3xl p-6 text-white shadow-lg"
        >
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="text-sm opacity-90 mb-1">내 포인트</div>
              <div className="text-4xl font-bold">{totalPoints}P</div>
            </div>
            <Trophy className="opacity-20" size={80} />
          </div>
          
          <div className="grid grid-cols-3 gap-3">
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
            <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3 text-center">
              <Star size={16} className="mx-auto mb-1" />
              <div className="text-2xl font-bold">12</div>
              <div className="text-xs opacity-80">리뷰</div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Tabs */}
      <div className="sticky top-14 z-40 bg-white/95 backdrop-blur-sm border-b border-gray-100 px-5 flex gap-1">
        {[
          { id: 'checkin', label: '체크인', icon: QrCode },
          { id: 'history', label: '방문기록', icon: Clock },
          { id: 'rewards', label: '리워드', icon: Gift },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
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

      {/* Content */}
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
              {/* QR Scan Button */}
              <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 text-center">
                <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                  <QrCode className="text-primary" size={48} />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">QR 코드 스캔</h3>
                <p className="text-sm text-gray-500 mb-6">장소에 비치된 QR 코드를 스캔하고<br />포인트를 적립하세요!</p>
                <button
                  onClick={handleScan}
                  className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-lg hover:bg-primary/90 active:scale-[0.98] transition-all"
                >
                  스캔 시작하기
                </button>
              </div>

              {/* Recent Badges */}
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

              {/* Point Guide */}
              <div className="bg-amber-50 rounded-3xl p-5 border border-amber-100">
                <h3 className="font-bold text-amber-900 mb-3 flex items-center gap-2">
                  <Zap className="text-amber-600" size={20} />
                  포인트 적립 안내
                </h3>
                <div className="space-y-2 text-sm text-amber-900">
                  <div className="flex items-center justify-between p-2 bg-white rounded-lg">
                    <span>장소 방문 체크인</span>
                    <span className="font-bold text-primary">+50P</span>
                  </div>
                  <div className="flex items-center justify-between p-2 bg-white rounded-lg">
                    <span>리뷰 작성</span>
                    <span className="font-bold text-primary">+30P</span>
                  </div>
                  <div className="flex items-center justify-between p-2 bg-white rounded-lg">
                    <span>사진 업로드</span>
                    <span className="font-bold text-primary">+20P</span>
                  </div>
                  <div className="flex items-center justify-between p-2 bg-white rounded-lg">
                    <span>친구 초대</span>
                    <span className="font-bold text-primary">+100P</span>
                  </div>
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
              {/* Monthly Stats */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">이번 달 통계</h3>
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-blue-50 rounded-xl p-3">
                    <div className="text-xs text-blue-600 mb-1">방문 횟수</div>
                    <div className="text-2xl font-bold text-blue-700">{totalVisits}</div>
                    <div className="text-xs text-blue-600 mt-1 flex items-center gap-1">
                      <TrendingUp size={10} />
                      지난달 대비 +2
                    </div>
                  </div>
                  <div className="bg-green-50 rounded-xl p-3">
                    <div className="text-xs text-green-600 mb-1">획득 포인트</div>
                    <div className="text-2xl font-bold text-green-700">180P</div>
                    <div className="text-xs text-green-600 mt-1 flex items-center gap-1">
                      <TrendingUp size={10} />
                      지난달 대비 +50
                    </div>
                  </div>
                </div>
              </div>

              {/* History List */}
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
                        <div className="text-xs text-gray-500 mt-1 flex items-center gap-2">
                          <Calendar size={10} />
                          {visit.date}
                        </div>
                        <div className="mt-1 flex items-center gap-2">
                          <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded-full font-bold">
                            +{visit.points}P
                          </span>
                          <span className="text-xs">{visit.badge}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* All Badges */}
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

          {activeTab === 'rewards' && (
            <motion.div
              key="rewards"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-4"
            >
              {/* Current Points */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 text-center">
                <div className="text-sm text-gray-500 mb-1">사용 가능한 포인트</div>
                <div className="text-4xl font-bold text-primary mb-2">{totalPoints}P</div>
                <div className="text-xs text-gray-500">다음 등급까지 150P 남았어요!</div>
                <div className="w-full bg-gray-100 rounded-full h-2 mt-3">
                  <div className="bg-primary h-full rounded-full" style={{ width: '70%' }} />
                </div>
              </div>

              {/* Rewards Grid */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-4">포인트로 교환하기</h3>
                <div className="space-y-3">
                  {REWARDS.map((reward) => (
                    <div
                      key={reward.id}
                      className={`p-4 rounded-2xl border-2 transition-all ${
                        reward.available
                          ? 'border-gray-100 bg-gray-50 hover:border-primary/30 hover:bg-primary/5 cursor-pointer'
                          : 'border-gray-100 bg-gray-50 opacity-50'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex-1">
                          <div className="font-bold text-gray-900">{reward.title}</div>
                          <div className="text-xs text-gray-500 mt-1">
                            <span className="bg-primary/10 text-primary px-2 py-0.5 rounded-full">{reward.category}</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="font-bold text-primary text-lg">{reward.points}P</div>
                          {!reward.available && (
                            <div className="text-xs text-gray-400 mt-1">포인트 부족</div>
                          )}
                        </div>
                      </div>
                      {reward.available && (
                        <button className="w-full mt-2 py-2 bg-primary text-white font-bold text-sm rounded-xl hover:bg-primary/90 transition-colors">
                          교환하기
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* How to Earn More */}
              <div className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-3xl p-5 border border-blue-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <Gift className="text-primary" size={20} />
                  더 많은 포인트 받는 법
                </h3>
                <div className="space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2 p-2 bg-white rounded-lg">
                    <Users className="text-primary flex-shrink-0" size={16} />
                    <span>친구 초대하고 <strong className="text-primary">100P</strong> 받기</span>
                  </div>
                  <div className="flex items-center gap-2 p-2 bg-white rounded-lg">
                    <Camera className="text-primary flex-shrink-0" size={16} />
                    <span>방문 사진 업로드하고 <strong className="text-primary">20P</strong> 받기</span>
                  </div>
                  <div className="flex items-center gap-2 p-2 bg-white rounded-lg">
                    <Star className="text-primary flex-shrink-0" size={16} />
                    <span>상세 리뷰 작성하고 <strong className="text-primary">30P</strong> 받기</span>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* QR Scan Modal */}
      <AnimatePresence>
        {showScanModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center px-4 bg-black/60 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white w-full max-w-sm rounded-3xl p-8 text-center"
            >
              <div className="w-48 h-48 mx-auto mb-6 bg-gray-900 rounded-2xl flex items-center justify-center relative overflow-hidden">
                <div className="absolute inset-0 border-4 border-primary rounded-2xl animate-pulse" />
                <QrCode className="text-white" size={64} />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">QR 코드 스캔 중...</h3>
              <p className="text-sm text-gray-500">카메라를 QR 코드에 맞춰주세요</p>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Success Modal */}
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
              <h3 className="text-2xl font-bold text-gray-900 mb-2">체크인 완료!</h3>
              <p className="text-gray-500 mb-4">한강공원 반려동물 놀이터</p>
              <div className="bg-gradient-to-br from-primary to-secondary text-white rounded-2xl p-6">
                <div className="text-sm opacity-90 mb-1">획득 포인트</div>
                <div className="text-4xl font-bold">+50P</div>
                <div className="text-sm opacity-90 mt-2">🎉 새로운 뱃지 획득!</div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
