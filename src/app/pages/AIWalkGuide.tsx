import React, { useState, useEffect } from 'react';
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
  Star,
  ExternalLink,
} from 'lucide-react';

interface AIWalkGuideProps {
  onNavigate: (page: string) => void;
}

const WEATHER_MAPPING: Record<string, { icon: any, color: string, label: string }> = {
  'SUNNY': { icon: Sun, color: 'text-orange-400', label: '맑음' },
  'CLOUDY': { icon: Cloud, color: 'text-gray-400', label: '흐림' },
  'RAINY': { icon: CloudRain, color: 'text-blue-400', label: '비' },
  'SNOWY': { icon: Droplets, color: 'text-blue-300', label: '눈' }, // Droplets as placeholder for snow
};

const DEFAULT_TIPS = [
  "현재 기온이 적당해요. 물은 꼭 챙겨주세요!",
  "오후에는 그늘진 곳을 추천해요.",
  "발바닥 화상 주의! 아스팔트 온도를 확인하세요.",
  "15분마다 물을 마시게 해주세요.",
];

const DEFAULT_EMERGENCY = [
  { name: "24시 동물병원", distance: "500m", phone: "02-1234-5678" },
  { name: "반려동물 응급센터", distance: "1.2km", phone: "02-8765-4321" },
];

