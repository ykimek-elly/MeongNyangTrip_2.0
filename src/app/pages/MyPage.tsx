import React from 'react';
import { Dog, Leaf, Navigation, Calendar, Clock, MapPin, Trash2, Heart, Award, Shield, Image as ImageIcon, MessageCircle, Send, AlertTriangle, EyeOff, ChevronRight, BarChart3, PawPrint, Pencil, Plus, X } from 'lucide-react';
import { places } from '../data/places';
import { useAppStore } from '../store/useAppStore';
import { useFeedStore } from '../store/useFeedStore';
import { PetProfileForm } from '../components/PetProfileForm';
import { getAgeGroupLabel, getCheckupCycleLabel } from '../data/pet-care-helpers';
import type { PetInfo } from '../store/useAppStore';
import { AnimatePresence } from 'motion/react';

import { motion } from 'motion/react';

interface MyPageProps {
  onNavigate: (page: string, params?: any) => void;
}

export function MyPage({ onNavigate }: MyPageProps) {
  const { username, wishlist, savedRoutes, removeSavedRoute, pet, registerPet, updatePet, removePet } = useAppStore();
  const { posts } = useFeedStore();
  const [showPetForm, setShowPetForm] = React.useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = React.useState(false);
  
  const wishItems = places.filter(p => wishlist.includes(p.id));

  // Admin overview stats
  const totalLikes = posts.reduce((acc, p) => acc + p.likes, 0);
  const totalComments = posts.reduce((acc, p) => acc + p.comments, 0);
  const totalDMs = posts.reduce((acc, p) => acc + p.dms, 0);
  const reportedPosts = posts.filter(p => p.isReported).length;
  const hiddenPosts = posts.filter(p => p.isHidden).length;
  const unreadDMs = posts.reduce((acc, p) => acc + p.dmList.filter(d => !d.isRead).length, 0);

  const overviewStats = [
    { label: '게시글', value: posts.length, icon: ImageIcon, color: 'bg-blue-50 text-blue-600' },
    { label: '좋아요', value: totalLikes, icon: Heart, color: 'bg-pink-50 text-primary' },
    { label: '댓글', value: totalComments, icon: MessageCircle, color: 'bg-green-50 text-green-600' },
    { label: 'DM', value: totalDMs, icon: Send, color: 'bg-purple-50 text-purple-600' },
    { label: '신고', value: reportedPosts, icon: AlertTriangle, color: 'bg-red-50 text-red-600' },
    { label: '숨김', value: hiddenPosts, icon: EyeOff, color: 'bg-gray-100 text-gray-600' },
  ];

  return (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className="min-h-screen bg-gray-50 pb-24"
    >
      {/* Profile Header */}
      <div className="bg-white p-6 pb-8 rounded-b-[40px] shadow-[0_4px_20px_rgba(0,0,0,0.05)] mb-6">
        {/* 반려동물 프로필 영역 */}
        {pet ? (
          <div className="mb-8 mt-4">
            <div className="flex items-center gap-4">
              <div className="w-[70px] h-[70px] bg-primary/10 rounded-full flex items-center justify-center shadow-inner text-3xl shrink-0">
                {pet.type === '강아지' ? '🐶' : '🐱'}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <h4 className="font-bold text-xl text-gray-900">{pet.name}</h4>
                  <span className="text-[10px] font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                    {getAgeGroupLabel(pet.age)}
                  </span>
                </div>
                <p className="text-sm text-gray-500">
                  {pet.breed} · {pet.age}살 · {pet.gender}
                </p>
                <div className="flex items-center gap-2 mt-1.5">
                  <span className="text-[10px] text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                    {pet.size === 'SMALL' ? '소형' : pet.size === 'MEDIUM' ? '중형' : '대형'}
                  </span>
                  <span className="text-[10px] text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                    활동량 {pet.activity === 'LOW' ? '적음' : pet.activity === 'NORMAL' ? '보통' : '많음'}
                  </span>
                  {pet.weight && (
                    <span className="text-[10px] text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                      {pet.weight}kg
                    </span>
                  )}
                </div>
              </div>
              <button
                onClick={() => setShowPetForm(true)}
                className="p-2.5 text-gray-400 hover:text-primary transition-colors bg-gray-50 rounded-full shrink-0"
              >
                <Pencil size={16} />
              </button>
            </div>

            {/* 검진 주기 배너 */}
            <div className="mt-4 bg-amber-50 border border-amber-100 rounded-2xl p-3 flex items-center gap-2">
              <Calendar size={14} className="text-amber-500 shrink-0" />
              <span className="text-xs text-amber-800">
                권장 검진 주기: <span className="font-bold">{getCheckupCycleLabel(pet.age)}</span>
              </span>
            </div>
          </div>
        ) : (
          <div className="mb-8 mt-4">
            <div className="flex items-center gap-4">
              <div className="w-[70px] h-[70px] bg-gray-100 rounded-full flex items-center justify-center shadow-inner">
                <PawPrint size={28} className="text-gray-300" />
              </div>
              <div className="flex-1">
                <h4 className="font-bold text-gray-900 mb-1">반려동물을 등록해주세요</h4>
                <p className="text-xs text-gray-400">등록하면 맞춤 케어 플랜을 받을 수 있어요!</p>
              </div>
              <button
                onClick={() => setShowPetForm(true)}
                className="px-4 py-2 bg-primary text-white text-xs font-bold rounded-full shadow-md active:scale-95 transition-all shrink-0 flex items-center gap-1"
              >
                <Plus size={12} />
                등록
              </button>
            </div>
          </div>
        )}
        
        <div className="grid grid-cols-2 gap-3">
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
          <button
            onClick={() => onNavigate('senior-pet')}
            className="flex-1 flex flex-col items-center justify-center gap-3 bg-white border border-gray-100 rounded-3xl p-5 shadow-sm active:scale-[0.98] transition-all"
          >
            <span className="w-12 h-12 bg-amber-50 rounded-full flex items-center justify-center">
              <Heart className="text-amber-500 fill-amber-100" size={24} />
            </span>
            <span className="text-sm font-bold text-gray-800">펫 케어 시스템</span>
          </button>
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

      {/* Admin Overview Summary */}
      <div className="px-6 mb-8">
        <div className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 pt-4 pb-2">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gray-900 rounded-full flex items-center justify-center">
                <Shield size={14} className="text-white" />
              </div>
              <div>
                <h6 className="text-sm font-bold text-gray-900">관리자 현황</h6>
                <span className="text-[10px] text-gray-400">라운지 전체 현황 요약</span>
              </div>
            </div>
            <button
              onClick={() => onNavigate('admin')}
              className="flex items-center gap-0.5 text-xs font-bold text-primary active:opacity-70 transition-opacity"
            >
              상세보기 <ChevronRight size={14} />
            </button>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-3 gap-2 px-4 py-3">
            {overviewStats.map((s, i) => (
              <div key={i} className="bg-gray-50 rounded-xl p-2.5 text-center border border-gray-100">
                <div className={`w-7 h-7 rounded-full ${s.color} flex items-center justify-center mx-auto mb-1.5`}>
                  <s.icon size={13} />
                </div>
                <div className="text-base font-bold text-gray-800">{s.value}</div>
                <div className="text-[10px] text-gray-500 font-medium">{s.label}</div>
              </div>
            ))}
          </div>

          {/* Alert Banner (if needed) */}
          {(reportedPosts > 0 || unreadDMs > 0) && (
            <div className="mx-4 mb-4 bg-red-50 border border-red-100 rounded-xl p-3 flex items-start gap-2.5">
              <AlertTriangle size={16} className="text-red-500 shrink-0 mt-0.5" />
              <div>
                <h6 className="text-xs font-bold text-red-700">관리자 알림</h6>
                <div className="text-[11px] text-red-600 space-y-0.5 mt-0.5">
                  {reportedPosts > 0 && <p>신고된 게시글 {reportedPosts}건</p>}
                  {unreadDMs > 0 && <p>읽지 않은 DM {unreadDMs}건</p>}
                </div>
              </div>
            </div>
          )}
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
            {wishItems.map(p => (
              <div 
                key={p.id} 
                className="bg-white p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer border border-gray-50"
                onClick={() => onNavigate('detail', { id: p.id })}
              >
                <img src={p.imageUrl || ""} className="w-full aspect-square rounded-2xl mb-2 object-cover bg-gray-100" alt={p.title} />
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
            initialData={pet}
            onClose={() => setShowPetForm(false)}
            onSubmit={(petData) => {
              if (pet) {
                updatePet(petData);
              } else {
                registerPet(petData);
              }
              setShowPetForm(false);
            }}
          />
        )}
      </AnimatePresence>

      {/* 반려동물 삭제 확인 모달 */}
      <AnimatePresence>
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center px-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/50 backdrop-blur-sm"
              onClick={() => setShowDeleteConfirm(false)}
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
                {pet?.name}의 정보를 삭제하시겠습니까?<br />
                펫 케어 데이터도 초기화됩니다.
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  className="flex-1 py-3 rounded-xl bg-gray-100 text-gray-500 font-bold text-sm hover:bg-gray-200 transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={() => { removePet(); setShowDeleteConfirm(false); }}
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