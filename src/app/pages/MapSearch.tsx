import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, MapPin, Navigation, Star, Sun, Wind, Dog, X, PawPrint } from 'lucide-react';
// 지도 배경 이미지 (Figma 에셋 대신 placeholder 사용)
const mapImage = 'https://images.unsplash.com/photo-1524661135-423995f22d0b?w=1200&q=80';

interface MapSearchProps {
  onNavigate: (page: string, params?: any) => void;
}

// Mock Data for "Emotional Spots"
const NEARBY_SPOTS = [
  {
    id: 2,
    title: '서울숲 반려동물 구역',
    tag: '햇살맛집',
    desc: '따스한 햇살이 가득한 반려동물 전용 구역',
    rating: 4.9,
    distance: '0.8km',
    x: '40%',
    y: '30%',
    lat: 37.5665,
    lng: 126.9780,
    img: 'https://images.unsplash.com/photo-1597633425046-08f5110420b5?w=600&q=80',
    categoryIcon: Sun,
    categoryColor: 'text-orange-500 bg-orange-50'
  },
  {
    id: 3,
    title: '남양주 물의정원',
    tag: '조용한산책',
    desc: '사람이 적고 한적한 힐링 스팟',
    rating: 4.8,
    distance: '1.2km',
    x: '70%',
    y: '60%',
    lat: 37.5650,
    lng: 126.9800,
    img: 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600&q=80',
    categoryIcon: Wind,
    categoryColor: 'text-green-600 bg-green-50'
  },
  {
    id: 4,
    title: '하늘공원 산책로',
    tag: '뛰뛰가능',
    desc: '목줄 없이 자유롭게 뛰노는 운동장',
    rating: 4.7,
    distance: '2.5km',
    x: '25%',
    y: '75%',
    lat: 37.5680,
    lng: 126.9750,
    img: 'https://images.unsplash.com/photo-1571570776991-b3b4d4982a0d?w=600&q=80',
    categoryIcon: Dog,
    categoryColor: 'text-blue-500 bg-blue-50'
  }
];

const FILTERS = [
  { id: 'all', label: '전체' },
  { id: '햇살맛집', label: '#햇살맛집' },
  { id: '조용한산책', label: '#조용한산책' },
  { id: '뛰뛰가능', label: '#뛰뛰가능' },
];

