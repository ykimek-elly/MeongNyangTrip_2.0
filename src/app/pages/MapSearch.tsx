import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { placeApi } from '../api/placeApi';
import { PlaceDto } from '../api/types';
import { useGeolocation } from '../hooks/useGeolocation';
import { Map, CustomOverlayMap } from 'react-kakao-maps-sdk';
import { ArrowLeft, MapPin, Navigation, Star, X, PawPrint, Stethoscope, Phone, ExternalLink } from 'lucide-react';

// 카카오 로컬 API 응답 타입
interface KakaoVet {
  id: string;
  place_name: string;
  address_name: string;
  road_address_name: string;
  phone: string;
  x: string; // 경도
  y: string; // 위도
  place_url: string;
  distance: string; // 미터
}

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
  { id: 'PLACE', label: '#명소' },
  { id: 'STAY', label: '#숙박' },
  { id: 'DINING', label: '#맛집' },
  { id: '동물병원', label: '#동물병원' },
];

export function MapSearch({ onNavigate }: MapSearchProps) {
  const setUserLocation = useAppStore(state => state.setUserLocation);
  const { lat, lng, address, error, isLoading, getLocation } = useGeolocation();

  const [activeFilter, setActiveFilter] = useState('all');
  const [places, setPlaces] = useState<SpotType[]>([]);
  const [selectedPlace, setSelectedPlace] = useState<SpotType | null>(null);

  // 동물병원 전용 상태
  const [vetPlaces, setVetPlaces] = useState<KakaoVet[]>([]);
  const [selectedVet, setSelectedVet] = useState<KakaoVet | null>(null);
  const [vetLoading, setVetLoading] = useState(false);

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

  // 동물병원 필터 활성 시 카카오 로컬 API 호출
  React.useEffect(() => {
    if (activeFilter !== '동물병원') {
      setVetPlaces([]);
      setSelectedVet(null);
      return;
    }
    if (!lat || !lng) return;

    const REST_KEY = import.meta.env.VITE_KAKAO_REST_API_KEY;
    setVetLoading(true);

    fetch(
      `https://dapi.kakao.com/v2/local/search/keyword.json?query=동물병원&x=${lng}&y=${lat}&radius=3000&sort=distance&size=15`,
      { headers: { Authorization: `KakaoAK ${REST_KEY}` } }
    )
      .then(r => r.json())
      .then(data => setVetPlaces(data.documents ?? []))
      .catch(e => console.error('카카오 로컬 API 오류:', e))
      .finally(() => setVetLoading(false));
  }, [activeFilter, lat, lng]);

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
    : activeFilter === '동물병원'
      ? []
      : places.filter(s => s.category === activeFilter);

  return (
    <div className="relative w-full h-full bg-gray-100 overflow-hidden flex flex-col">
      {/* Kakao Map Area */}
      <div className="absolute inset-0 z-0">
        <Map
          center={{ lat: lat || 37.5665, lng: lng || 126.9780 }}   // 초기 중심좌표 (사용자 위치 없으면 서울시청)
          style={{ width: "100%", height: "100%" }}
          level={5} // 초기 확대 레벨
        >
          {/* 동물병원 마커 */}
          {vetPlaces.map((vet) => (
            <CustomOverlayMap
              key={`vet-${vet.id}`}
              position={{ lat: parseFloat(vet.y), lng: parseFloat(vet.x) }}
              clickable={true}
            >
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="cursor-pointer flex flex-col items-center"
                onClick={() => { setSelectedVet(vet); setSelectedPlace(null); }}
              >
                <div className={`relative p-2 rounded-full shadow-lg border-2 border-white transition-transform ${
                  selectedVet?.id === vet.id ? 'bg-blue-500 scale-110 z-20' : 'bg-white hover:bg-blue-50'
                }`}>
                  <Stethoscope size={18} className={selectedVet?.id === vet.id ? 'text-white' : 'text-blue-500'} />
                  <div className="absolute inset-0 rounded-full animate-ping bg-blue-400 opacity-20" />
                </div>
                <span className={`mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold shadow-sm backdrop-blur-sm max-w-[80px] truncate ${
                  selectedVet?.id === vet.id ? 'bg-blue-500 text-white' : 'bg-white/90 text-gray-800'
                }`}>
                  {vet.place_name}
                </span>
              </motion.div>
            </CustomOverlayMap>
          ))}

          {filteredSpots.map((spot) => (
            <CustomOverlayMap
              key={spot.id}
              position={{
                lat: spot.latitude,
                lng: spot.longitude,
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
                <div className={`relative p-2 rounded-full shadow-lg border-2 border-white transition-transform ${selectedPlace?.id === spot.id ? 'bg-primary scale-110 z-20' : 'bg-white text-primary hover:bg-gray-50'
                  }`}>
                  <PawPrint size={20} className={selectedPlace?.id === spot.id ? 'text-white' : 'text-primary'} fill={selectedPlace?.id === spot.id ? 'white' : 'currentColor'} />
                  {/* Ripple Effect */}
                  <div className="absolute inset-0 rounded-full animate-ping bg-primary opacity-20" />
                </div>
                <span className={`mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold shadow-sm backdrop-blur-sm transition-opacity ${selectedPlace?.id === spot.id ? 'bg-primary text-white' : 'bg-white/80 text-gray-800'
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
                setSelectedVet(null);
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shadow-sm transition-all ${activeFilter === filter.id
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
        className={`absolute right-4 bottom-[82px] z-10 bg-white p-3 rounded-full shadow-lg hover:text-primary active:scale-95 transition-all ${lat && lng ? 'text-primary' : 'text-gray-700'
          }`}
      >
        <Navigation size={24} className={isLoading ? "animate-spin" : ""} />
      </button>

      {/* 동물병원 로딩 인디케이터 */}
      {vetLoading && (
        <div className="absolute top-32 left-1/2 -translate-x-1/2 z-20 bg-white/90 backdrop-blur px-4 py-2 rounded-full shadow-md flex items-center gap-2 text-sm font-bold text-blue-500">
          <Stethoscope size={14} className="animate-pulse" />
          주변 동물병원 검색 중...
        </div>
      )}

      {/* 동물병원 위치 요청 안내 */}
      {activeFilter === '동물병원' && !lat && !vetLoading && (
        <div className="absolute top-32 left-1/2 -translate-x-1/2 z-20 bg-white/90 backdrop-blur px-4 py-2 rounded-full shadow-md flex items-center gap-2 text-sm font-bold text-gray-600">
          <MapPin size={14} className="text-primary" />
          위치 버튼을 눌러 주변 병원을 찾아보세요
        </div>
      )}

      {/* 동물병원 팝업 */}
      <AnimatePresence>
        {selectedVet && (
          <motion.div
            initial={{ y: '100%', opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: '100%', opacity: 0 }}
            className="absolute bottom-[60px] left-0 w-full z-20 p-4"
          >
            <div className="bg-white rounded-3xl shadow-[0_-5px_30px_rgba(0,0,0,0.1)] p-4 relative">
              <button
                onClick={() => setSelectedVet(null)}
                className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 z-10 p-1 bg-white/50 rounded-full"
              >
                <X size={20} />
              </button>

              <div className="flex items-start gap-3 mb-3">
                <div className="w-10 h-10 bg-blue-50 rounded-2xl flex items-center justify-center shrink-0">
                  <Stethoscope size={20} className="text-blue-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <span className="text-[10px] font-bold text-blue-500 bg-blue-50 px-2 py-0.5 rounded-full">#동물병원</span>
                  <h3 className="font-bold text-gray-900 text-base mt-0.5 truncate">{selectedVet.place_name}</h3>
                  <p className="text-xs text-gray-500 mt-0.5 truncate">
                    {selectedVet.road_address_name || selectedVet.address_name}
                  </p>
                  {selectedVet.phone && (
                    <a
                      href={`tel:${selectedVet.phone}`}
                      className="text-xs text-primary flex items-center gap-1 mt-0.5"
                    >
                      <Phone size={10} /> {selectedVet.phone}
                    </a>
                  )}
                </div>
                <span className="text-xs text-gray-400 shrink-0">
                  {Math.round(parseInt(selectedVet.distance))}m
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <a
                  href={selectedVet.place_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-yellow-400 text-white text-xs font-bold rounded-xl hover:bg-yellow-500 active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 카카오맵
                </a>
                <a
                  href={`https://map.naver.com/v5/search/${encodeURIComponent(selectedVet.place_name + ' ' + (selectedVet.road_address_name || selectedVet.address_name))}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-green-500 text-white text-xs font-bold rounded-xl hover:bg-green-600 active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 네이버맵
                </a>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Place Summary Card */}
      <AnimatePresence>
        {selectedPlace && (
          <motion.div
            initial={{ y: '100%', opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: '100%', opacity: 0 }}
            className="absolute bottom-[60px] left-0 w-full z-20 p-4"
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