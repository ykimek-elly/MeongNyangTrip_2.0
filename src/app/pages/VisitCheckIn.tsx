import { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft, Award, MapPin, Calendar, Camera, Check,
  Clock, TrendingUp, LocateFixed, ImagePlus, AlertCircle, Upload, Search, X, Sparkles,
} from 'lucide-react';
import { checkInApi } from '../api/checkInApi';
import type { CheckInStatsResponse } from '../api/checkInApi';

// ── Kakao Place 타입 ──────────────────────────────────────────────────────────
interface KakaoPlace {
  id: string;
  place_name: string;
  address_name: string;
  road_address_name?: string;
  category_group_name?: string;
  x: string; // 경도
  y: string; // 위도
}

const KAKAO_REST_KEY = import.meta.env.VITE_KAKAO_REST_API_KEY ?? '';

async function searchKakaoPlaces(query: string): Promise<KakaoPlace[]> {
  if (!query.trim()) return [];
  // REST API 키가 없으면 더미 데이터 반환 (개발용)
  if (!KAKAO_REST_KEY) {
    await new Promise((r) => setTimeout(r, 400));
    const MOCK: KakaoPlace[] = [
      { id: '1', place_name: '경복궁', address_name: '서울 종로구 사직로 161', category_group_name: '관광명소', x: '126.977', y: '37.579' },
      { id: '2', place_name: '해운대 해수욕장', address_name: '부산 해운대구 해운대해변로 264', category_group_name: '관광명소', x: '129.158', y: '35.158' },
      { id: '3', place_name: '성산일출봉', address_name: '제주 서귀포시 성산읍 일출로 284-12', category_group_name: '관광명소', x: '126.942', y: '33.458' },
      { id: '4', place_name: '북촌한옥마을', address_name: '서울 종로구 계동길 37', category_group_name: '관광명소', x: '126.985', y: '37.582' },
      { id: '5', place_name: '광안리 해수욕장', address_name: '부산 수영구 광안해변로 219', category_group_name: '관광명소', x: '129.118', y: '35.153' },
    ];
    return MOCK.filter((p) =>
      p.place_name.includes(query) || p.address_name.includes(query)
    ).slice(0, 5).concat(MOCK).slice(0, 5);
  }
  const res = await fetch(
    `https://dapi.kakao.com/v2/local/search/keyword.json?query=${encodeURIComponent(query)}&size=10`,
    { headers: { Authorization: `KakaoAK ${KAKAO_REST_KEY}` } }
  );
  if (!res.ok) throw new Error('Kakao API error');
  const data = await res.json();
  return data.documents ?? [];
}

interface VisitCheckInProps {
  onNavigate: (page: string, params?: any) => void;
}

type TabType = 'checkin' | 'photo' | 'history';

