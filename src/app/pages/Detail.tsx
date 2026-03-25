import React, { useState, useEffect } from 'react';
import {
  ArrowLeft, Heart, Share2, MapPin, Star, ChevronRight,
  Phone, Clock, Info, Home, TreePine, PawPrint, ClipboardList,
  Building2, Bookmark, Tag, FileText, ThumbsUp, ThumbsDown,
  Map, TrendingUp, Newspaper,
} from 'lucide-react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { ShareSheet } from '../components/ShareSheet';
import { PlaceImage } from '../components/PlaceImage';
import { reviewApi } from '../api/reviewApi';
import { ReviewDto } from '../api/types';

const CATEGORY_TAG: Record<string, string> = {
  PLACE: '#반려동물명소',
  STAY: '#반려동물숙박',
  DINING: '#반려동물식당',
};

interface DetailProps {
  id: number;
  onNavigate?: (page: string, params?: any) => void;
}

export function Detail({ id, onNavigate }: DetailProps) {
  const places = useAppStore((s) => s.places);
  const place = places.find(p => p.id === id);
  const [showShare, setShowShare] = useState(false);
  const [reviewRating, setReviewRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [reviewCount, setReviewCount] = useState(0);
  const [avgRating, setAvgRating] = useState(0);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [showFullDesc, setShowFullDesc] = useState(false);
  const [showAiPolicy, setShowAiPolicy] = useState(false);
  const [activeTab, setActiveTab] = useState<'info' | 'review'>('info');

  const { wishlist, toggleWishlist, isLoggedIn } = useAppStore();
  const isWishlisted = place ? wishlist.includes(id) : false;

  useEffect(() => {
    if (!place) return;
    setReviewCount(place.reviewCount ?? 0);
    setAvgRating(place.rating ?? 0);
    setIsLoadingReviews(true);
    reviewApi.getReviewsByPlace(id)
      .then((data) => {
        setReviews(data.reviews);
        setReviewCount(data.totalCount);
        setAvgRating(data.averageRating);
      })
      .catch((err) => console.error('[Review] 불러오기 실패:', err))
      .finally(() => setIsLoadingReviews(false));
  }, [id]);

  if (!place) return null;

  const displayTitle = (place as any).name || place.title || '이름 없음';
  const fullAddress = place.addr2
    ? `${place.address} ${place.addr2}`.trim()
    : place.address;

  // 반려동물 정책 표시 여부
  const hasPetInfo = place.chkPetInside || place.accomCountPet?.trim() || place.petTurnAdroose || place.tags || place.petFacility || place.petPolicy;

  // tags 파싱
  const tagList = place.tags
    ? place.tags.split(',').map((t: string) => t.replace(/\[.*?\]/g, '').trim()).filter(Boolean)
    : [];
  const sizeTags = tagList.filter(t => ['소형', '중형', '대형', '모두 가능', '소형견', '중형견', '대형견'].some(k => t.includes(k)));
  const indoorTags = tagList.filter(t => ['실내', '실외', '실내외'].includes(t));
  const categoryTags = tagList.filter(t => !sizeTags.includes(t) && !indoorTags.includes(t));

  // AI 별점 breakdown
  const aiBreakdown = place.aiRating != null ? (() => {
    if (place.aiRating === 0) return null;
    const aScore = 2.0;
    let bScore = 0.0;
    const petFriendly = place.chkPetInside === 'Y' || place.tags?.includes('대형견');
    if (petFriendly) bScore += 0.4;
    if (place.imageUrl && place.overview && place.overview.length >= 50) bScore += 0.3;
    if (place.phone && place.homepage) bScore += 0.3;
    bScore = Math.round(bScore * 10) / 10;
    const cdScore = Math.max(0, Math.round((place.aiRating! - aScore - bScore) * 10) / 10);
    return { aScore, bScore, cdScore };
  })() : null;

  const hasAiContent = (place.aiRating != null && place.aiRating > 0) || place.overview || place.blogCount;

  const handleSubmitReview = async () => {
    if (reviewRating === 0 || !reviewText.trim() || !isLoggedIn) return;
    try {
      const saved = await reviewApi.createReview(id, { rating: reviewRating, content: reviewText.trim() });
      setReviews([saved, ...reviews]);
      setReviewCount(c => c + 1);
      setReviewRating(0);
      setReviewText('');
    } catch (err) {
      console.error('[Review] 등록 실패:', err);
    }
  };

  // 리뷰 별점 분포 계산
  const ratingDistribution = [5, 4, 3, 2, 1].map(star => ({
    star,
    count: reviews.filter(r => r.rating === star).length,
  }));

  return (
    <motion.div
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 50 }}
      transition={{ type: 'spring', damping: 28, stiffness: 320 }}
      className="bg-white min-h-screen relative"
    >
      <div className="max-w-[600px] mx-auto">
        {/* ── 히어로 이미지 + 플로팅 헤더 ── */}
        <div className="relative h-[260px] overflow-hidden">
          <PlaceImage
            imageUrl={place.imageUrl ?? (place as any).img ?? null}
            category={place.category}
            className="w-full h-full object-cover"
            iconSize={60}
            alt={displayTitle}
          />
          {/* 그라데이션 오버레이 */}
          <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-transparent to-black/30 pointer-events-none" />

          {/* 플로팅 헤더 버튼 */}
          <div className="absolute top-0 left-0 right-0 px-4 h-14 flex items-center justify-between z-10">
            <button
              onClick={() => onNavigate && onNavigate('back')}
              className="w-9 h-9 flex items-center justify-center bg-black/25 backdrop-blur-sm rounded-full text-white hover:bg-black/40 transition-spring hover:scale-[1.1] active:scale-[0.9]"
              aria-label="뒤로가기"
            >
              <ArrowLeft size={18} />
            </button>
            <div className="flex gap-2">
              <button
                onClick={() => toggleWishlist(id)}
                className="w-9 h-9 flex items-center justify-center bg-black/25 backdrop-blur-sm rounded-full text-white hover:bg-black/40 transition-spring hover:scale-[1.1] active:scale-[0.9]"
              >
                <Heart size={18} className={isWishlisted ? "fill-white" : ""} />
              </button>
              <button
                onClick={() => setShowShare(true)}
                className="w-9 h-9 flex items-center justify-center bg-black/25 backdrop-blur-sm rounded-full text-white hover:bg-black/40 transition-spring hover:scale-[1.1] active:scale-[0.9]"
              >
                <Share2 size={18} />
              </button>
            </div>
          </div>

          {/* 이미지 하단 카테고리 배지 */}
          <div className="absolute bottom-3 left-4">
            <span className="text-[11px] font-bold text-white bg-black/35 backdrop-blur-sm px-2.5 py-1 rounded-full">
              {CATEGORY_TAG[place.category] ?? '#반려동물여행'}
            </span>
          </div>
        </div>

        {/* ── 타이틀 카드 ── */}
        <div className="px-5 pt-4 pb-3 border-b border-gray-100">
          <h2 className="text-[20px] font-bold text-gray-900 mb-1.5 leading-tight">{displayTitle}</h2>

          {/* 카테고리 해시태그 */}
          {categoryTags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mb-2.5">
              {categoryTags.slice(0, 2).map((tag, idx) => (
                <span key={idx} className="text-[12px] text-primary font-medium">#{tag}</span>
              ))}
            </div>
          )}

          {/* 평점 */}
          <div className="flex items-center gap-2 mb-2.5">
            {reviewCount > 0 ? (
              <>
                <div className="flex items-center gap-1">
                  <Star size={14} className="text-brand-point fill-brand-point" />
                  <span className="text-[13px] font-bold text-gray-900">{avgRating.toFixed(1)}</span>
                </div>
                <span className="text-xs text-gray-400">리뷰 {reviewCount}개</span>
              </>
            ) : place.aiRating ? (
              <>
                <div className="flex items-center gap-1">
                  <Star size={14} className="fill-[#008BFF] text-[#008BFF]" />
                  <span className="text-[13px] font-bold text-gray-900">{place.aiRating.toFixed(1)}</span>
                </div>
                <button
                  onClick={() => setShowAiPolicy(true)}
                  className="flex items-center gap-1 bg-[#008BFF]/10 text-[#008BFF] text-[10px] font-bold px-2 py-0.5 rounded-full hover:bg-[#008BFF]/20 transition-spring"
                >
                  <span className="flex items-center justify-center w-[12px] h-[12px] bg-[#008BFF] text-white rounded-full text-[8px] font-black leading-none">i</span>
                  AI 추천
                </button>
              </>
            ) : (
              <span className="text-xs text-gray-400 flex items-center gap-1"><PawPrint size={12} /> 첫 리뷰를 남겨주세요!</span>
            )}
          </div>

          {/* 주소 */}
          <div className="flex items-start gap-1.5">
            <MapPin size={12} className="text-gray-400 shrink-0 mt-0.5" />
            <span className="text-[12px] text-gray-500 leading-relaxed">{fullAddress}</span>
          </div>
        </div>

        {/* ── 탭 네비게이션 ── */}
        <div className="sticky top-0 z-40 bg-white border-b border-gray-100 flex">
          <button
            onClick={() => setActiveTab('info')}
            className={`flex-1 py-3 text-[13px] font-bold transition-spring relative ${
              activeTab === 'info' ? 'text-primary' : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            정보
            {activeTab === 'info' && (
              <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t" />
            )}
          </button>
          <button
            onClick={() => setActiveTab('review')}
            className={`flex-1 py-3 text-[13px] font-bold transition-spring relative ${
              activeTab === 'review' ? 'text-primary' : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            리뷰
            {reviewCount > 0 && (
              <span className={`ml-1.5 text-[11px] px-1.5 py-0.5 rounded-full font-bold ${
                activeTab === 'review' ? 'bg-primary/15 text-primary' : 'bg-gray-100 text-gray-400'
              }`}>
                {reviewCount}
              </span>
            )}
            {activeTab === 'review' && (
              <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t" />
            )}
          </button>
        </div>

        {/* ── 정보 탭 ── */}
        {activeTab === 'info' && (
          <div className="px-5 py-4 space-y-5">

            {/* 반려동물 동반 정보 */}
            {hasPetInfo && (
              <div>
                <h3 className="text-[13px] font-bold text-gray-500 uppercase tracking-wider mb-3 flex items-center gap-2">
                  <PawPrint size={14} className="text-primary" /> 반려동물 동반 정보
                  <div className="flex-1 h-px bg-gray-100" />
                </h3>
                <div className="flex flex-wrap gap-2 mb-3">
                  {place.chkPetInside === 'Y' && (
                    <div className="flex items-center gap-1.5 bg-primary/5 border border-primary/20 px-3 py-2 rounded-xl">
                      <Home size={14} className="text-primary" />
                      <span className="text-[12px] font-medium text-primary">실내 동반가능</span>
                    </div>
                  )}
                  {place.chkPetInside === 'N' && (
                    <div className="flex items-center gap-1.5 bg-green-50 border border-green-200 px-3 py-2 rounded-xl">
                      <TreePine size={14} className="text-green-600" />
                      <span className="text-[12px] font-medium text-green-700">실외 동반</span>
                    </div>
                  )}
                  {indoorTags.map((tag, idx) => !place.chkPetInside && (
                    <div key={idx} className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 px-3 py-2 rounded-xl">
                      {tag.includes('실내') ? <Home size={14} className="text-gray-500" /> : <TreePine size={14} className="text-gray-500" />}
                      <span className="text-[12px] font-medium text-gray-700">{tag}</span>
                    </div>
                  ))}
                  {sizeTags.map((tag, idx) => (
                    <div key={idx} className="flex items-center gap-1.5 bg-amber-50 border border-amber-200 px-3 py-2 rounded-xl">
                      <PawPrint size={14} className="text-amber-600" />
                      <span className="text-[12px] font-medium text-amber-700">{tag}</span>
                    </div>
                  ))}
                  {place.accomCountPet?.trim() && (
                    <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 px-3 py-2 rounded-xl">
                      <PawPrint size={14} className="text-gray-500" />
                      <span className="text-[12px] font-medium text-gray-700">수용 {place.accomCountPet}</span>
                    </div>
                  )}
                </div>
                {place.petTurnAdroose && (
                  <div className="bg-gray-50 border border-gray-200 rounded-xl p-3">
                    <p className="text-[11px] font-bold text-gray-700 mb-1 flex items-center gap-1"><ClipboardList size={11} /> 동반 규정</p>
                    <p className="text-xs text-gray-600 leading-relaxed whitespace-pre-line">{place.petTurnAdroose}</p>
                  </div>
                )}
                {place.petFacility && place.petFacility !== '정보 없음' && (
                  <div className="bg-primary/5 border border-primary/15 rounded-xl p-3 mt-2">
                    <div className="flex items-center gap-1.5 mb-1">
                      <p className="text-[11px] font-bold text-primary flex items-center gap-1"><Building2 size={11} /> 반려동물 전용 시설</p>
                      <span className="text-[9px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1 py-0.5 rounded-full font-medium">AI 보강</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.petFacility}</p>
                  </div>
                )}
                {place.petPolicy && place.petPolicy !== '정보 없음' && (
                  <div className="bg-amber-50 border border-amber-100 rounded-xl p-3 mt-2">
                    <div className="flex items-center gap-1.5 mb-1">
                      <p className="text-[11px] font-bold text-amber-700 flex items-center gap-1"><Bookmark size={11} /> 반려동물 이용 규정</p>
                      <span className="text-[9px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1 py-0.5 rounded-full font-medium">AI 보강</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.petPolicy}</p>
                  </div>
                )}
              </div>
            )}

            {/* 운영 + 업체 정보 */}
            <div>
              <h3 className="text-[13px] font-bold text-gray-500 uppercase tracking-wider mb-3 flex items-center gap-2">
                업체 정보
                <div className="flex-1 h-px bg-gray-100" />
                <span className="text-[10px] bg-green-50 text-green-600 border border-green-200 px-1.5 py-0.5 rounded-full font-medium normal-case tracking-normal">검증된 정보</span>
              </h3>
              <div className="bg-gray-50 rounded-2xl p-4 space-y-3">
                <div className="flex items-start gap-3">
                  <div className="w-7 h-7 flex items-center justify-center bg-white border border-gray-200 rounded-lg shrink-0">
                    <MapPin size={13} className="text-gray-500" />
                  </div>
                  <span className="text-[12px] text-gray-700 leading-relaxed pt-1">{fullAddress}</span>
                </div>
                {place.phone && (
                  <a href={`tel:${place.phone}`} className="flex items-center gap-3 text-[12px] text-gray-700 hover:text-primary transition-spring group">
                    <div className="w-7 h-7 flex items-center justify-center bg-white border border-gray-200 rounded-lg shrink-0 group-hover:border-primary/30 transition-spring">
                      <Phone size={13} className="text-gray-500 group-hover:text-primary transition-spring" />
                    </div>
                    {place.phone}
                  </a>
                )}
                {(place.operatingHours || place.operationPolicy) && (
                  <div className="flex items-start gap-3">
                    <div className="w-7 h-7 flex items-center justify-center bg-white border border-gray-200 rounded-lg shrink-0">
                      <Clock size={13} className="text-gray-500" />
                    </div>
                    <div className="pt-1">
                      {place.operatingHours && (
                        <p className="text-[12px] text-gray-700 leading-relaxed">{place.operatingHours}</p>
                      )}
                      {place.operationPolicy && place.operationPolicy !== '정보 없음' && (
                        <p className="text-[11px] text-gray-500 leading-relaxed mt-0.5">{place.operationPolicy}</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* AI 분석 섹션 */}
            {hasAiContent && (
              <div>
                <h3 className="text-[13px] font-bold text-gray-500 uppercase tracking-wider mb-3 flex items-center gap-2">
                  AI 분석
                  <div className="flex-1 h-px bg-gray-100" />
                  <span className="text-[10px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1.5 py-0.5 rounded-full font-medium normal-case tracking-normal">AI 생성</span>
                </h3>

                <div className="space-y-3">
                  {/* AI 추천 별점 근거 */}
                  {aiBreakdown && (
                    <div className="border border-[#008BFF]/15 rounded-2xl overflow-hidden">
                      {/* 총점 헤더 */}
                      <div className="bg-gradient-to-r from-[#008BFF]/8 to-blue-50/60 px-4 py-3 flex items-center justify-between">
                        <div>
                          <p className="text-[12px] font-bold text-gray-800">AI 종합 점수</p>
                          <p className="text-[10px] text-gray-500 mt-0.5">운영 안정성 + 반려동물 친화도 + 화제성</p>
                        </div>
                        <div className="flex items-end gap-1">
                          <span className="text-[22px] font-black text-[#008BFF] leading-none">{place.aiRating!.toFixed(1)}</span>
                          <span className="text-[11px] text-gray-400 mb-0.5">/ 5.0</span>
                        </div>
                      </div>
                      {/* 점수 바 */}
                      <div className="px-4 py-2.5 bg-white">
                        <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                          <motion.div
                            className="h-full bg-gradient-to-r from-[#008BFF] to-blue-300 rounded-full"
                            initial={{ width: '0%' }}
                            animate={{ width: `${(place.aiRating! / 5) * 100}%` }}
                            transition={{ type: 'spring', damping: 25, stiffness: 100, delay: 0.3 }}
                          />
                        </div>
                      </div>
                      {/* 세부 항목 */}
                      <div className="divide-y divide-gray-50">
                        {/* 운영 안정성 */}
                        <div className="px-4 py-3 flex items-center gap-3">
                          <Building2 size={18} className="text-green-600" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[12px] font-bold text-gray-800">운영 안정성</span>
                              <span className="text-[13px] font-black text-green-600">{aiBreakdown.aScore.toFixed(1)}</span>
                            </div>
                            <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                              <motion.div
                                className="h-full bg-green-400 rounded-full"
                                initial={{ width: '0%' }}
                                animate={{ width: `${(aiBreakdown.aScore / 5) * 100}%` }}
                                transition={{ type: 'spring', damping: 25, stiffness: 100, delay: 0.45 }}
                              />
                            </div>
                          </div>
                        </div>
                        {/* 반려동물 친화도 */}
                        <div className="px-4 py-3 flex items-center gap-3">
                          <PawPrint size={18} className="text-[#008BFF]" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[12px] font-bold text-gray-800">반려동물 친화도</span>
                              <span className="text-[13px] font-black text-[#008BFF]">{aiBreakdown.bScore.toFixed(1)}</span>
                            </div>
                            <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                              <motion.div
                                className="h-full bg-[#008BFF] rounded-full"
                                initial={{ width: '0%' }}
                                animate={{ width: `${aiBreakdown.bScore * 100}%` }}
                                transition={{ type: 'spring', damping: 25, stiffness: 100, delay: 0.55 }}
                              />
                            </div>
                          </div>
                        </div>
                        {/* 화제성 */}
                        <div className="px-4 py-3 flex items-center gap-3">
                          <Newspaper size={18} className="text-orange-500" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[12px] font-bold text-gray-800">화제성</span>
                              <span className="text-[13px] font-black text-orange-500">{aiBreakdown.cdScore.toFixed(1)}</span>
                            </div>
                            <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                              <motion.div
                                className="h-full bg-orange-400 rounded-full"
                                initial={{ width: '0%' }}
                                animate={{ width: `${Math.min((aiBreakdown.cdScore / 2) * 100, 100)}%` }}
                                transition={{ type: 'spring', damping: 25, stiffness: 100, delay: 0.65 }}
                              />
                            </div>
                          </div>
                        </div>
                      </div>
                      {/* 산정 기준 링크 */}
                      <button
                        onClick={() => setShowAiPolicy(true)}
                        className="w-full flex items-center justify-center gap-1 py-2.5 border-t border-gray-50 text-[11px] text-[#008BFF] font-medium hover:bg-blue-50/30 transition-spring"
                      >
                        <Info size={11} />
                        AI 점수 산정 기준 보기
                      </button>
                    </div>
                  )}

                  {/* 장소 해시태그 */}
                  {place.tags && (() => {
                    const allTags = place.tags!
                      .split(/[,，]/)
                      .map(t => t.trim().replace(/\[NAVER\]/g, '').trim())
                      .filter(Boolean);
                    if (allTags.length === 0) return null;
                    const restrictionPattern = /미만|이하|제한|금지|불가|소형견만|소형만|케이지|목줄/;
                    const positive = allTags.filter(t => !restrictionPattern.test(t));
                    const restricted = allTags.filter(t => restrictionPattern.test(t));
                    return (
                      <div>
                        <p className="text-[12px] font-bold text-gray-700 mb-2 flex items-center gap-1"><Tag size={12} /> 장소 태그</p>
                        <div className="flex flex-wrap gap-1.5">
                          {positive.map((tag, idx) => (
                            <span key={`pos-${idx}`} className="text-[12px] font-medium bg-green-50 text-green-700 border border-green-200 px-2.5 py-1 rounded-full transition-spring hover:scale-105 active:scale-95 cursor-default inline-block animate-fade-in-up" style={{ animationDelay: `${idx * 0.04}s` }}>
                              #{tag}
                            </span>
                          ))}
                          {restricted.map((tag, idx) => (
                            <span key={`res-${idx}`} className="text-[12px] font-medium bg-orange-50 text-orange-600 border border-orange-200 px-2.5 py-1 rounded-full transition-spring hover:scale-105 active:scale-95 cursor-default inline-block">
                              #{tag}
                            </span>
                          ))}
                        </div>
                      </div>
                    );
                  })()}

                  {/* 장소 소개 */}
                  {place.overview && (
                    <div>
                      <p className="text-[12px] font-bold text-gray-700 mb-2 flex items-center gap-1"><FileText size={12} /> 장소 소개</p>
                      <p className={`text-xs text-gray-600 leading-relaxed ${!showFullDesc ? 'line-clamp-3' : ''}`}>
                        {place.overview}
                      </p>
                      {place.overview.length > 100 && (
                        <button
                          onClick={() => setShowFullDesc(!showFullDesc)}
                          className="text-[#008BFF] text-xs font-bold flex items-center gap-0.5 mt-1.5 hover:underline"
                        >
                          {showFullDesc ? '접기' : '더보기'} <ChevronRight size={13} className={showFullDesc ? 'rotate-90' : ''} />
                        </button>
                      )}
                    </div>
                  )}

                  {/* 블로그 반응 */}
                  {place.blogCount != null && place.blogCount > 0 && (
                    <div>
                      <p className="text-[12px] font-bold text-gray-700 mb-1 flex items-center gap-1">
                        <TrendingUp size={12} /> 블로그 반응
                        <span className="text-[11px] font-normal text-gray-400 ml-1.5">
                          {place.blogCount.toLocaleString()}건 분석
                        </span>
                      </p>
                      <p className="text-[11px] text-gray-400 mb-2">"애견동반" 블로그 후기 키워드</p>
                      <div className="flex flex-wrap gap-1.5">
                        {place.blogPositiveTags?.split(',').filter(Boolean).map((tag, idx) => (
                          <span key={idx} className="inline-flex items-center gap-1 text-[12px] font-medium bg-green-50 text-green-700 border border-green-200 px-2.5 py-1 rounded-full">
                            <ThumbsUp size={10} /> {tag}
                          </span>
                        ))}
                        {place.blogNegativeTags?.split(',').filter(Boolean).map((tag, idx) => (
                          <span key={idx} className="inline-flex items-center gap-1 text-[12px] font-medium bg-red-50 text-red-500 border border-red-200 px-2.5 py-1 rounded-full">
                            <ThumbsDown size={10} /> {tag}
                          </span>
                        ))}
                        {!place.blogPositiveTags && !place.blogNegativeTags && (
                          <p className="text-xs text-gray-400">특정 키워드가 감지되지 않았습니다.</p>
                        )}
                      </div>
                    </div>
                  )}

                  <p className="text-[10px] text-gray-400 text-center pt-1 border-t border-blue-100/50">
                    AI 분석 정보는 참고용이며 실제와 다를 수 있습니다.
                  </p>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── 리뷰 탭 ── */}
        {activeTab === 'review' && (
          <div className="px-5 py-4">

            {/* 리뷰 요약 카드 */}
            {reviews.length > 0 && (
              <div className="bg-gray-50 rounded-2xl p-4 mb-5 flex gap-5 items-center">
                <div className="text-center shrink-0">
                  <p className="text-[36px] font-black text-gray-900 leading-none">{avgRating.toFixed(1)}</p>
                  <div className="flex justify-center gap-0.5 my-1.5">
                    {[1, 2, 3, 4, 5].map(star => (
                      <Star
                        key={star}
                        size={11}
                        className={star <= Math.round(avgRating) ? "text-brand-point fill-brand-point" : "text-gray-200"}
                      />
                    ))}
                  </div>
                  <p className="text-[11px] text-gray-400">{reviewCount}개 리뷰</p>
                </div>
                <div className="flex-1 space-y-1.5">
                  {ratingDistribution.map(({ star, count }) => (
                    <div key={star} className="flex items-center gap-2">
                      <span className="text-[11px] text-gray-500 w-3 text-right shrink-0">{star}</span>
                      <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-brand-point rounded-full transition-all"
                          style={{ width: reviews.length > 0 ? `${(count / reviews.length) * 100}%` : '0%' }}
                        />
                      </div>
                      <span className="text-[11px] text-gray-400 w-4 shrink-0">{count}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 리뷰 작성 */}
            <div className="border border-gray-200 rounded-2xl p-4 mb-5">
              <p className="text-[13px] font-bold text-gray-800 mb-3">리뷰 작성하기</p>
              <div className="flex items-center gap-2 mb-3">
                <span className="text-[12px] text-gray-500">방문은 어떠셨나요?</span>
                <div className="flex gap-0.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button key={star} onClick={() => setReviewRating(star)} className="p-0.5 transition-spring hover:scale-[1.2] active:scale-[0.85]">
                      <Star
                        size={20}
                        className={star <= reviewRating ? "text-brand-point fill-brand-point" : "text-gray-300"}
                      />
                    </button>
                  ))}
                </div>
              </div>
              <textarea
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="이 장소에 대한 솔직한 후기를 남겨주세요."
                className="w-full h-[72px] bg-gray-50 rounded-xl px-3 py-2.5 text-xs text-gray-700 placeholder:text-gray-400 border border-gray-100 resize-none outline-none focus:border-primary/50 transition-spring"
              />
              {!isLoggedIn && (
                <p className="text-[11px] text-gray-400 text-center mt-2">로그인 후 리뷰를 작성할 수 있습니다.</p>
              )}
              <button
                onClick={handleSubmitReview}
                disabled={!isLoggedIn || reviewRating === 0 || !reviewText.trim()}
                className={`w-full mt-3 py-2.5 rounded-xl text-sm font-bold transition-spring ${
                  isLoggedIn && reviewRating > 0 && reviewText.trim()
                    ? 'bg-primary text-white hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.97]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                리뷰 등록하기
              </button>
            </div>

            {/* 리뷰 목록 */}
            {isLoadingReviews ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="flex gap-2.5 animate-pulse">
                    <div className="w-8 h-8 rounded-full bg-gray-100 shrink-0" />
                    <div className="flex-1 space-y-2 py-1">
                      <div className="h-2.5 bg-gray-100 rounded-full w-1/3" />
                      <div className="h-2 bg-gray-100 rounded-full w-full" />
                      <div className="h-2 bg-gray-100 rounded-full w-2/3" />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="space-y-4">
                {reviews.length === 0 ? (
                  <p className="text-center py-8 text-xs text-gray-400">아직 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요!</p>
                ) : (
                  reviews.map((review, idx) => (
                    <div key={review.reviewId} className={`pb-4 border-b border-gray-50 last:border-0 ${idx < 5 ? 'animate-fade-in-up' : ''}`} style={idx < 5 ? { animationDelay: `${idx * 0.07}s` } : undefined}>
                      <div className="flex items-start gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-0.5">
                          <span className="text-[11px] font-bold text-primary">
                            {review.nickname?.charAt(0) ?? '?'}
                          </span>
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-xs font-bold text-gray-800">{review.nickname}</span>
                            <span className="text-[11px] text-gray-400">
                              {review.createdAt ? review.createdAt.slice(0, 10).replace(/-/g, '.') : ''}
                            </span>
                          </div>
                          <div className="flex gap-0.5 mb-1.5">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <Star
                                key={star}
                                size={12}
                                className={star <= review.rating ? "text-brand-point fill-brand-point" : "text-gray-200"}
                              />
                            ))}
                          </div>
                          <p className="text-xs text-gray-600 leading-relaxed">{review.content}</p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 고정 하단 바 여유 공간 — flexbox padding-bottom 버그 우회 */}
      <div className="h-24" />

      {/* ── 고정 하단 바 ── */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 px-5 py-3 pb-5 z-50 max-w-[600px] mx-auto">
        <div className="flex gap-3">
          <button
            onClick={() => toggleWishlist(id)}
            className={`flex-1 py-3.5 rounded-xl text-sm font-bold border-2 flex items-center justify-center gap-1.5 transition-spring hover:scale-[1.02] active:scale-[0.97] ${
              isWishlisted
                ? 'bg-destructive/5 border-destructive text-destructive'
                : 'bg-gray-50 border-gray-200 text-gray-700 hover:border-gray-300'
            }`}
          >
            <Heart size={16} className={isWishlisted ? 'fill-destructive text-destructive' : ''} />
            {isWishlisted ? '찜 완료' : '찜하기'}
          </button>
          <button
            onClick={() => onNavigate?.('map', { placeId: id })}
            className="flex-1 py-3.5 rounded-xl text-sm font-bold bg-primary text-white text-center hover:bg-primary/90 transition-spring hover:scale-[1.02] active:scale-[0.97] flex items-center justify-center gap-2"
          >
            <Map size={16} /> 길찾기
          </button>
        </div>
      </div>

      {/* 공유 시트 */}
      <ShareSheet
        isOpen={showShare}
        onClose={() => setShowShare(false)}
        postId={id}
        postImage={place.imageUrl || (place as any).img || ""}
        postUser={displayTitle}
      />

      {/* AI 추천 안내 모달 */}
      {showAiPolicy && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
          onClick={() => setShowAiPolicy(false)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            onClick={(e) => e.stopPropagation()}
            className="bg-white rounded-2xl p-5 max-w-[320px] w-full shadow-xl"
          >
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-[15px] font-bold text-gray-900 flex items-center gap-1.5">
                <span className="flex items-center justify-center w-4 h-4 bg-[#008BFF] text-white rounded-full text-[10px] font-black">i</span>
                AI 추천 가이드
              </h3>
              <button onClick={() => setShowAiPolicy(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div className="space-y-3 text-xs text-gray-600 leading-relaxed">
              <p>
                <strong className="text-[#008BFF]">멍냥트립 AI 추천 점수</strong>는 공공데이터 + 블로그 반응을 종합하여 산정됩니다.
              </p>
              <div className="bg-gray-50 rounded-xl p-3 space-y-2.5">
                <div className="flex gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-500 mt-1.5 shrink-0" />
                  <div>
                    <strong className="text-gray-800">운영 안정성</strong>
                    <p className="mt-0.5 text-[11px] text-gray-500">지속적인 운영 안정성 (기본 2.0점)</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#008BFF] mt-1.5 shrink-0" />
                  <div>
                    <strong className="text-gray-800">반려동물 친화도</strong>
                    <p className="mt-0.5 text-[11px] text-gray-500">실내동반·대형견·연락처 정보 충실도</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-orange-400 mt-1.5 shrink-0" />
                  <div>
                    <strong className="text-gray-800">화제성</strong>
                    <p className="mt-0.5 text-[11px] text-gray-500">블로그 후기 수 + 최신성 + 감성 반응</p>
                  </div>
                </div>
              </div>
              <p className="text-[11px] text-gray-400 text-center pt-2 border-t border-gray-100">
                AI 점수는 지속적인 학습을 통해 업데이트됩니다.
              </p>
            </div>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
