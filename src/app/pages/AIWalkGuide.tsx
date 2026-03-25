import React, { useState } from 'react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { walkGuideApi, WalkGuideResponse } from '../api/walkGuideApi';
import {
  ArrowLeft,
  Sparkles,
  Cloud,
  Sun,
  CloudRain,
  Wind,
  Thermometer,
  Clock,
  MapPin,
  AlertTriangle,
  Heart,
  Zap,
  Minus,
  Activity,
  Navigation,
  Phone,
  CheckCircle,
  Dog,
  Cat,
  Droplets,
} from 'lucide-react';

interface AIWalkGuideProps {
  onNavigate: (page: string) => void;
}

const WEATHER_CONDITIONS = [
  { id: 'sunny', label: '맑음', icon: Sun, color: 'text-orange-400', bg: 'bg-orange-50' },
  { id: 'cloudy', label: '흐림', icon: Cloud, color: 'text-gray-400', bg: 'bg-gray-50' },
  { id: 'rainy', label: '비', icon: CloudRain, color: 'text-blue-400', bg: 'bg-blue-50' },
  { id: 'windy', label: '바람', icon: Wind, color: 'text-cyan-400', bg: 'bg-cyan-50' },
];

const MOCK_WALK_DATA = {
  summary: "오늘은 산책하기 아주 좋은 날씨예요!",
  temperature: 23,
  weather: 'sunny',
  bestTime: "16:00 - 18:00",
  duration: "30분",
  routes: [
    { id: 1, name: "한강공원 산책로", distance: "2.3km", difficulty: "쉬움", type: "공원" },
    { id: 2, name: "올림픽공원 둘레길", distance: "3.5km", difficulty: "보통", type: "공원" },
    { id: 3, name: "서울숲 반려동물 구역", distance: "1.8km", difficulty: "쉬움", type: "숲" },
  ],
  tips: [
    "현재 기온이 적당해요. 물은 꼭 챙겨주세요!",
    "오후에는 그늘진 곳을 추천해요.",
    "발바닥 화상 주의! 아스팔트 온도를 확인하세요.",
    "15분마다 물을 마시게 해주세요.",
  ],
  emergency: [
    { name: "24시 동물병원", distance: "500m", phone: "02-1234-5678" },
    { name: "반려동물 응급센터", distance: "1.2km", phone: "02-8765-4321" },
  ]
};

