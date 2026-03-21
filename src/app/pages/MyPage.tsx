import React from 'react';
import { Leaf, Navigation, Calendar, Clock, MapPin, Trash2, Heart, Award, Shield, Image as ImageIcon, MessageCircle, Send, AlertTriangle, EyeOff, ChevronRight, PawPrint, Pencil, Plus, Star, Smile, Meh, Frown } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { useFeedStore } from '../store/useFeedStore';
import { useDMStore } from '../store/useDMStore';
import { PetProfileForm } from '../components/PetProfileForm';
import { PlaceImage } from '../components/PlaceImage';
import type { PetInfo } from '../store/useAppStore';
import { AnimatePresence } from 'motion/react';
import { motion } from 'motion/react';

interface MyPageProps {
  onNavigate: (page: string, params?: any) => void;
}

export function MyPage({ onNavigate }: MyPageProps) {
  const { wishlist, savedRoutes, removeSavedRoute, pets, addPet, updatePet, removePet, setRepresentativePet, isAdmin, username, places, fetchPlaces } = useAppStore();
  const { posts } = useFeedStore();
  const { conversations, getUnreadTotal } = useDMStore();

  const [showPetForm, setShowPetForm] = React.useState(false);
  const [editingIndex, setEditingIndex] = React.useState<number | null>(null);
  const [deleteTargetIndex, setDeleteTargetIndex] = React.useState<number | null>(null);
  const [petMood, setPetMood] = React.useState<'good' | 'normal' | 'bad' | null>(null);

  React.useEffect(() => { if (places.length === 0) fetchPlaces(); }, []);
  const wishItems = places.filter(p => wishlist.includes(p.id));

  // 나의 활동 통계 (일반 유저용)
  const myTotalLikes    = posts.reduce((acc, p) => acc + p.likes, 0);
  const myTotalComments = posts.reduce((acc, p) => acc + p.comments, 0);
  const dmUnreadCount   = getUnreadTotal(username);
  const dmTotalCount    = conversations.reduce((acc, c) => acc + c.messages.filter(m => m.from !== username).length, 0);

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
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className="bg-gray-50"
    >
      {/* Profile Header */}
      <div className="bg-white p-6 pb-8 rounded-b-[40px] shadow-[0_4px_20px_rgba(0,0,0,0.05)] mb-6">

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
            className="flex items-center gap-1 px-3 py-1.5 bg-primary text-white text-xs font-bold rounded-full shadow-md active:scale-95 transition-all"
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
                className={`relative flex items-center gap-3 p-3.5 rounded-2xl border-2 transition-colors ${
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
                  {pet.type === '강아지' ? '🐶' : '🐱'}
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
                      className="flex items-center gap-1 text-[10px] font-bold text-gray-500 bg-gray-100 hover:bg-primary/10 hover:text-primary px-2 py-1 rounded-lg transition-colors"
                    >
                      <Star size={10} /> 대표설정
                    </button>
                  )}
                  <div className="flex gap-1">
                    <button
                      onClick={() => openEdit(index)}
                      className="p-2 text-gray-400 hover:text-primary bg-gray-50 hover:bg-primary/5 rounded-lg transition-colors"
                    >
                      <Pencil size={13} />
                    </button>
                    <button
                      onClick={() => confirmDelete(index)}
                      className="p-2 text-gray-400 hover:text-destructive bg-gray-50 hover:bg-red-50 rounded-lg transition-colors"
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
          <div className="bg-gray-50 p-4 rounded-2xl text-center border border-gray-100">
            <span className="text-gray-500 text-xs font-medium block mb-1">나의 찜</span>
            <h5 className="font-bold text-primary text-xl">{wishlist.length}</h5>
          </div>
          <div className="bg-gray-50 p-4 rounded-2xl text-center border border-gray-100">
            <span className="text-gray-500 text-xs font-medium block mb-1">다녀온 곳</span>
            <h5 className="font-bold text-gray-900 text-xl">0</h5>
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
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-all active:scale-95 ${
                  petMood === 'good' ? 'bg-green-500 text-white' : 'bg-white text-gray-500 border border-gray-100'
                }`}
              >
                <Smile size={14} />
                좋아요
              </button>
              <button
                onClick={() => setPetMood('normal')}
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-all active:scale-95 ${
                  petMood === 'normal' ? 'bg-amber-400 text-white' : 'bg-white text-gray-500 border border-gray-100'
                }`}
              >
                <Meh size={14} />
                그냥요
              </button>
              <button
                onClick={() => setPetMood('bad')}
                className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl text-[10px] font-bold transition-all active:scale-95 ${
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
            className="flex-1 flex flex-col items-center justify-center gap-3 bg-white border border-gray-100 rounded-3xl p-5 shadow-sm active:scale-[0.98] transition-all"
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
            className="w-full flex items-center justify-between p-4 bg-gray-900 rounded-3xl shadow-sm active:scale-[0.98] transition-all"
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
                    s.page ? 'cursor-pointer active:scale-95 transition-transform hover:bg-gray-100' : ''
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
            {wishItems.map(p => (
              <div
                key={p.id}
                className="bg-white p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer border border-gray-50"
                onClick={() => onNavigate('detail', { id: p.id })}
              >
                <PlaceImage imageUrl={p.imageUrl} category={p.category} className="w-full aspect-square rounded-2xl mb-2 object-cover" />
                <h6 className="font-bold text-gray-900 text-sm truncate px-1">{p.title}</h6>
                <span className="text-gray-400 text-[10px] px-1">{p.address}</span>
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
                  className="absolute top-4 right-4 p-2 text-gray-400 hover:text-red-500 transition-colors bg-gray-50 rounded-full"
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
                  className="flex-1 py-3 rounded-xl bg-gray-100 text-gray-500 font-bold text-sm hover:bg-gray-200 transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={executeDelete}
                  className="flex-1 py-3 rounded-xl bg-destructive text-white font-bold text-sm hover:bg-destructive/90 active:scale-95 transition-all"
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
