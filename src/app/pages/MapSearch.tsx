import React, { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { placeApi } from '../api/placeApi';
import { PlaceDto } from '../api/types';
import { useGeolocation } from '../hooks/useGeolocation';
import { Map, CustomOverlayMap } from 'react-kakao-maps-sdk';
import { ArrowLeft, MapPin, Navigation, Star, X, PawPrint, Stethoscope, Phone, ExternalLink, Search } from 'lucide-react';
import { PlaceImage } from '../components/PlaceImage';

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
  initialPlaceId?: number;
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

export function MapSearch({ onNavigate, initialPlaceId }: MapSearchProps) {
  const setUserLocation = useAppStore(state => state.setUserLocation);
  const { lat, lng, address, error, getLocation } = useGeolocation();

  const [activeFilter, setActiveFilter] = useState('all');
  const [places, setPlaces] = useState<SpotType[]>([]);
  const [selectedPlace, setSelectedPlace] = useState<SpotType | null>(null);
  const [mapLevel, setMapLevel] = useState(5);
  const [spinning, setSpinning] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const locatingRef = useRef(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // 동물병원 전용 상태
  const [vetPlaces, setVetPlaces] = useState<KakaoVet[]>([]);
  const [selectedVet, setSelectedVet] = useState<KakaoVet | null>(null);
  const [vetLoading, setVetLoading] = useState(false);
  const vetFetchedRef = useRef(false); // 중복 호출 방지

  const handleLocate = () => {
    setSpinning(true);
    locatingRef.current = true;
    getLocation();
    setTimeout(() => setSpinning(false), 600);
  };

  // 위치 정보 받아오면 상태 업데이트 + 버튼 클릭 시 지도 이동 & 줌 리셋
  React.useEffect(() => {
    if (lat && lng) {
      setUserLocation({ lat, lng, address });
      if (locatingRef.current && mapRef.current) {
        mapRef.current.setLevel(5);
        mapRef.current.setCenter(new window.kakao.maps.LatLng(lat, lng));
        setMapLevel(5);
        locatingRef.current = false;
      }
    }
  }, [lat, lng, address, setUserLocation]);

  // 에러 알림
  React.useEffect(() => {
    if (error) {
      alert(error);
    }
  }, [error]);

  // 1. 필터 변경 감지 - 동물병원 아닐 때 클리어
  React.useEffect(() => {
    if (activeFilter !== '동물병원') {
      setVetPlaces([]);
      setSelectedVet(null);
      vetFetchedRef.current = false;
    }
  }, [activeFilter]);

  // 2. 위치 확보 후 동물병원 탭이면 API 호출
  React.useEffect(() => {
    if (activeFilter !== '동물병원') return;
    if (!lat || !lng) return;
    if (vetFetchedRef.current) return;
    vetFetchedRef.current = true;

    const REST_KEY = import.meta.env.VITE_KAKAO_REST_API_KEY;
    setVetLoading(true);
    fetch(
      `https://dapi.kakao.com/v2/local/search/keyword.json?query=동물병원&x=${lng}&y=${lat}&radius=3000&sort=distance&size=15`,
      { headers: { Authorization: `KakaoAK ${REST_KEY}` } }
    )
      .then(r => r.json())
      .then(data => {
        console.log('[동물병원] Kakao API 응답:', data);
        if (data.errorType || data.code) {
          console.error('[동물병원] API 오류:', data.message || data.msg);
          return;
        }
        setVetPlaces(data.documents ?? []);
      })
      .catch(e => console.error('[동물병원] 오류:', e))
      .finally(() => setVetLoading(false));
  }, [lat, lng, activeFilter]);

  // 장소 데이터 조회: 위치 확보 시 근처 5km, 미확보 시 전체 목록
  React.useEffect(() => {
    const fetchPlaces = async () => {
      try {
        const data = lat && lng
          ? await placeApi.getPlaces(undefined, undefined, lat, lng, 5000)
          : await placeApi.getPlaces();
        setPlaces(data as SpotType[]);
      } catch (error) {
        console.error('Failed to fetch places:', error);
      }
    };
    fetchPlaces();
  }, [lat, lng]);

  // 길찾기 모드: 진입 시 현재 위치 자동 요청
  React.useEffect(() => {
    if (initialPlaceId) getLocation();
  }, []);

  // 장소 데이터 준비되면 자동 선택 (위치 없을 때는 목적지만 중앙)
  React.useEffect(() => {
    if (!initialPlaceId || places.length === 0) return;
    const target = places.find(p => p.id === initialPlaceId);
    if (!target) return;
    setSelectedPlace(target);
    if (!lat && mapRef.current) {
      mapRef.current.setLevel(4);
      mapRef.current.setCenter(new window.kakao.maps.LatLng(target.latitude, target.longitude));
      setMapLevel(4);
    }
  }, [initialPlaceId, places]);

  // 현재 위치 + 목적지 둘 다 준비되면 bounds fit
  React.useEffect(() => {
    if (!initialPlaceId || !lat || !lng || places.length === 0 || !mapRef.current) return;
    const target = places.find(p => p.id === initialPlaceId);
    if (!target) return;
    const bounds = new window.kakao.maps.LatLngBounds();
    bounds.extend(new window.kakao.maps.LatLng(lat, lng));
    bounds.extend(new window.kakao.maps.LatLng(target.latitude, target.longitude));
    // 하단 팝업 카드 공간 확보: bottom 300px 패딩
    mapRef.current.setBounds(bounds, 80, 40, 300, 40);
  }, [initialPlaceId, lat, lng, places]);

  const filteredSpots = activeFilter === 'all'
    ? places
    : activeFilter === '동물병원'
      ? []
      : places.filter(s => s.category === activeFilter);

  // ─── 검색 필터 함수 (드롭다운 + 지도 마커 공용) ───
  const CATEGORY_LABEL: Record<string, string> = { PLACE: '명소 관광지 공원 놀이터', STAY: '숙박 펜션 호텔 글램핑 독채', DINING: '맛집 카페 식당 애견카페 레스토랑' };
  const CATEGORY_COLOR: Record<string, { bg: string; bgSelected: string; text: string; dot: string; dotSelected: string; ping: string }> = {
    PLACE:  { bg: 'bg-white', bgSelected: 'bg-primary',    text: 'text-primary',    dot: 'bg-primary/70',    dotSelected: 'bg-primary',    ping: 'bg-primary' },
    STAY:   { bg: 'bg-white', bgSelected: 'bg-green-500',  text: 'text-green-500',  dot: 'bg-green-400/70',  dotSelected: 'bg-green-500',  ping: 'bg-green-500' },
    DINING: { bg: 'bg-white', bgSelected: 'bg-orange-500', text: 'text-orange-500', dot: 'bg-orange-400/70', dotSelected: 'bg-orange-500', ping: 'bg-orange-500' },
  };
  const getAreaAlias = (addr: string) => {
    const aliases: string[] = [];
    if (addr.includes('서울')) aliases.push('서울 서울시');
    if (addr.includes('경기')) aliases.push('경기 경기도');
    const cityMatch = addr.match(/([\uAC00-\uD7A3]{1,4})(시|군|구)/);
    if (cityMatch) aliases.push(cityMatch[1]);
    return aliases.join(' ');
  };
  const filterBySearch = (list: SpotType[], query: string) => {
    if (!query.trim()) return list;
    const keywords = query.toLowerCase().split(/\s+/).filter(Boolean);
    return list.filter(p => {
      const addr = p.address || '';
      const searchTarget = [
        p.name || '', p.title || '', addr,
        CATEGORY_LABEL[p.category] || '', p.tags || '',
        getAreaAlias(addr)
      ].join(' ').toLowerCase();
      return keywords.every(kw => searchTarget.includes(kw));
    });
  };

  // 지도에 표시할 마커: 카테고리 필터를 먼저 적용한 후, 검색어 필터링 수행
  const displaySpots = searchQuery.trim()
    ? filterBySearch(filteredSpots, searchQuery)
    : filteredSpots;

  // 검색 시 지도 bounds 자동 조절
  React.useEffect(() => {
    if (!searchQuery.trim() || !mapRef.current) return;

    // 유효한 한국 위경도(위도 30~43, 경도 124~132 이내)를 가진 장소만 필터링
    const matched = filterBySearch(filteredSpots, searchQuery).filter(
      p => p.latitude && p.longitude && p.latitude > 30 && p.longitude > 120
    );

    if (matched.length === 0) return;
    if (matched.length === 1) {
      mapRef.current.setLevel(4);
      mapRef.current.setCenter(new window.kakao.maps.LatLng(matched[0].latitude, matched[0].longitude));
      setMapLevel(4);
    } else {
      const bounds = new window.kakao.maps.LatLngBounds();
      matched.forEach(p => bounds.extend(new window.kakao.maps.LatLng(p.latitude, p.longitude)));
      mapRef.current.setBounds(bounds, 60, 60, 60, 60);
    }
  }, [searchQuery, activeFilter, filteredSpots]);

  return (
    <div className="relative w-full h-full bg-gray-100 overflow-hidden flex flex-col">
      {/* Kakao Map Area */}
      <div className="absolute inset-0 z-0">
        <Map
          center={{ lat: lat || 37.5665, lng: lng || 126.9780 }}
          style={{ width: "100%", height: "100%" }}
          level={5}
          onZoomChanged={(map) => setMapLevel(map.getLevel())}
          onCreate={(map) => { mapRef.current = map; }}
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
                {mapLevel > 5 ? (
                  // 축소 시: 점
                  <div className={`w-3 h-3 rounded-full border-2 border-white shadow-md ${selectedVet?.id === vet.id ? 'bg-blue-500 scale-125' : 'bg-blue-400'
                    }`} />
                ) : (
                  // 확대 시: 아이콘 + 상호명
                  <>
                    <div className={`relative p-2 rounded-full shadow-lg border-2 border-white transition-transform ${selectedVet?.id === vet.id ? 'bg-blue-500 scale-110 z-20' : 'bg-white hover:bg-blue-50'
                      }`}>
                      <Stethoscope size={18} className={selectedVet?.id === vet.id ? 'text-white' : 'text-blue-500'} />
                      {selectedVet?.id === vet.id && <div className="absolute inset-0 rounded-full animate-ping bg-blue-400 opacity-20" />}
                    </div>
                    <span className={`mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold shadow-sm backdrop-blur-sm max-w-[80px] truncate ${selectedVet?.id === vet.id ? 'bg-blue-500 text-white' : 'bg-white/90 text-gray-800'
                      }`}>
                      {vet.place_name}
                    </span>
                  </>
                )}
              </motion.div>
            </CustomOverlayMap>
          ))}

          {/* 현재 위치 마커 — 길찾기 모드에서만 표시 */}
          {initialPlaceId && lat && lng && (
            <CustomOverlayMap position={{ lat, lng }} clickable={false}>
              <div className="relative w-5 h-5 flex items-center justify-center">
                <div className="absolute inset-0 rounded-full bg-blue-400 animate-ping opacity-40" />
                <div className="w-5 h-5 bg-blue-500 rounded-full border-2 border-white shadow-lg z-10 flex items-center justify-center">
                  <div className="w-2 h-2 bg-white rounded-full" />
                </div>
              </div>
            </CustomOverlayMap>
          )}

          {displaySpots.map((spot) => (
            <CustomOverlayMap
              key={spot.id}
              position={{ lat: spot.latitude, lng: spot.longitude }}
              clickable={true}
            >
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0, opacity: 0 }}
                className="cursor-pointer flex flex-col items-center"
                onClick={() => setSelectedPlace(spot)}
              >
                {(() => {
                  const cc = CATEGORY_COLOR[spot.category] ?? CATEGORY_COLOR['PLACE'];
                  const isSelected = selectedPlace?.id === spot.id;
                  return mapLevel > 5 ? (
                    // 축소 시: 점
                    <div className={`w-4 h-4 rounded-full border-[3px] border-white shadow-[0_0_0_1.5px_rgba(0,0,0,0.15),0_2px_6px_rgba(0,0,0,0.25)] ${isSelected ? `${cc.dotSelected} scale-150` : cc.dot}`} />
                  ) : (
                    // 확대 시: 아이콘 + 상호명
                    <>
                      <div className={`relative p-2 rounded-full shadow-lg border-2 border-white transition-transform ${isSelected ? `${cc.bgSelected} scale-110 z-20` : `${cc.bg} hover:bg-gray-50`}`}>
                        <PawPrint size={20} className={isSelected ? 'text-white' : cc.text} fill={isSelected ? 'white' : 'currentColor'} />
                        {isSelected && <div className={`absolute inset-0 rounded-full animate-ping ${cc.ping} opacity-20`} />}
                      </div>
                      <span className={`mt-1 px-2 py-0.5 rounded-md text-[10px] font-bold shadow-sm backdrop-blur-sm ${isSelected ? `${cc.bgSelected} text-white` : 'bg-white/80 text-gray-800'}`}>
                        {spot.name || spot.title}
                      </span>
                    </>
                  );
                })()}
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
          <div className="relative flex-1" ref={searchRef}>
            <div className="bg-white/90 backdrop-blur pl-4 pr-1.5 py-1.5 rounded-full shadow-sm border border-white/50 flex items-center gap-2">
              <Search size={16} className="text-gray-400 shrink-0" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => { setSearchInput(e.target.value); setShowSuggestions(true); }}
                onFocus={() => searchInput && setShowSuggestions(true)}
                onKeyDown={(e) => {
                  if (e.nativeEvent.isComposing) return; // 한글 입력 조합 중복 방지
                  if (e.key === 'Enter') {
                    setSearchQuery(searchInput);
                    e.currentTarget.blur();
                    setShowSuggestions(false);
                  }
                }}
                placeholder="장소를 검색해보세요"
                className="bg-transparent text-sm text-gray-800 placeholder:text-gray-400 outline-none w-full"
              />
              {searchInput && (
                <button
                  onClick={() => { setSearchInput(''); setSearchQuery(''); setShowSuggestions(false); setSelectedPlace(null); }}
                  className="text-gray-400 hover:text-gray-600 mr-0.5"
                >
                  <X size={14} />
                </button>
              )}
              <button
                onClick={() => { setSearchQuery(searchInput); setShowSuggestions(false); }}
                className="bg-primary text-white font-bold text-[13px] whitespace-nowrap px-3.5 py-1.5 rounded-full shadow-sm active:scale-95 transition-all"
              >
                검색
              </button>
            </div>
            {/* 검색 자동완성 드롭다운 */}
            {showSuggestions && searchInput.length >= 1 && (() => {
              const filtered = filterBySearch(places, searchInput)
                .sort((a, b) => (b.rating || 0) - (a.rating || 0))
                .slice(0, 8);
              if (filtered.length === 0) return null;
              return (
                <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden z-30">
                  {filtered.map(place => (
                    <button
                      key={place.id}
                      onClick={() => {
                        setSelectedPlace(place);
                        setSearchInput(place.name || place.title || '');
                        setSearchQuery(place.name || place.title || '');
                        setShowSuggestions(false);
                        if (mapRef.current) {
                          mapRef.current.setLevel(3);
                          mapRef.current.setCenter(new window.kakao.maps.LatLng(place.latitude, place.longitude));
                          setMapLevel(3);
                        }
                      }}
                      className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition-colors text-left border-b border-gray-50 last:border-0"
                    >
                      <PawPrint size={14} className="text-primary shrink-0" />
                      <div className="min-w-0">
                        <p className="text-sm font-bold text-gray-800 truncate">{place.name || place.title}</p>
                        <p className="text-[11px] text-gray-400 truncate">{place.address}</p>
                      </div>
                    </button>
                  ))}
                </div>
              );
            })()}
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
                setSearchQuery('');
                setSearchInput('');
                setShowSuggestions(false);
                if (filter.id === '동물병원') {
                  vetFetchedRef.current = false; // 탭 클릭 시 강제 리셋
                  if (!lat) getLocation();
                }
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

      {/* 지도 어두운 오버레이 — 팝업 배너 활성 시 */}
      <AnimatePresence>
        {(selectedPlace || selectedVet) && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 z-10 bg-black/30 pointer-events-none"
          />
        )}
      </AnimatePresence>

      <motion.button
        onClick={handleLocate}
        animate={{ rotate: spinning ? 360 : 0 }}
        transition={{ duration: 0.55, ease: "easeInOut" }}
        className={`absolute right-4 bottom-[82px] z-10 bg-white p-3 rounded-full shadow-lg hover:text-primary active:scale-95 transition-colors ${lat && lng ? 'text-primary' : 'text-gray-700'}`}
      >
        <Navigation size={24} />
      </motion.button>

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
            className="absolute bottom-[0px] left-0 w-full z-20 p-4"
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
                  <div className="flex items-center gap-2 mt-0.5">
                    <h3 className="font-bold text-gray-900 text-base truncate">{selectedVet.place_name}</h3>
                    <span className="text-[11px] text-gray-400 shrink-0">{Math.round(parseInt(selectedVet.distance))}m</span>
                  </div>
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
              </div>

              <div className="grid grid-cols-2 gap-2">
                <a
                  href={lat && lng
                    ? `https://map.kakao.com/link/from/내위치,${lat},${lng}/to/${encodeURIComponent(selectedVet.place_name)},${parseFloat(selectedVet.y)},${parseFloat(selectedVet.x)}`
                    : selectedVet.place_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-yellow-400 text-gray-900 text-xs font-bold rounded-xl active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 카카오 길찾기
                </a>
                <a
                  href={lat && lng
                    ? `https://m.map.naver.com/route.nhn?menu=route&sname=내위치&sx=${lng}&sy=${lat}&ename=${encodeURIComponent(selectedVet.place_name)}&ex=${selectedVet.x}&ey=${selectedVet.y}&pathType=0&showMap=true`
                    : `https://map.naver.com/v5/search/${encodeURIComponent(selectedVet.place_name + ' ' + (selectedVet.road_address_name || selectedVet.address_name))}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-green-500 text-white text-xs font-bold rounded-xl active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 네이버 길찾기
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
            className="absolute bottom-[0px] left-0 w-full z-20 p-4"
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
                  <PlaceImage imageUrl={selectedPlace.imageUrl} category={selectedPlace.category} className="w-full h-full object-cover" iconSize={28} alt={selectedPlace.title} />
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
                      {(selectedPlace.reviewCount || 0) > 0 ? (
                        <span className="flex items-center gap-1 text-brand-point font-bold">
                          <Star size={14} className="fill-brand-point" /> {(selectedPlace.rating || 0).toFixed(1)}
                        </span>
                      ) : selectedPlace.aiRating ? (
                        <span className="flex items-center gap-1 text-gray-900 font-bold">
                          <Star size={14} className="fill-[#008BFF] text-[#008BFF]" /> {selectedPlace.aiRating.toFixed(1)}
                          <span className="text-[10px] font-normal text-gray-400 -ml-0.5">[AI]</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-brand-point font-bold">
                          <Star size={14} className="fill-brand-point" /> {(selectedPlace.rating || 0).toFixed(1)}
                        </span>
                      )}
                      <span className="text-gray-400">|</span>
                      <span className="text-primary flex items-center gap-1">
                        <MapPin size={12} /> {selectedPlace.distance}
                      </span>
                    </div>

                    <button
                      onClick={() => onNavigate('detail', { id: selectedPlace.id })}
                      className="bg-primary text-white text-xs font-bold px-3 py-1.5 rounded-full shadow-md active:scale-95 transition-transform"
                    >
                      상세보기
                    </button>
                  </div>
                </div>
              </div>

              {/* 길찾기 앱 연결 — 장소 선택 시 항상 표시 */}
              <div className="grid grid-cols-2 gap-2 mt-3 pt-3 border-t border-gray-100">
                <a
                  href={lat && lng
                    ? `https://map.kakao.com/link/from/내위치,${lat},${lng}/to/${encodeURIComponent(selectedPlace.name || selectedPlace.title || '')},${selectedPlace.latitude},${selectedPlace.longitude}`
                    : `https://map.kakao.com/link/to/${encodeURIComponent(selectedPlace.name || selectedPlace.title || '')},${selectedPlace.latitude},${selectedPlace.longitude}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-yellow-400 text-gray-900 text-xs font-bold rounded-xl active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 카카오 길찾기
                </a>
                <a
                  href={lat && lng
                    ? `https://m.map.naver.com/route.nhn?menu=route&sname=내위치&sx=${lng}&sy=${lat}&ename=${encodeURIComponent(selectedPlace.name || selectedPlace.title || '')}&ex=${selectedPlace.longitude}&ey=${selectedPlace.latitude}&pathType=0&showMap=true`
                    : `https://map.naver.com/v5/search/${encodeURIComponent(selectedPlace.name || selectedPlace.title || '')}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1.5 py-2.5 bg-green-500 text-white text-xs font-bold rounded-xl active:scale-95 transition-all"
                >
                  <ExternalLink size={13} /> 네이버 길찾기
                </a>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}