export function AIWalkGuide({ onNavigate }: AIWalkGuideProps) {
  const { isLoggedIn, getRepresentativePet, addSavedRoute } = useAppStore();
  const pet = getRepresentativePet();
  const [isLoading, setIsLoading] = useState(false);
  const [showRecommendation, setShowRecommendation] = useState(false);
  const [showPopup, setShowPopup] = useState(false);
  const [selectedDogSize, setSelectedDogSize] = useState<string>(pet?.size || 'MEDIUM');
  const [selectedActivity, setSelectedActivity] = useState<string>(pet?.activity || 'NORMAL');

  const [guideData, setGuideData] = useState<WalkGuideResponse | null>(null);

  const handleGenerateGuide = async () => {
    setIsLoading(true);
    try {
      const result = await walkGuideApi.generate({
        petSize: (selectedDogSize as 'SMALL' | 'MEDIUM' | 'LARGE'),
        activityLevel: (selectedActivity as 'LOW' | 'NORMAL' | 'HIGH'),
      });
      setGuideData(result);
    } catch {
      // API 미연동 / mock 모드 → 기존 mock 데이터 사용
      setGuideData(MOCK_WALK_DATA as WalkGuideResponse);
    } finally {
      setIsLoading(false);
      setShowRecommendation(true);
    }
  };

  const recommendation = guideData ?? MOCK_WALK_DATA as WalkGuideResponse;

  const handleSaveRoute = () => {
    if (!isLoggedIn) {
      alert("로그인이 필요합니다.");
      onNavigate('login');
      return;
    }
    
    addSavedRoute({
      id: Date.now().toString(),
      date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }),
      weather: recommendation.weather,
      temperature: recommendation.temperature,
      bestTime: recommendation.bestTime,
      routes: recommendation.routes.map(r => ({ name: r.name, distance: r.distance, type: r.type }))
    });

    setShowPopup(true);
    setTimeout(() => {
      setShowPopup(false);
    }, 3000);
  };

  const currentWeather = WEATHER_CONDITIONS.find(w => w.id === recommendation.weather);

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary/5 to-white pb-24">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full transition-spring hover:scale-[1.1] active:scale-[0.9]">
          <ArrowLeft size={22} />
        </button>
        <div className="flex items-center gap-2 ml-2">
          <Sparkles className="text-primary" size={20} />
          <h1 className="font-bold text-lg">AI 산책 가이드</h1>
        </div>
      </header>

      <div className="px-5 py-6">
        {!showRecommendation ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="space-y-6"
          >
            {/* Hero Section */}
            <div className="text-center mb-8 animate-fade-in-up">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', damping: 12, stiffness: 200, delay: 0.15 }}
                className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4"
              >
                <Sparkles className="text-primary" size={40} />
              </motion.div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                {isLoggedIn && pet ? (
                  <>{pet.name}의 오늘의 산책,<br />AI가 알려드릴게요!</>
                ) : (
                  <>오늘의 산책,<br />AI가 알려드릴게요!</>
                )}
              </h2>
              <p className="text-gray-500">날씨, 시간, 우리 아이 컨디션을 고려한<br />맞춤형 산책 가이드를 받아보세요.</p>
            </div>

            {/* Current Weather — Double-Bezel */}
            <div className="p-1 bg-primary/5 rounded-[1.6rem] ring-1 ring-primary/10 animate-fade-in-up" style={{ animationDelay: '0.1s' }}>
            <div className="bg-white rounded-[1.25rem] p-5">
              <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Sun className="text-orange-400" size={20} />
                현재 날씨
              </h3>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-4xl font-bold text-gray-900">{recommendation.temperature}°C</div>
                  <div className="text-sm text-gray-500 mt-1">맑음, 산책하기 좋은 날씨</div>
                </div>
                <Sun className="text-orange-400 fill-orange-100" size={64} />
              </div>
            </div>
            </div>

            {isLoggedIn && pet ? (
              <div className="bg-primary/5 border border-primary/20 rounded-3xl p-5 mb-6">
                {/* Pet Info (Members) */}
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center text-xl shadow-sm border border-primary/10">
                    {pet.type === '강아지' ? <Dog size={20} className="text-primary" /> : <Cat size={20} className="text-primary" />}
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 text-lg">{pet.name}</h3>
                    <p className="text-xs text-gray-600 mt-0.5">
                      {pet.breed} · {pet.age}살 · {pet.size === 'SMALL' ? '소형견' : pet.size === 'MEDIUM' ? '중형견' : '대형견'} · 
                      활동량 {pet.activity === 'LOW' ? '적음' : pet.activity === 'NORMAL' ? '보통' : '많음'}
                    </p>
                  </div>
                </div>
                <p className="text-sm text-gray-700 mt-3 bg-white p-3 rounded-xl shadow-sm">
                  등록된 정보를 바탕으로 {pet.name}에게 딱 맞는 산책 코스를 추천해 드릴게요!
                </p>
              </div>
            ) : (
              <>
                {/* Dog Size Selection */}
                <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3">우리 아이 크기</h3>
                  <div className="grid grid-cols-3 gap-2">
                    {['SMALL', 'MEDIUM', 'LARGE'].map((size) => (
                      <button
                        key={size}
                        onClick={() => setSelectedDogSize(size)}
                        className={`p-3 rounded-xl border-2 transition-spring hover:scale-[1.04] active:scale-[0.95] ${
                          selectedDogSize === size
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-gray-100 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        <div className="font-bold text-sm">
                          {size === 'SMALL' ? '소형견' : size === 'MEDIUM' ? '중형견' : '대형견'}
                        </div>
                        <div className="text-xs mt-1 opacity-60">
                          {size === 'SMALL' ? '~10kg' : size === 'MEDIUM' ? '10~25kg' : '25kg~'}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Activity Level */}
                <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3">활동량</h3>
                  <div className="grid grid-cols-3 gap-2">
                    {[
                      { id: 'LOW',    label: '적음', Icon: Minus },
                      { id: 'NORMAL', label: '보통', Icon: Activity },
                      { id: 'HIGH',   label: '많음', Icon: Zap },
                    ].map((activity) => {
                      const isActive = selectedActivity === activity.id;
                      return (
                        <button
                          key={activity.id}
                          onClick={() => setSelectedActivity(activity.id)}
                          className={`p-3 rounded-xl border-2 transition-spring hover:scale-[1.04] active:scale-[0.95] ${
                            isActive ? 'border-primary bg-primary/5' : 'border-gray-100 hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex justify-center mb-1">
                            <activity.Icon size={24} className={isActive ? 'text-primary' : 'text-gray-400'} strokeWidth={isActive ? 2.5 : 2} />
                          </div>
                          <div className={`font-bold text-sm ${isActive ? 'text-primary' : 'text-gray-600'}`}>
                            {activity.label}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </>
            )}

            {/* Generate Button */}
            <button
              onClick={handleGenerateGuide}
              disabled={isLoading}
              className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-md hover:bg-primary/90 transition-spring hover:scale-[1.02] active:scale-[0.97] flex items-center justify-center gap-2"
            >
              {isLoading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  AI 분석 중...
                </>
              ) : (
                <>
                  <Sparkles size={20} />
                  맞춤 산책 가이드 받기
                </>
              )}
            </button>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="space-y-4"
          >
            {/* Summary Card */}
            <div className="bg-gradient-to-br from-primary to-secondary rounded-3xl p-6 text-white shadow-md">
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    {currentWeather && <currentWeather.icon size={24} />}
                    <span className="text-sm opacity-90">오늘의 추천</span>
                  </div>
                  <h3 className="text-xl font-bold leading-tight">{recommendation.summary}</h3>
                </div>
                <div className="text-right">
                  <div className="text-3xl font-bold">{recommendation.temperature}°</div>
                  <div className="text-xs opacity-80">완벽해요!</div>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-3 mt-4">
                <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3">
                  <Clock size={16} className="mb-1" />
                  <div className="text-xs opacity-80">최적 시간</div>
                  <div className="font-bold">{recommendation.bestTime}</div>
                </div>
                <div className="bg-white/20 backdrop-blur-sm rounded-xl p-3">
                  <Zap size={16} className="mb-1" />
                  <div className="text-xs opacity-80">추천 시간</div>
                  <div className="font-bold">{recommendation.duration}</div>
                </div>
              </div>
            </div>

            {/* Recommended Routes — Double-Bezel */}
            <div className="p-1 bg-primary/5 rounded-[1.6rem] ring-1 ring-primary/10 animate-fade-in-up" style={{ animationDelay: '0.15s' }}>
            <div className="bg-white rounded-[1.25rem] p-5">
              <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Navigation className="text-primary" size={20} />
                추천 산책로
              </h3>
              <div className="space-y-3">
                {recommendation.routes.map((route, idx) => (
                  <div
                    key={route.name}
                    className={`flex items-center justify-between p-3 bg-gray-50 rounded-xl hover:bg-primary/5 transition-spring hover:scale-[1.01] active:scale-[0.98] cursor-pointer ${idx < 5 ? 'animate-fade-in-up' : ''}`}
                    style={idx < 5 ? { animationDelay: `${0.2 + idx * 0.07}s` } : undefined}
                  >
                    <div className="flex-1">
                      <div className="font-bold text-gray-900">{route.name}</div>
                      <div className="text-xs text-gray-500 mt-1 flex items-center gap-2">
                        <span className="bg-primary/10 text-primary px-2 py-0.5 rounded-full">{route.type}</span>
                        <span>{route.distance}</span>
                        <span>·</span>
                        <span>{route.difficulty}</span>
                      </div>
                    </div>
                    <MapPin className="text-primary" size={20} />
                  </div>
                ))}
              </div>
            </div>
            </div>

            {/* Tips */}
            <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Heart className="text-primary" size={20} />
                산책 꿀팁
              </h3>
              <div className="space-y-2">
                {recommendation.tips.map((tip, idx) => (
                  <div key={idx} className={`flex items-start gap-2 text-sm text-gray-600 leading-relaxed ${idx < 6 ? 'animate-fade-in-up' : ''}`} style={idx < 6 ? { animationDelay: `${0.25 + idx * 0.07}s` } : undefined}>
                    <span className="text-primary mt-0.5">•</span>
                    <span>{tip}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Emergency Contacts */}
            <div className="bg-red-50 rounded-3xl p-5 border border-red-100">
              <h3 className="font-bold text-red-600 mb-3 flex items-center gap-2">
                <AlertTriangle size={20} />
                응급 연락처
              </h3>
              <div className="space-y-2">
                {recommendation.emergency?.map((contact, idx) => (
                  <div key={idx} className="flex items-center justify-between p-3 bg-white rounded-xl">
                    <div>
                      <div className="font-bold text-gray-900 text-sm">{contact.name}</div>
                      <div className="text-xs text-gray-500 mt-0.5">{contact.distance} 거리</div>
                    </div>
                    <a
                      href={`tel:${contact.phone}`}
                      className="flex items-center gap-1 text-primary font-bold text-sm"
                    >
                      <Phone size={14} />
                      {contact.phone}
                    </a>
                  </div>
                ))}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <button
                onClick={() => setShowRecommendation(false)}
                className="py-3 bg-gray-100 text-gray-700 font-bold rounded-xl hover:bg-gray-200 transition-spring hover:scale-[1.02] active:scale-[0.97]"
              >
                다시 설정
              </button>
              <button
                onClick={handleSaveRoute}
                className="py-3 bg-primary text-white font-bold rounded-xl hover:bg-primary/90 transition-spring hover:scale-[1.02] active:scale-[0.97] relative"
              >
                추천 경로 저장
              </button>
            </div>
          </motion.div>
        )}
      </div>

      {/* Save Popup */}
      {showPopup && (
        <motion.div
          initial={{ opacity: 0, y: 50, x: '-50%' }}
          animate={{ opacity: 1, y: 0, x: '-50%' }}
          exit={{ opacity: 0, y: 50, x: '-50%' }}
          className="fixed bottom-24 left-1/2 -translate-x-1/2 bg-gray-900 text-white px-6 py-3 rounded-full shadow-2xl z-50 flex items-center gap-2 whitespace-nowrap"
        >
          <CheckCircle className="text-primary" size={20} />
          <span className="font-medium">마이페이지에 저장되었습니다.</span>
        </motion.div>
      )}
    </div>
  );
}