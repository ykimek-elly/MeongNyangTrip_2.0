import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { placeApi } from '../api/placeApi';
import { PlaceDto } from '../api/types';
import { useGeolocation } from '../hooks/useGeolocation';
import { Map, CustomOverlayMap } from 'react-kakao-maps-sdk';
import { ArrowLeft, MapPin, Navigation, Star, Sun, Wind, Dog, X, PawPrint } from 'lucide-react';

const mapImage = 'https://images.unsplash.com/photo-1524661135-423995f22d0b?w=1200&q=80';

export interface MapSearchProps {
  onNavigate: (page: string, params?: any) => void;
}

// 확장된(Tag, Desc 포함) UI 사용용 임시 타입 단언
type SpotType = PlaceDto & { 
  tag?: string; 
  desc?: string; 
  rating?: number; 
  distance?: string;
  categoryIcon?: React.ElementType;
  categoryColor?: string;
  title?: string; // 하드코딩 호환용
  img?: string;   // 하드코딩 호환용
  name?: string;  // 하드코딩 호환용
};

const FILTERS = [
  { id: 'all', label: '전체' },
  { id: '햇살맛집', label: '#햇살맛집' },
  { id: '조용한산책', label: '#조용한산책' },
  { id: '뛰뛰가능', label: '#뛰뛰가능' },
];

export function MapSearch({ onNavigate }: MapSearchProps) {
  const setUserLocation = useAppStore(state => state.setUserLocation);
  const { lat, lng, address, error, isLoading, getLocation } = useGeolocation();
  
  const [activeFilter, setActiveFilter] = useState('all');
  const [places, setPlaces] = useState<SpotType[]>([]);
  const [selectedPlace, setSelectedPlace] = useState<SpotType | null>(null);
  const [isLocating, setIsLocating] = useState(false);

  const handleLocate = async () => {
    getLocation();
  };

  // 위치 정보 받아오면 상태 업데이트
  React.useEffect(() => {
    if (lat && lng) {
      setUserLocation({ lat, lng, address });
    }
  }, [lat, lng, address, setUserLocation]);

  // 에러 알림
  React.useEffect(() => {
    if (error) {
      alert(error);
    }
  }, [error]);

  // API를 통해 장소 데이터 받아오기 (Mock Interceptor가 가로챔)
  React.useEffect(() => {
    const fetchPlaces = async () => {
      try {
        const data = await placeApi.getPlaces();
        setPlaces(data as SpotType[]);
      } catch (error) {
        console.error('Failed to fetch places:', error);
      }
    };
    fetchPlaces();
  }, []);

  const filteredSpots = activeFilter === 'all' 
    ? places 
    : places.filter(s => s.tag === activeFilter);

  return (
    <div className="relative w-full h-screen bg-gray-100 overflow-hidden flex flex-col">
      {/* Kakao Map Area */}
      <div className="absolute inset-0 z-0">
        <Map
          center={{ lat: lat || 37.5665, lng: lng || 126.9780 }}   // 초기 중심좌표 (사용자 위치 없으면 서울시청)
          style={{ width: "100%", height: "100%" }}
          level={5} // 초기 확대 레벨
        >
          {filteredSpots.map((spot) => (
            // spot.desc 등에서 lat, lng 추출이 필요하나 현재 Mock Data 형태이므로 임의의 좌표 처리 (기존 % 대신 실제 좌표 매핑 필요)
            // 여기선 임시로 spot.id 값을 활용한 가벼운 오프셋 좌표로 보여줌
            <CustomOverlayMap
              key={spot.id}
              position={{ 
                lat: (lat || 37.5665) + (spot.id % 2 === 0 ? 0.002 : -0.002) + (spot.id * 0.0005), 
                lng: (lng || 126.9780) + (spot.id % 3 === 0 ? 0.002 : -0.002) - (spot.id * 0.0005)
              }}
              clickable={true}
            >
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0, opacity: 0 }}
                whileHover={{ scale: 1.1 }}
                className="cursor-pointer flex flex-col items-center"
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
                  {spot.name || spot.title}
                </span>
              </motion.div>
            </CustomOverlayMap>
          ))}
        </Map>
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
        className={`absolute right-4 bottom-24 z-10 bg-white p-3 rounded-full shadow-lg hover:text-primary active:scale-95 transition-all ${
          lat && lng ? 'text-primary' : 'text-gray-700'
        }`}
      >
        <Navigation size={24} className={isLoading ? "animate-spin" : ""} />
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
                  <img src={selectedPlace.imageUrl || selectedPlace.img} alt={selectedPlace.name || selectedPlace.title} className="w-full h-full object-cover" />
                </div>
                
                <div className="flex-1 min-w-0 pt-1">
                  <div className="flex items-start justify-between mb-1">
                    <div>
                      {selectedPlace.categoryIcon ? (
                        <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-1.5 py-0.5 rounded mb-1 ${selectedPlace.categoryColor}`}>
                          <selectedPlace.categoryIcon size={10} /> {selectedPlace.tag}
                        </span>
                      ) : (
                        <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-1.5 py-0.5 rounded mb-1 bg-gray-100 text-gray-600`}>
                          {selectedPlace.tag || '#추천'}
                        </span>
                      )}
                      <h3 className="font-bold text-gray-900 text-lg truncate">{selectedPlace.name || selectedPlace.title}</h3>
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