export function AIWalkGuide({ onNavigate }: AIWalkGuideProps) {
  const { isLoggedIn, getRepresentativePet, addSavedRoute, userLocation } = useAppStore();
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
        lat: userLocation?.lat ?? undefined,
        lng: userLocation?.lng ?? undefined,
      });
      setGuideData(result);
      setShowRecommendation(true);
    } catch (err) {
      console.error('AI 가이드 생성 실패:', err);
      alert('AI 산책 가이드를 생성하는 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveRoute = () => {
    if (!isLoggedIn) {
      alert("로그인이 필요합니다.");
      onNavigate('login');
      return;
    }
    
    if (guideData) {
      addSavedRoute({
        id: Date.now().toString(),
        date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }),
        weather: guideData.weatherType.toLowerCase() as any,
        temperature: 20, // Real API doesn't provide temp yet, using default
        bestTime: "지금",
        routes: [{ name: guideData.place.title, distance: "추천 장소", type: guideData.place.category }]
      });

      setShowPopup(true);
      setTimeout(() => setShowPopup(false), 3000);
    }
  };

  const weatherInfo = guideData ? (WEATHER_MAPPING[guideData.weatherType] || WEATHER_MAPPING['SUNNY']) : WEATHER_MAPPING['SUNNY'];

  return (
    <div className="bg-gradient-to-b from-primary/5 to-white pb-6 min-h-screen">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-800 hover:bg-gray-100 rounded-full transition-shadow">
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
            className="space-y-6"
          >
            <div className="text-center mb-8">
              <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                <Sparkles className="text-primary" size={40} />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                {isLoggedIn && pet ? (
                  <>{pet.name}의 오늘의 산책,<br />AI가 알려드릴게요!</>
                ) : (
                  <>오늘의 산책,<br />AI가 알려드릴게요!</>
                )}
              </h2>
              <p className="text-gray-500">날씨, 우리 아이 컨디션을 고려한<br />맞춤형 산책 가이드를 받아보세요.</p>
            </div>

            {/* Selection UI */}
            <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-800 mb-3">우리 아이 크기</h3>
              <div className="grid grid-cols-3 gap-2">
                {['SMALL', 'MEDIUM', 'LARGE'].map((size) => (
                  <button
                    key={size}
                    onClick={() => setSelectedDogSize(size)}
                    className={`p-3 rounded-xl border-2 transition-all ${
                      selectedDogSize === size
                        ? 'border-primary bg-primary/5 text-primary'
                        : 'border-gray-100 text-gray-600'
                    }`}
                  >
                    <div className="font-bold text-sm">
                      {size === 'SMALL' ? '소형견' : size === 'MEDIUM' ? '중형견' : '대형견'}
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-800 mb-3">활동량</h3>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'LOW', label: '적음', Icon: Minus },
                  { id: 'NORMAL', label: '보통', Icon: Activity },
                  { id: 'HIGH', label: '많음', Icon: Zap },
                ].map((activity) => (
                  <button
                    key={activity.id}
                    onClick={() => setSelectedActivity(activity.id)}
                    className={`p-3 rounded-xl border-2 transition-all ${
                      selectedActivity === activity.id ? 'border-primary bg-primary/5' : 'border-gray-100'
                    }`}
                  >
                    <div className="flex justify-center mb-1">
                      <activity.Icon size={24} className={selectedActivity === activity.id ? 'text-primary' : 'text-gray-400'} />
                    </div>
                    <div className="font-bold text-sm text-center">{activity.label}</div>
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={handleGenerateGuide}
              disabled={isLoading}
              className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-md flex items-center justify-center gap-2 hover:bg-primary/90 disabled:bg-gray-300"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Sparkles size={20} />
              )}
              {isLoading ? 'AI 분석 중...' : '맞춤 산책 가이드 받기'}
            </button>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Weather & AI Summary */}
            <div className="bg-gradient-to-br from-primary to-secondary rounded-3xl p-6 text-white shadow-lg">
              <div className="flex items-center gap-3 mb-3">
                <div className="bg-white/20 p-2 rounded-xl backdrop-blur-sm">
                  {weatherInfo && <weatherInfo.icon size={28} />}
                </div>
                <div>
                  <div className="text-xs opacity-90">오늘의 산책 지수</div>
                  <div className="text-lg font-bold">{guideData?.weatherWalkLevel}</div>
                </div>
              </div>
              <p className="text-sm leading-relaxed opacity-95 bg-black/10 p-3 rounded-2xl border border-white/10">
                {guideData?.weatherSummary}
              </p>
            </div>

            {/* Detailed Recommendation Text */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 relative overflow-hidden">
               <div className="absolute top-0 left-0 w-1 h-full bg-primary" />
               <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                 <Sparkles className="text-primary" size={18} />
                 AI 산책 커스텀 가이드
               </h3>
               <p className="text-gray-700 leading-relaxed text-[15px] whitespace-pre-wrap">
                 {guideData?.recommendationDescription}
               </p>
            </div>

            {/* Recommended Place Card */}
            {guideData?.place && (
              <div className="space-y-3">
                <h3 className="font-bold text-gray-800 px-1">추천 장소</h3>
                <div 
                  className="bg-white rounded-3xl overflow-hidden shadow-md border border-gray-100 group"
                  onClick={() => onNavigate(`place-${guideData.place.id}`)}
                >
                  <div className="relative h-48 overflow-hidden">
                    <img 
                      src={guideData.place.imageUrl || 'https://via.placeholder.com/400x200?text=No+Image'} 
                      alt={guideData.place.title}
                      className="w-full h-full object-cover transition-transform group-hover:scale-105"
                    />
                    <div className="absolute top-3 right-3 bg-white/90 backdrop-blur-sm px-2 py-1 rounded-lg text-xs font-bold flex items-center gap-1 text-primary">
                      <Star size={12} className="fill-primary" />
                      {guideData.place.aiRating.toFixed(1)}
                    </div>
                  </div>
                  <div className="p-5">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <span className="text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full font-bold mb-1 inline-block">
                          {guideData.place.category}
                        </span>
                        <h4 className="font-bold text-lg text-gray-900">{guideData.place.title}</h4>
                      </div>
                      <ExternalLink size={18} className="text-gray-400" />
                    </div>
                    <div className="flex items-center gap-1 text-xs text-gray-500 mb-3">
                      <MapPin size={12} />
                      {guideData.place.address}
                    </div>
                    <p className="text-sm text-gray-600 line-clamp-2 leading-relaxed">
                      {guideData.place.overview || "설명이 없습니다."}
                    </p>
                    
                    {guideData.place.blogPositiveTags && (
                      <div className="flex flex-wrap gap-1.5 mt-4">
                        {guideData.place.blogPositiveTags.split(',').map(tag => (
                          <span key={tag} className="text-[11px] text-gray-500 bg-gray-100 px-2.5 py-1 rounded-lg font-medium">
                            #{tag.trim()}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Tips Section */}
            <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Heart className="text-primary" size={20} />
                오늘의 산책 팁
              </h3>
              <div className="space-y-2">
                {DEFAULT_TIPS.map((tip, idx) => (
                  <div key={idx} className="flex items-start gap-2 text-sm text-gray-600 leading-relaxed">
                    <span className="text-primary mt-0.5">•</span>
                    <span>{tip}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <button
                onClick={() => setShowRecommendation(false)}
                className="py-4 bg-gray-100 text-gray-700 font-bold rounded-2xl hover:bg-gray-200 transition-all"
              >
                조건 변경
              </button>
              <button
                onClick={handleSaveRoute}
                className="py-4 bg-primary text-white font-bold rounded-2xl hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all"
              >
                추천 장소 저장
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
          className="fixed bottom-[90px] left-1/2 -translate-x-1/2 bg-gray-900 text-white px-6 py-3 rounded-full shadow-2xl z-50 flex items-center gap-2 whitespace-nowrap"
        >
          <CheckCircle className="text-primary" size={20} />
          <span className="font-medium">마이페이지에 저장되었습니다.</span>
        </motion.div>
      )}
    </div>
  );
}