export function VisitCheckIn({ onNavigate }: VisitCheckInProps) {
  const [activeTab, setActiveTab] = useState<TabType>('checkin');

  // 현장 인증 상태
  const [photoTaken, setPhotoTaken] = useState(false);
  const [locationStatus, setLocationStatus] = useState<'idle' | 'loading' | 'found' | 'error'>('idle');
  const [locationName, setLocationName] = useState('');
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);

  // 사진 업로드 인증 상태
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [uploadedPreview, setUploadedPreview] = useState<string | null>(null);
  const [exifStatus, setExifStatus] = useState<'idle' | 'loading' | 'found' | 'error'>('idle');
  const [exifLocationName, setExifLocationName] = useState('');
  const [exifLat, setExifLat] = useState<number | null>(null);
  const [exifLng, setExifLng] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 장소 검색 상태 (GPS 없을 때)
  const [placeQuery, setPlaceQuery] = useState('');
  const [placeResults, setPlaceResults] = useState<KakaoPlace[]>([]);
  const [placeLoading, setPlaceLoading] = useState(false);
  const [placeError, setPlaceError] = useState(false);
  const [selectedPlace, setSelectedPlace] = useState<KakaoPlace | null>(null);
  const [showPlaceDropdown, setShowPlaceDropdown] = useState(false);
  const placeSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const placeWrapRef = useRef<HTMLDivElement>(null);

  // 공통 상태
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [newBadge, setNewBadge] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [stats, setStats] = useState<CheckInStatsResponse | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [successLocation, setSuccessLocation] = useState('');

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!placeWrapRef.current?.contains(e.target as Node)) setShowPlaceDropdown(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // 장소 검색 디바운스
  const handlePlaceInput = useCallback((q: string) => {
    setPlaceQuery(q);
    setSelectedPlace(null);
    setShowPlaceDropdown(true);
    if (placeSearchTimer.current) clearTimeout(placeSearchTimer.current);
    if (!q.trim()) { setPlaceResults([]); return; }
    placeSearchTimer.current = setTimeout(async () => {
      setPlaceLoading(true);
      setPlaceError(false);
      try {
        const results = await searchKakaoPlaces(q);
        setPlaceResults(results);
      } catch {
        setPlaceError(true);
        setPlaceResults([]);
      } finally {
        setPlaceLoading(false);
      }
    }, 350);
  }, []);

  const handleSelectPlace = (place: KakaoPlace) => {
    setSelectedPlace(place);
    setPlaceQuery(place.place_name);
    setShowPlaceDropdown(false);
  };

  const clearSelectedPlace = () => {
    setSelectedPlace(null);
    setPlaceQuery('');
    setPlaceResults([]);
  };

  useEffect(() => {
    checkInApi.getMyStats()
      .then(setStats)
      .catch(() => setStats(null))
      .finally(() => setIsLoadingStats(false));
  }, []);

  const totalVisits = stats?.totalVisits ?? 0;
  const unlockedBadges = stats?.unlockedBadges ?? 0;

  // 현장 위치 가져오기
  const handleGetLocation = () => {
    setLocationStatus('loading');
    navigator.geolocation?.getCurrentPosition(
      (pos) => {
        const { latitude: lat, longitude: lng } = pos.coords;
        setLatitude(lat);
        setLongitude(lng);
        setLocationStatus('found');
        try {
          const kakao = (window as any).kakao;
          if (kakao?.maps?.services) {
            const geocoder = new kakao.maps.services.Geocoder();
            geocoder.coord2Address(lng, lat, (result: any, status: any) => {
              if (status === kakao.maps.services.Status.OK && result[0]) {
                setLocationName(
                  result[0].road_address?.address_name ||
                  result[0].address?.address_name ||
                  '현재 위치'
                );
              } else {
                setLocationName('현재 위치');
              }
            });
          } else {
            setLocationName('현재 위치');
          }
        } catch {
          setLocationName('현재 위치');
        }
      },
      () => setLocationStatus('error')
    );
  };

  // EXIF GPS 추출
  const handleFileUpload = async (file: File) => {
    setUploadedFile(file);
    setExifStatus('loading');
    setExifLat(null);
    setExifLng(null);
    setExifLocationName('');
    // 장소 검색 초기화
    setSelectedPlace(null);
    setPlaceQuery('');
    setPlaceResults([]);

    // 미리보기
    const reader = new FileReader();
    reader.onload = (e) => setUploadedPreview(e.target?.result as string);
    reader.readAsDataURL(file);

    try {
      // exifr 동적 import
      const exifr = await import('exifr');
      const gps = await exifr.gps(file);

      if (gps && gps.latitude && gps.longitude) {
        setExifLat(gps.latitude);
        setExifLng(gps.longitude);

        // 카카오 역지오코딩
        try {
          const kakao = (window as any).kakao;
          if (kakao?.maps?.services) {
            const geocoder = new kakao.maps.services.Geocoder();
            geocoder.coord2Address(gps.longitude, gps.latitude, (result: any, status: any) => {
              if (status === kakao.maps.services.Status.OK && result[0]) {
                setExifLocationName(
                  result[0].road_address?.address_name ||
                  result[0].address?.address_name ||
                  '사진 촬영 위치'
                );
              } else {
                setExifLocationName('사진 촬영 위치');
              }
              setExifStatus('found');
            });
          } else {
            setExifLocationName('사진 촬영 위치');
            setExifStatus('found');
          }
        } catch {
          setExifLocationName('사진 촬영 위치');
          setExifStatus('found');
        }
      } else {
        setExifStatus('error');
      }
    } catch {
      setExifStatus('error');
    }
  };

  // 현장 인증 제출
  const handleCheckinSubmit = async () => {
    if (!locationName || latitude === null || longitude === null) return;
    setIsSubmitting(true);
    try {
      const result = await checkInApi.createCheckIn({ placeName: locationName, latitude, longitude });
      if (result.badgeName) setNewBadge(result.badgeName);
      const updated = await checkInApi.getMyStats();
      setStats(updated);
      setSuccessLocation(locationName);
      setShowSuccessModal(true);
      setTimeout(() => {
        setShowSuccessModal(false);
        setPhotoTaken(false);
        setLocationStatus('idle');
        setLocationName('');
        setLatitude(null);
        setLongitude(null);
        setNewBadge(null);
      }, 2500);
    } catch {
      alert('인증에 실패했어요. 로그인 상태를 확인해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 사진 업로드 인증 제출
  const handlePhotoSubmit = async () => {
    // GPS 자동 인식 또는 수동 선택된 장소 중 하나 필요
    const placeName = exifStatus === 'found' ? exifLocationName : selectedPlace?.place_name ?? '';
    const lat = exifStatus === 'found' ? exifLat : selectedPlace ? parseFloat(selectedPlace.y) : null;
    const lng = exifStatus === 'found' ? exifLng : selectedPlace ? parseFloat(selectedPlace.x) : null;
    if (!placeName || lat === null || lng === null) return;
    setIsSubmitting(true);
    try {
      const result = await checkInApi.createCheckIn({ placeName, latitude: lat, longitude: lng });
      if (result.badgeName) setNewBadge(result.badgeName);
      const updated = await checkInApi.getMyStats();
      setStats(updated);
      setSuccessLocation(placeName);
      setShowSuccessModal(true);
      setTimeout(() => {
        setShowSuccessModal(false);
        setUploadedFile(null);
        setUploadedPreview(null);
        setExifStatus('idle');
        setExifLocationName('');
        setExifLat(null);
        setExifLng(null);
        setSelectedPlace(null);
        setPlaceQuery('');
        setNewBadge(null);
      }, 2500);
    } catch {
      alert('인증에 실패했어요. 로그인 상태를 확인해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const canCheckinSubmit = photoTaken && locationStatus === 'found' && !isSubmitting;
  const canPhotoSubmit = (exifStatus === 'found' || (exifStatus === 'error' && !!selectedPlace)) && !isSubmitting;

  const tabs = [
    { id: 'checkin', label: '현장 인증', icon: LocateFixed },
    { id: 'photo', label: '사진 인증', icon: Upload },
    { id: 'history', label: '방문기록', icon: Clock },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary/5 to-white pb-24">
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
          className="bg-gradient-to-br from-primary to-secondary rounded-3xl p-5 text-white shadow-md"
        >
          {isLoadingStats ? (
            <div className="text-center text-white/80 py-2 text-sm">불러오는 중...</div>
          ) : (
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
          )}
        </motion.div>
      </div>

      {/* 탭 */}
      <div className="sticky top-14 z-40 bg-white/95 backdrop-blur-sm border-b border-gray-100 px-5 flex gap-1">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as TabType)}
            className={`flex-1 py-3 text-xs font-bold transition-colors relative flex items-center justify-center gap-1 ${
              activeTab === tab.id ? 'text-primary' : 'text-gray-400'
            }`}
          >
            <tab.icon size={14} />
            {tab.label}
            {activeTab === tab.id && (
              <motion.div layoutId="tab-indicator" className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
            )}
          </button>
        ))}
      </div>

      <div className="px-5 py-5">
        <AnimatePresence mode="wait">

          {/* 현장 인증 탭 */}
          {activeTab === 'checkin' && (
            <motion.div
              key="checkin"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-5"
            >
              <div className="bg-blue-50 border border-blue-100 rounded-2xl px-4 py-3">
                <p className="text-xs text-blue-600 font-medium">📍 현재 위치를 확인하고 사진을 찍어 인증해요</p>
              </div>

              {/* 위치 확인 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">1</span>
                  현재 위치 확인
                </h3>
                {locationStatus === 'idle' && (
                  <button onClick={handleGetLocation} className="w-full py-3.5 rounded-2xl border-2 border-primary/30 text-primary font-bold text-sm flex items-center justify-center gap-2 hover:bg-primary/5 transition-colors">
                    <LocateFixed size={18} /> 위치 가져오기
                  </button>
                )}
                {locationStatus === 'loading' && (
                  <div className="w-full py-3.5 rounded-2xl bg-gray-50 text-gray-500 text-sm flex items-center justify-center gap-2">
                    <LocateFixed size={18} className="animate-pulse text-primary" /> 위치를 확인하는 중...
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
                      <AlertCircle size={16} className="shrink-0" /> 위치를 가져올 수 없어요. 권한을 확인해주세요.
                    </div>
                    <button onClick={handleGetLocation} className="w-full py-3 rounded-2xl border border-gray-200 text-gray-600 text-sm font-bold hover:bg-gray-50 transition-colors">
                      다시 시도
                    </button>
                  </div>
                )}
              </div>

              {/* 사진 촬영 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">2</span>
                  방문 사진 촬영
                </h3>
                <button
                  onClick={() => setPhotoTaken(prev => !prev)}
                  className={`w-full h-36 rounded-2xl border-2 border-dashed flex flex-col items-center justify-center gap-2 transition-all ${
                    photoTaken ? 'border-green-400 bg-green-50' : 'border-gray-200 bg-gray-50 hover:border-primary/50 hover:bg-primary/5'
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

              <button
                onClick={handleCheckinSubmit}
                disabled={!canCheckinSubmit}
                className={`w-full py-4 rounded-2xl font-bold text-base transition-all ${
                  canCheckinSubmit ? 'bg-primary text-white shadow-md active:scale-[0.98]' : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                {isSubmitting ? '저장 중...' : '방문 인증 완료'}
              </button>

              {stats && stats.badges.filter(b => b.unlocked).length > 0 && (
                <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                    <Award className="text-primary" size={20} /> 획득한 뱃지
                  </h3>
                  <div className="grid grid-cols-3 gap-3">
                    {stats.badges.filter(b => b.unlocked).map((badge) => (
                      <div key={badge.id} className="bg-gradient-to-br from-primary/10 to-secondary/10 rounded-2xl p-4 text-center border border-primary/20">
                        <div className="text-3xl mb-2">{badge.icon}</div>
                        <div className="text-xs font-bold text-gray-800">{badge.name}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          )}

          {/* 사진 업로드 인증 탭 */}
          {activeTab === 'photo' && (
            <motion.div
              key="photo"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-5"
            >
              <div className="bg-amber-50 border border-amber-100 rounded-2xl px-4 py-3">
                <p className="text-xs text-amber-700 font-medium">🖼️ GPS 정보가 담긴 사진을 업로드하면 위치를 자동으로 인식해요</p>
              </div>

              {/* 파일 업로드 */}
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">1</span>
                  사진 업로드
                </h3>

                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleFileUpload(file);
                  }}
                />

                {!uploadedPreview ? (
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full h-40 rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50 hover:border-primary/50 hover:bg-primary/5 flex flex-col items-center justify-center gap-2 transition-all"
                  >
                    <div className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center">
                      <Upload size={22} className="text-primary" />
                    </div>
                    <span className="text-sm font-bold text-gray-700">사진 선택하기</span>
                    <span className="text-xs text-gray-400">GPS 정보가 있는 사진을 선택해주세요</span>
                  </button>
                ) : (
                  <div className="relative">
                    <img
                      src={uploadedPreview}
                      alt="업로드된 사진"
                      className="w-full h-48 object-cover rounded-2xl"
                    />
                    <button
                      onClick={() => {
                        setUploadedFile(null);
                        setUploadedPreview(null);
                        setExifStatus('idle');
                        setExifLocationName('');
                        setExifLat(null);
                        setExifLng(null);
                        if (fileInputRef.current) fileInputRef.current.value = '';
                      }}
                      className="absolute top-2 right-2 bg-black/50 text-white rounded-full w-7 h-7 flex items-center justify-center text-xs font-bold hover:bg-black/70 transition-colors"
                    >
                      ✕
                    </button>
                  </div>
                )}
              </div>

              {/* EXIF 위치 결과 */}
              {exifStatus !== 'idle' && (
                <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3 flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold shrink-0">2</span>
                    방문지역
                  </h3>

                  {exifStatus === 'loading' && (
                    <div className="w-full py-3.5 rounded-2xl bg-gray-50 text-gray-500 text-sm flex items-center justify-center gap-2">
                      <MapPin size={18} className="animate-pulse text-primary" /> 위치 정보를 읽는 중...
                    </div>
                  )}

                  {exifStatus === 'found' && (
                    <div className="w-full py-3.5 px-4 rounded-2xl bg-green-50 border border-green-200 flex items-center gap-3">
                      <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center shrink-0">
                        <MapPin size={16} className="text-green-600" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-xs text-green-600 font-bold">위치 정보 자동 인식</div>
                        <div className="text-sm font-bold text-gray-800 truncate">{exifLocationName}</div>
                        <div className="text-xs text-gray-400 mt-0.5">
                          {exifLat?.toFixed(5)}, {exifLng?.toFixed(5)}
                        </div>
                      </div>
                      <Check size={18} className="text-green-600 shrink-0" />
                    </div>
                  )}

                  {exifStatus === 'error' && (
                    <div className="space-y-3">
                      {/* 경고 배너 */}
                      <div className="w-full py-3 px-4 rounded-2xl bg-orange-50 border border-orange-100 flex items-start gap-2 text-sm text-orange-700">
                        <AlertCircle size={16} className="shrink-0 mt-0.5" />
                        <div>
                          <div className="font-bold">GPS 정보가 없어요</div>
                          <div className="text-xs mt-0.5 text-orange-500">방문한 장소를 직접 검색해서 선택해주세요</div>
                        </div>
                      </div>

                      {/* 장소 검색창 */}
                      <div ref={placeWrapRef} className="relative">
                        <div className={`flex items-center gap-2 border-2 rounded-2xl px-3 py-3 transition-colors ${selectedPlace ? 'border-primary bg-primary/5' : 'border-gray-200 bg-white focus-within:border-primary/50'}`}>
                          <Search size={16} className="text-gray-400 shrink-0" />

                          {selectedPlace ? (
                            /* 선택된 장소 칩 */
                            <div className="flex items-center gap-2 flex-1 min-w-0">
                              <div className="w-2 h-2 rounded-full bg-primary shrink-0" />
                              <span className="text-sm font-bold text-gray-900 shrink-0">{selectedPlace.place_name}</span>
                              <span className="text-xs text-gray-400 truncate">{selectedPlace.address_name}</span>
                              <button
                                onClick={clearSelectedPlace}
                                className="ml-auto w-5 h-5 rounded-full bg-gray-200 flex items-center justify-center shrink-0 hover:bg-gray-300 transition-colors"
                              >
                                <X size={10} className="text-gray-600" />
                              </button>
                            </div>
                          ) : (
                            <input
                              className="flex-1 text-sm outline-none bg-transparent placeholder-gray-400"
                              placeholder="예) 경복궁, 해운대 해수욕장, 한라산..."
                              value={placeQuery}
                              onChange={(e) => handlePlaceInput(e.target.value)}
                              onFocus={() => setShowPlaceDropdown(true)}
                              autoComplete="off"
                            />
                          )}
                          {placeLoading && <div className="w-4 h-4 border-2 border-primary/30 border-t-primary rounded-full animate-spin shrink-0" />}
                        </div>

                        {/* 검색 드롭다운 */}
                        {showPlaceDropdown && !selectedPlace && (
                          <div className="absolute top-full mt-1 left-0 right-0 z-50 bg-white border border-gray-200 rounded-2xl shadow-xl overflow-hidden max-h-60 overflow-y-auto">
                            {placeError && (
                              <div className="flex items-center gap-2 px-4 py-3 text-sm text-red-500">
                                <AlertCircle size={14} /> 검색 중 오류가 발생했어요
                              </div>
                            )}
                            {!placeError && placeQuery && !placeLoading && placeResults.length === 0 && (
                              <div className="px-4 py-3 text-sm text-gray-400">'{placeQuery}'에 대한 결과가 없어요</div>
                            )}
                            {placeResults.map((place) => (
                              <button
                                key={place.id}
                                onClick={() => handleSelectPlace(place)}
                                className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0 text-left"
                              >
                                <div className="w-8 h-8 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                                  <MapPin size={14} className="text-primary" />
                                </div>
                                <div className="flex-1 min-w-0">
                                  <div className="text-sm font-bold text-gray-900">{place.place_name}</div>
                                  <div className="text-xs text-gray-400 truncate">{place.address_name}</div>
                                  {place.category_group_name && (
                                    <span className="inline-block mt-1 text-[10px] font-bold px-1.5 py-0.5 rounded bg-primary/10 text-primary">{place.category_group_name}</span>
                                  )}
                                </div>
                              </button>
                            ))}
                            {!placeQuery && (
                              <div className="px-4 py-3">
                                <div className="text-xs font-bold text-gray-400 mb-2">💡 이렇게 검색해보세요</div>
                                {['카페, 식당, 공원 이름으로', '관광지, 해수욕장 이름으로', '지역명 + 장소 이름으로'].map((t) => (
                                  <div key={t} className="text-xs text-gray-300 leading-relaxed">· {t}</div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>

                      {/* 선택 완료 카드 */}
                      {selectedPlace && (
                        <div className="w-full py-3 px-4 rounded-2xl bg-primary/5 border border-primary/20 flex items-center gap-3">
                          <div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center shrink-0">
                            <MapPin size={16} className="text-primary" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="text-xs text-primary font-bold">방문지 선택 완료</div>
                            <div className="text-sm font-bold text-gray-800 truncate">{selectedPlace.place_name}</div>
                            <div className="text-xs text-gray-400 truncate">{selectedPlace.address_name}</div>
                          </div>
                          <Check size={18} className="text-primary shrink-0" />
                        </div>
                      )}

                      <button
                        onClick={() => fileInputRef.current?.click()}
                        className="w-full py-3 rounded-2xl border border-gray-200 text-gray-600 text-sm font-bold hover:bg-gray-50 transition-colors"
                      >
                        다른 사진 선택
                      </button>
                    </div>
                  )}
                </div>
              )}

              <button
                onClick={handlePhotoSubmit}
                disabled={!canPhotoSubmit}
                className={`w-full py-4 rounded-2xl font-bold text-base transition-all ${
                  canPhotoSubmit ? 'bg-primary text-white shadow-lg active:scale-[0.98]' : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                {isSubmitting ? '저장 중...' : '방문 인증 완료'}
              </button>
            </motion.div>
          )}

          {/* 방문기록 탭 */}
          {activeTab === 'history' && (
            <motion.div
              key="history"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-4"
            >
              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">이번 달 통계</h3>
                <div className="bg-blue-50 rounded-xl p-3">
                  <div className="text-xs text-blue-600 mb-1">방문 횟수</div>
                  <div className="text-2xl font-bold text-blue-700">{stats?.thisMonthVisits ?? 0}</div>
                  <div className="text-xs text-blue-600 mt-1 flex items-center gap-1">
                    <TrendingUp size={10} /> 이번 달 방문
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                <h3 className="font-bold text-gray-800 mb-3">최근 방문</h3>
                {isLoadingStats ? (
                  <div className="text-center text-gray-400 py-4">불러오는 중...</div>
                ) : !stats?.recentHistory.length ? (
                  <div className="text-center text-gray-400 py-4">아직 방문 기록이 없어요</div>
                ) : (
                  <div className="space-y-3">
                    {stats.recentHistory.map((visit) => (
                      <div key={visit.checkinId} className="flex gap-3 p-3 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors">
                        <div className="w-16 h-16 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                          <MapPin className="text-primary" size={24} />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-bold text-sm text-gray-900 truncate">{visit.placeName}</div>
                          <div className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                            <Calendar size={10} />
                            {new Date(visit.checkedInAt).toLocaleDateString('ko-KR')}
                          </div>
                          {visit.badgeName && (
                            <div className="mt-1.5 text-xs flex items-center gap-1"><Award size={12} className="text-brand-point" /> {visit.badgeName}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {stats && (
                <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3">전체 뱃지 ({stats.unlockedBadges}/{stats.badges.length})</h3>
                  <div className="grid grid-cols-3 gap-3">
                    {stats.badges.map((badge) => (
                      <div key={badge.id} className={`rounded-2xl p-4 text-center border-2 ${
                        badge.unlocked
                          ? 'bg-gradient-to-br from-primary/10 to-secondary/10 border-primary/20'
                          : 'bg-gray-50 border-gray-100 opacity-40'
                      }`}>
                        <div className="text-3xl mb-2">{badge.icon}</div>
                        <div className="text-xs font-bold text-gray-800 mb-1">{badge.name}</div>
                        <div className="text-[10px] text-gray-500 leading-tight">{badge.description}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* 성공 모달 */}
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
              <p className="text-gray-500 mb-4">{successLocation}</p>
              <div className="bg-gradient-to-br from-primary to-secondary text-white rounded-2xl p-5">
                <div className="text-sm font-bold mb-1">
                  <span className="flex items-center gap-1.5"><Sparkles size={14} /> {newBadge ? `"${newBadge}" 뱃지 획득!` : '방문이 기록되었어요!'}</span>
                </div>
                <div className="text-xs opacity-80">위치 정보가 저장됐어요</div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}