export function MapSearch({ onNavigate }: MapSearchProps) {
  const [activeFilter, setActiveFilter] = useState('all');
  const [selectedPlace, setSelectedPlace] = useState<typeof NEARBY_SPOTS[0] | null>(null);
  const [isLocating, setIsLocating] = useState(false);

  const handleLocate = () => {
    setIsLocating(true);
    setTimeout(() => {
      setIsLocating(false);
      alert('현재 위치를 갱신했습니다!');
    }, 1500);
  };

  const filteredSpots = activeFilter === 'all' 
    ? NEARBY_SPOTS 
    : NEARBY_SPOTS.filter(s => s.tag === activeFilter);

  return (
    <div className="relative w-full h-screen bg-gray-100 overflow-hidden flex flex-col">
      {/* Map Background */}
      {/* Fallback Image Map (Kakao Map SDK requires a real API key) */}
      <div 
        className="absolute inset-0 z-0 bg-cover bg-center opacity-90"
        style={{ 
          backgroundImage: `url('${mapImage}')`,
        }}
      />
      <div className="absolute inset-0 bg-primary/5 pointer-events-none" />
      
      {/* Map Markers */}
      <div className="absolute inset-0 z-0 pointer-events-none">
        {filteredSpots.map((spot) => (
          <motion.div
            key={spot.id}
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            whileHover={{ scale: 1.1 }}
            className="absolute cursor-pointer pointer-events-auto flex flex-col items-center"
            style={{ left: spot.x, top: spot.y }}
            onClick={() => setSelectedPlace(spot)}
          >
            <div className={`relative p-2 rounded-full shadow-lg border-2 border-white transition-transform ${
              selectedPlace?.id === spot.id ? 'bg-primary scale-110 z-20' : 'bg-white text-primary hover:bg-gray-50'
            }`}>
              <PawPrint size={20} className={selectedPlace?.id === spot.id ? 'text-white' : 'text-primary'} fill={selectedPlace?.id === spot.id ? 'white' : 'currentColor'} />
              {/* Ripple Effect */}
              <div className="absolute inset-0 rounded-full animate-ping bg-primary opacity-20" />
            </div>
            <span className={`mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold shadow-sm backdrop-blur-sm transition-opacity ${
              selectedPlace?.id === spot.id ? 'bg-primary text-white' : 'bg-white/80 text-gray-800'
            }`}>
              {spot.title}
            </span>
          </motion.div>
        ))}
      </div>

      {/* Top UI */}
      <div className="relative z-10 pt-4 px-4 pb-2">
        <div className="flex items-center gap-2 mb-4">
          <button 
            onClick={() => onNavigate('home')} 
            className="p-2 bg-white/90 backdrop-blur rounded-full shadow-sm hover:bg-white transition-colors"
          >
            <ArrowLeft size={20} className="text-gray-700" />
          </button>
          <div className="bg-white/90 backdrop-blur px-4 py-2 rounded-full shadow-sm flex-1 border border-white/50">
            <h1 className="text-sm font-bold text-gray-800">멍냥지도 <span className="text-primary font-normal text-xs ml-1">내 주변 감성 스팟</span></h1>
          </div>
        </div>

        {/* Filter Chips */}
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
          {FILTERS.map(filter => (
            <button
              key={filter.id}
              onClick={() => {
                setActiveFilter(filter.id);
                setSelectedPlace(null);
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shadow-sm transition-all ${
                activeFilter === filter.id
                  ? 'bg-primary text-white'
                  : 'bg-white/90 text-gray-600 hover:bg-white'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {/* Current Location Button */}
      <button 
        onClick={handleLocate}
        className="absolute right-4 bottom-24 z-10 bg-white p-3 rounded-full shadow-lg text-gray-700 hover:text-primary active:scale-95 transition-all"
      >
        <Navigation size={24} className={isLocating ? "animate-spin" : ""} />
      </button>

      {/* Place Summary Card */}
      <AnimatePresence>
        {selectedPlace && (
          <motion.div
            initial={{ y: '100%', opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: '100%', opacity: 0 }}
            className="absolute bottom-0 left-0 w-full z-20 p-4 pb-24" // pb-24 for bottom nav space
          >
            <div className="bg-white rounded-3xl shadow-[0_-5px_30px_rgba(0,0,0,0.1)] p-4 relative overflow-hidden">
              <button 
                onClick={() => setSelectedPlace(null)}
                className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 z-10 p-1 bg-white/50 rounded-full"
              >
                <X size={20} />
              </button>
              
              <div className="flex gap-4">
                <div className="w-24 h-24 rounded-2xl overflow-hidden flex-shrink-0 bg-gray-100">
                  <img src={selectedPlace.img} alt={selectedPlace.title} className="w-full h-full object-cover" />
                </div>
                
                <div className="flex-1 min-w-0 pt-1">
                  <div className="flex items-start justify-between mb-1">
                    <div>
                      <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-1.5 py-0.5 rounded mb-1 ${selectedPlace.categoryColor}`}>
                        <selectedPlace.categoryIcon size={10} /> {selectedPlace.tag}
                      </span>
                      <h3 className="font-bold text-gray-900 text-lg truncate">{selectedPlace.title}</h3>
                    </div>
                  </div>
                  
                  <p className="text-xs text-gray-500 mb-3 truncate">{selectedPlace.desc}</p>
                  
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 text-xs font-medium">
                      <span className="flex items-center gap-1 text-brand-point">
                        <Star size={14} className="fill-brand-point" /> {selectedPlace.rating}
                      </span>
                      <span className="text-gray-400">|</span>
                      <span className="text-primary flex items-center gap-1">
                        <MapPin size={12} /> {selectedPlace.distance}
                      </span>
                    </div>
                    
                    <button 
                      onClick={() => onNavigate('detail', { id: selectedPlace.id })} // Mock ID, usually needs real ID
                      className="bg-primary text-white text-xs font-bold px-3 py-1.5 rounded-full shadow-md active:scale-95 transition-transform"
                    >
                      상세보기
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}