import React from 'react';
import { Leaf, Navigation, Calendar, Clock, MapPin, Trash2, Heart, Award, Shield, Image as ImageIcon, MessageCircle, Send, AlertTriangle, EyeOff, ChevronRight, PawPrint, Pencil, Plus, Star, Smile, Meh, Frown, Dog, Cat } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { useFeedStore } from '../store/useFeedStore';
import { useDMStore } from '../store/useDMStore';
import { PetProfileForm } from '../components/PetProfileForm';
import { PlaceImage } from '../components/PlaceImage';
import type { PetInfo } from '../store/useAppStore';
import { AnimatePresence } from 'motion/react';
import { motion } from 'motion/react';
import { checkInApi } from '../api/checkInApi';
import { authApi } from '../api/authApi';
import regionCoordinates from '../../../exports/region-coordinates.json';

const SIDO_LIST = Object.keys(regionCoordinates) as (keyof typeof regionCoordinates)[];
const DEFAULT_LAT = 37.5172;
const DEFAULT_LNG = 127.0473;

interface MyPageProps {
  onNavigate: (page: string, params?: any) => void;
}

export function MyPage({ onNavigate }: MyPageProps) {
  const { wishlist, savedRoutes, removeSavedRoute, pets, addPet, updatePet, removePet, setRepresentativePet, isAdmin, username, places, fetchPlaces, userRegionSido, userRegionDistrict, userActivityRadius, setUserRegion } = useAppStore();
  const { posts } = useFeedStore();
  const { conversations, getUnreadTotal } = useDMStore();

  const [showPetForm, setShowPetForm] = React.useState(false);
  const [editingIndex, setEditingIndex] = React.useState<number | null>(null);
  const [deleteTargetIndex, setDeleteTargetIndex] = React.useState<number | null>(null);
  const [petMood, setPetMood] = React.useState<'good' | 'normal' | 'bad' | null>(null);

  // 활동 지역 & 반경 편집 (store 초기값 반영)
  const [locationSido, setLocationSido] = React.useState(userRegionSido);
  const [locationDistrict, setLocationDistrict] = React.useState(userRegionDistrict);
  const [locationRadius, setLocationRadius] = React.useState<5 | 15 | 30>(userActivityRadius);
  const [isSavingLocation, setIsSavingLocation] = React.useState(false);
  const [locationSaved, setLocationSaved] = React.useState(false);

  const locationDistricts = locationSido
    ? Object.keys((regionCoordinates as Record<string, Record<string, { lat: number; lng: number }>>)[locationSido] ?? {})
    : [];
  const locationCoords = locationSido && locationDistrict
    ? (regionCoordinates as Record<string, Record<string, { lat: number; lng: number }>>)[locationSido]?.[locationDistrict]
    : null;

  const handleSaveLocation = async () => {
    setIsSavingLocation(true);
    setLocationSaved(false);
    try {
      const regionText = locationSido && locationDistrict
        ? `${locationSido} ${locationDistrict}`
        : locationSido || '';
      await authApi.saveLocation(
        locationCoords?.lat ?? DEFAULT_LAT,
        locationCoords?.lng ?? DEFAULT_LNG,
        locationRadius,
        regionText
      );
      setUserRegion(locationSido, locationDistrict, locationRadius);
      setLocationSaved(true);
      setTimeout(() => setLocationSaved(false), 2000);
    } catch {
      // 실패 시 조용히 처리
    } finally {
      setIsSavingLocation(false);
    }
  };
const [totalVisits, setTotalVisits] = React.useState(0);

React.useEffect(() => {
  checkInApi.getMyStats()
    .then(stats => setTotalVisits(stats.totalVisits))
    .catch(() => setTotalVisits(0));
}, []);
  React.useEffect(() => { if (places.length === 0) fetchPlaces(); }, []);
  const wishItems = places.filter(p => wishlist.includes(p.id));

  // 나의 활동 통계 (일반 유저용)
  const myTotalLikes    = posts.reduce((acc, p) => acc + p.likes, 0);
  const myTotalComments = posts.reduce((acc, p) => acc + p.comments, 0);
  const dmUnreadCount   = getUnreadTotal();
  const dmTotalCount    = conversations.length;

  const myStats = [
    { label: '게시글',  value: posts.length,    icon: ImageIcon,    color: 'bg-blue-50 text-blue-600',   page: null          },
    { label: '받은 좋아요', value: myTotalLikes,   icon: Heart,        color: 'bg-pink-50 text-primary',    page: null          },
    { label: '댓글',   value: myTotalComments, icon: MessageCircle, color: 'bg-green-50 text-green-600', page: null          },
    { label: 'DM',     value: dmTotalCount,    icon: Send,         color: 'bg-purple-50 text-purple-600', page: 'dm', unread: dmUnreadCount },
  ];

  const handlePetSubmit = (petData: PetInfo) => {
    if (editingIndex !== null) {
      updatePet(editingIndex, petData);
    } else {
      addPet(petData);
    }
    setShowPetForm(false);
    setEditingIndex(null);
  };

  const openEdit = (index: number) => {
    setEditingIndex(index);
    setShowPetForm(true);
  };

  const openAdd = () => {
    setEditingIndex(null);
    setShowPetForm(true);
  };

  const confirmDelete = (index: number) => {
    setDeleteTargetIndex(index);
  };

  const executeDelete = () => {
    if (deleteTargetIndex !== null) {
      removePet(deleteTargetIndex);
      setDeleteTargetIndex(null);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', damping: 28, stiffness: 320 }}
      className="bg-gray-50"
    >
      {/* Profile Header */}
      <div className="bg-white p-6 pb-8 rounded-b-[40px] shadow-[0_4px_24px_rgba(227,99,148,0.08)] mb-6">

        {/* 반려동물 섹션 헤더 */}
        <div className="flex items-center justify-between mb-4 mt-4">
          <div className="flex items-center gap-2">
            <PawPrint size={18} className="text-primary" />
            <h4 className="font-bold text-gray-800">내 반려동물</h4>
            {pets.length > 0 && (
              <span className="text-xs font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                {pets.length}마리
              </span>
            )}
          </div>
          <button
            onClick={openAdd}
            className="flex items-center gap-1 px-3 py-1.5 bg-primary text-white text-xs font-bold rounded-full shadow-md active:scale-[0.97] transition-spring"
          >
            <Plus size={12} /> 추가
          </button>
        </div>

        {/* 펫 카드 목록 */}
        {pets.length === 0 ? (
          <div className="flex items-center gap-4 py-4 px-2 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
            <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center shrink-0">
              <PawPrint size={24} className="text-gray-300" />
            </div>
            <div>
              <p className="text-sm font-bold text-gray-500">반려동물을 등록해주세요</p>
              <p className="text-xs text-gray-400 mt-0.5">등록하면 맞춤 알림을 받을 수 있어요!</p>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {pets.map((pet, index) => (
              <motion.div
                key={index}
                layout
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`relative flex items-center gap-3 p-3.5 rounded-2xl border-2 transition-spring ${
                  pet.isRepresentative
                    ? 'border-primary bg-primary/5'
                    : 'border-gray-100 bg-white'
                }`}
              >
                {/* 대표 뱃지 */}
                {pet.isRepresentative && (
                  <div className="absolute -top-2 left-3 flex items-center gap-1 bg-primary text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm">
                    <Star size={9} className="fill-white" /> 대표
                  </div>
                )}

                {/* 아바타 */}
                <div className={`w-14 h-14 rounded-full flex items-center justify-center text-2xl shrink-0 ${
                  pet.isRepresentative ? 'bg-primary/15' : 'bg-gray-100'
                }`}>
                  {pet.type === '강아지' ? <Dog size={18} className="text-primary" /> : <Cat size={18} className="text-primary" />}
                </div>

                {/* 정보 */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <span className="font-bold text-gray-900">{pet.name}</span>
                  </div>
                  <p className="text-xs text-gray-500">
                    {pet.breed} · {pet.age}살 · {pet.gender}
                  </p>
                  <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                    <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">
                      {pet.size === 'SMALL' ? '소형' : pet.size === 'MEDIUM' ? '중형' : '대형'}
                    </span>
                    <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">
                      활동량 {pet.activity === 'LOW' ? '적음' : pet.activity === 'NORMAL' ? '보통' : '많음'}
                    </span>
                    {pet.weight && (
                      <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">
                        {pet.weight}kg
                      </span>
                    )}
                  </div>
                </div>

                {/* 액션 버튼 */}
                <div className="flex flex-col gap-1.5 shrink-0">
                  {/* 대표 설정 버튼 */}
                  {!pet.isRepresentative && (
                    <button
                      onClick={() => setRepresentativePet(index)}
                      className="flex items-center gap-1 text-[10px] font-bold text-gray-500 bg-gray-100 hover:bg-primary/10 hover:text-primary px-2 py-1 rounded-lg transition-spring"
                    >
                      <Star size={10} /> 대표설정
                    </button>
                  )}
                  <div className="flex gap-1">
                    <button
                      onClick={() => openEdit(index)}
                      className="p-2 text-gray-400 hover:text-primary bg-gray-50 hover:bg-primary/5 rounded-lg transition-spring"
                    >
                      <Pencil size={13} />
                    </button>
                    <button
                      onClick={() => confirmDelete(index)}
                      className="p-2 text-gray-400 hover:text-destructive bg-gray-50 hover:bg-red-50 rounded-lg transition-spring"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}

            {/* 알림 안내 */}
            <p className="text-[11px] text-gray-400 text-center mt-1">
              ★ 대표 동물로 설정된 반려동물 기준으로 알림이 발송됩니다
            </p>
          </div>
        )}


        <div className="grid grid-cols-2 gap-3 mt-6">
          <div className="p-0.5 bg-primary/5 rounded-[1.3rem] ring-1 ring-primary/10">
            <div className="bg-white p-4 rounded-[1.1rem] text-center">
              <span className="text-gray-500 text-xs font-medium block mb-1 leading-snug">나의 찜</span>
              <h5 className="font-bold text-primary text-xl">{wishlist.length}</h5>
            </div>
          </div>
          <div className="p-0.5 bg-gray-50 rounded-[1.3rem] ring-1 ring-gray-100">
            <div className="bg-white p-4 rounded-[1.1rem] text-center">
              <span className="text-gray-500 text-xs font-medium block mb-1 leading-snug">다녀온 곳</span>
              <h5 className="font-bold text-gray-900 text-xl">{totalVisits}</h5>
            </div>
          </div>
        </div>
      </div>

      {/* Personalized Services */}
      <div className="px-6 mb-8">
        <h6 className="font-bold text-gray-800 mb-4 ml-1 flex items-center gap-2">
          <Award className="text-primary" size={18} />
          개인 맞춤 서비스
        </h6>
        <div className="flex gap-3">
          <div className="flex-1 flex flex-col gap-2 bg-primary/5 border border-primary/15 rounded-3xl p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-1">
              <span className="w-8 h-8 bg-primary/15 rounded-full flex items-center justify-center shrink-0">
                <Smile className="text-primary" size={16} />
              </span>
              <span className="text-sm font-bold text-gray-800">오늘 기분 어때?</span>
            </div>
            <div className="flex gap-1.5">
              <button
                onClick={() => setPetMood('good')}
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-spring active:scale-[0.97] ${
                  petMood === 'good' ? 'bg-green-500 text-white' : 'bg-white text-gray-500 border border-gray-100'
                }`}
              >
                <Smile size={14} />
                좋아요
              </button>
              <button
                onClick={() => setPetMood('normal')}
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-spring active:scale-[0.97] ${
                  petMood === 'normal' ? 'bg-amber-400 text-white' : 'bg-white text-gray-500 border border-gray-100'
                }`}
              >
                <Meh size={14} />
                그냥요
              </button>
              <button
                onClick={() => setPetMood('bad')}
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-spring active:scale-[0.97] ${
                  petMood === 'bad' ? 'bg-red-400 text-white' : 'bg-white text-gray-500 border border-gray-100'
                }`}
              >
                <Frown size={14} />
                힘들어요
              </button>
            </div>
          </div>
          <button
            onClick={() => onNavigate('visit-checkin')}
            className="flex-1 flex flex-col items-center justify-center gap-3 bg-white border border-gray-100 rounded-3xl p-5 shadow-sm active:scale-[0.97] transition-spring"
          >
            <span className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center">
              <Award className="text-primary" size={24} />
            </span>
            <span className="text-sm font-bold text-gray-800">방문 인증 센터</span>
          </button>
        </div>
      </div>

      {/* 나의 활동 / 관리자 센터 */}
      <div className="px-6 mb-8">
        {isAdmin ? (
          /* 관리자 전용: 관리자 센터 바로가기 */
          <button
            onClick={() => onNavigate('admin')}
            className="w-full flex items-center justify-between p-4 bg-gray-900 rounded-3xl shadow-sm active:scale-[0.97] transition-spring"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary/20 rounded-2xl flex items-center justify-center">
                <Shield size={20} className="text-primary" />
              </div>
              <div className="text-left">
                <div className="text-sm font-bold text-white">관리자 센터</div>
                <div className="text-[11px] text-gray-400 mt-0.5">회원·장소·신고·통계 관리</div>
              </div>
            </div>
            <ChevronRight size={18} className="text-gray-500" />
          </button>
        ) : (
          /* 일반 유저: 나의 활동 통계 */
          <div className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden">
            <div className="px-4 pt-4 pb-2">
              <h6 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <ImageIcon size={15} className="text-primary" /> 나의 활동
              </h6>
            </div>
            <div className="grid grid-cols-4 gap-2 px-4 pb-4">
              {myStats.map((s, i) => (
                <div
                  key={i}
                  onClick={s.page ? () => onNavigate(s.page!) : undefined}
                  className={`bg-gray-50 rounded-xl p-2.5 text-center border border-gray-100 relative ${
                    s.page ? 'cursor-pointer active:scale-[0.97] transition-spring hover:bg-gray-100' : ''
                  }`}
                >
                  <div className={`w-7 h-7 rounded-full ${s.color} flex items-center justify-center mx-auto mb-1.5 relative`}>
                    <s.icon size={13} />
                    {s.unread ? (
                      <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-red-500 text-white text-[8px] font-bold rounded-full flex items-center justify-center">
                        {s.unread}
                      </span>
                    ) : null}
                  </div>
                  <div className="text-base font-bold text-gray-800">{s.value}</div>
                  <div className="text-[10px] text-gray-500 font-medium">{s.label}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 활동 지역 & 반경 */}
      <div className="px-6 mb-8">
        <h6 className="font-bold text-gray-800 mb-4 ml-1 flex items-center gap-2">
          <MapPin className="text-primary" size={18} />
          활동 지역 & 반경
        </h6>
        <div className="bg-white rounded-3xl border border-gray-100 shadow-sm p-4 space-y-4">
          {/* 지역 드롭다운 */}
          <div>
            <p className="text-xs font-bold text-gray-600 mb-2">활동 지역 <span className="text-gray-400 font-normal">(미선택 시 서울 강남구 기본)</span></p>
            <div className="flex gap-2">
              <select
                value={locationSido}
                onChange={e => { setLocationSido(e.target.value); setLocationDistrict(''); setLocationSaved(false); }}
                className="flex-1 px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-700 outline-none focus:border-primary transition-spring"
              >
                <option value="">시·도 선택</option>
                {SIDO_LIST.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <select
                value={locationDistrict}
                onChange={e => { setLocationDistrict(e.target.value); setLocationSaved(false); }}
                disabled={!locationSido}
                className="flex-1 px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-700 outline-none focus:border-primary transition-spring disabled:text-gray-300"
              >
                <option value="">시·군·구 선택</option>
                {locationDistricts.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          </div>

          {/* 반경 세그먼트 바 */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <p className="text-xs font-bold text-gray-600">활동 반경</p>
              <span className="text-[11px] text-gray-400">선택한 지역 기준으로 장소를 추천해드려요</span>
            </div>
            <div className="flex rounded-2xl overflow-hidden border-2 border-gray-100 h-12">
              {([
                { value: 5  as const, label: '5km',   desc: '가까운 거리' },
                { value: 15 as const, label: '15km',  desc: '중간 거리' },
                { value: 30 as const, label: '먼거리', desc: '넓은 범위' },
              ]).map((rs, idx) => {
                const filled = idx <= [5, 15, 30].indexOf(locationRadius);
                return (
                  <button
                    key={rs.value}
                    type="button"
                    onClick={() => { setLocationRadius(rs.value); setLocationSaved(false); }}
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

          {/* 저장 버튼 */}
          <button
            onClick={handleSaveLocation}
            disabled={isSavingLocation}
            className={`w-full py-3 rounded-2xl text-sm font-bold transition-spring active:scale-[0.98] ${
              locationSaved
                ? 'bg-green-500 text-white'
                : 'bg-primary text-white hover:bg-primary/90'
            }`}
          >
            {isSavingLocation ? '저장 중...' : locationSaved ? '✓ 저장됐어요' : '저장하기'}
          </button>
        </div>
      </div>

      {/* Wishlist */}
      <div className="px-6 mb-8">
        <h6 className="font-bold text-gray-800 mb-4 ml-1 flex items-center gap-2">
          <Leaf className="text-primary" size={18} />
          찜한 장소 목록
        </h6>
        {wishItems.length === 0 ? (
          <div className="text-center py-10 bg-white rounded-3xl border border-gray-100 text-gray-400">
            <Leaf size={32} className="mx-auto mb-2 opacity-50" />
            <p className="text-sm">찜한 장소가 비어있습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {wishItems.map((p, idx) => (
              <div
                key={p.id}
                className="p-1 bg-white rounded-[1.6rem] ring-1 ring-gray-100 shadow-[0_2px_12px_rgba(0,0,0,0.05)] active:scale-[0.97] transition-spring cursor-pointer animate-fade-in-up"
                style={{ animationDelay: `${idx * 0.06}s` }}
                onClick={() => onNavigate('detail', { id: p.id })}
              >
                <div className="bg-gray-50 rounded-[1.25rem] p-1.5">
                  <PlaceImage imageUrl={p.imageUrl} category={p.category} className="w-full aspect-square rounded-xl mb-0 object-cover" />
                </div>
                <div className="px-2 py-2">
                  <h6 className="font-bold text-gray-900 text-sm truncate leading-snug">{p.title}</h6>
                  <span className="text-gray-400 text-[10px]">{p.address}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Saved Routes */}
      <div className="px-6">
        <h6 className="font-bold text-gray-800 mb-4 ml-1 flex items-center gap-2">
          <Navigation className="text-primary" size={18} />
          저장된 AI 추천 경로
        </h6>
        {(!savedRoutes || savedRoutes.length === 0) ? (
          <div className="text-center py-10 bg-white rounded-3xl border border-gray-100 text-gray-400">
            <Navigation size={32} className="mx-auto mb-2 opacity-50" />
            <p className="text-sm">저장된 추천 경로가 없습니다.</p>
            <button
              onClick={() => onNavigate('ai-walk-guide')}
              className="mt-4 px-4 py-2 bg-primary/10 text-primary rounded-full text-sm font-bold"
            >
              추천 경로 받으러 가기
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {savedRoutes.map(route => (
              <div key={route.id} className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 relative">
                <button
                  onClick={() => removeSavedRoute(route.id)}
                  className="absolute top-4 right-4 p-2 text-gray-400 hover:text-red-500 transition-spring bg-gray-50 rounded-full"
                >
                  <Trash2 size={16} />
                </button>
                <div className="flex items-center gap-2 text-sm text-gray-500 mb-3">
                  <Calendar size={14} />
                  <span>{route.date}</span>
                  <span className="w-1 h-1 bg-gray-300 rounded-full mx-1" />
                  <Clock size={14} />
                  <span>{route.bestTime}</span>
                </div>
                <div className="space-y-2 mt-4">
                  {route.routes.map((r, idx) => (
                    <div key={idx} className="flex items-center justify-between bg-gray-50 p-3 rounded-xl">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center text-primary">
                          <MapPin size={16} />
                        </div>
                        <div>
                          <div className="font-bold text-gray-900 text-sm">{r.name}</div>
                          <div className="text-xs text-gray-500 flex items-center gap-1 mt-0.5">
                            <span className="bg-white border border-gray-200 px-1.5 py-0.5 rounded text-[10px]">{r.type}</span>
                            <span>{r.distance}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 반려동물 등록/수정 폼 모달 */}
      <AnimatePresence>
        {showPetForm && (
          <PetProfileForm
            initialData={editingIndex !== null ? pets[editingIndex] : null}
            hasExistingPets={editingIndex === null && pets.length > 0}
            onClose={() => { setShowPetForm(false); setEditingIndex(null); }}
            onSubmit={handlePetSubmit}
          />
        )}
      </AnimatePresence>

      {/* 반려동물 삭제 확인 모달 */}
      <AnimatePresence>
        {deleteTargetIndex !== null && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center px-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/50 backdrop-blur-sm"
              onClick={() => setDeleteTargetIndex(null)}
            />
            <motion.div
              initial={{ scale: 0.85, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.85, opacity: 0 }}
              transition={{ type: 'spring', damping: 25, stiffness: 350 }}
              className="bg-white w-full max-w-[300px] rounded-3xl p-6 relative z-10 shadow-2xl text-center"
            >
              <div className="w-14 h-14 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Trash2 size={24} className="text-destructive" />
              </div>
              <h3 className="font-bold text-gray-800 mb-2">반려동물 정보 삭제</h3>
              <p className="text-sm text-gray-500 mb-5">
                <span className="font-bold">{deleteTargetIndex !== null ? pets[deleteTargetIndex]?.name : ''}</span>의 정보를 삭제하시겠습니까?
                {pets[deleteTargetIndex ?? -1]?.isRepresentative && pets.length > 1 && (
                  <><br /><span className="text-amber-600 text-xs mt-1 block">다음 등록 동물이 대표로 자동 설정됩니다.</span></>
                )}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setDeleteTargetIndex(null)}
                  className="flex-1 py-3 rounded-xl bg-gray-100 text-gray-500 font-bold text-sm hover:bg-gray-200 transition-spring"
                >
                  취소
                </button>
                <button
                  onClick={executeDelete}
                  className="flex-1 py-3 rounded-xl bg-destructive text-white font-bold text-sm hover:bg-destructive/90 active:scale-[0.97] transition-spring"
                >
                  삭제
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
