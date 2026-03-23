import React, { useState, useEffect } from 'react';
import {
  ArrowLeft, Heart, Share2, MapPin, Star, ChevronRight,
  Phone, ChevronDown, ChevronUp
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
  const [showAiSection, setShowAiSection] = useState(true);

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

  // tags 파싱 — 크기/실내외 정보 추출
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

  // AI 분석 섹션 노출 여부
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

  return (
    <motion.div
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 50 }}
      className="bg-white min-h-screen pb-24 relative"
    >
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-12 flex items-center justify-between max-w-[600px] mx-auto w-full">
        <button
          onClick={() => onNavigate && onNavigate('back')}
          className="p-1.5 -ml-1.5 text-gray-800 hover:bg-gray-100 rounded-full"
          aria-label="뒤로가기"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-[15px] font-bold text-gray-900 truncate max-w-[200px]">{displayTitle}</h1>
        <div className="flex gap-0.5">
          <button
            onClick={() => toggleWishlist(id)}
            className="p-1.5 text-gray-500 hover:bg-gray-100 rounded-full transition-colors"
          >
            <Heart size={20} className={isWishlisted ? "fill-destructive text-destructive" : ""} />
          </button>
          <button
            onClick={() => setShowShare(true)}
            className="p-1.5 text-gray-500 hover:bg-gray-100 rounded-full"
          >
            <Share2 size={20} />
          </button>
        </div>
      </header>

      <div className="max-w-[600px] mx-auto">
        {/* 메인 이미지 */}
        <div className="w-full aspect-video overflow-hidden">
          <PlaceImage
            imageUrl={place.imageUrl ?? (place as any).img ?? null}
            category={place.category}
            className="w-full h-full object-cover"
            iconSize={52}
            alt={displayTitle}
          />
        </div>

        <div className="px-5">
          {/* 제목 + 평점 */}
          <div className="pt-5 pb-4 border-b border-gray-100">
            <h2 className="text-[20px] font-bold text-gray-900 mb-1.5">{displayTitle}</h2>
            {/* 카테고리 태그 */}
            {categoryTags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mb-2">
                {categoryTags.slice(0, 2).map((tag, idx) => (
                  <span key={idx} className="text-[12px] text-primary font-medium">#{tag}</span>
                ))}
                <span className="text-[12px] text-primary font-medium">
                  {CATEGORY_TAG[place.category] ?? '#반려동물여행'}
                </span>
              </div>
            )}
            {reviewCount > 0 ? (
              <div className="flex items-center gap-1.5">
                <Star size={14} className="text-brand-point fill-brand-point" />
                <span className="text-xs font-bold text-gray-900">{avgRating.toFixed(1)}</span>
                <span className="text-xs text-gray-400">({reviewCount}개 리뷰)</span>
              </div>
            ) : place.aiRating ? (
              <div className="flex items-center gap-1.5">
                <Star size={14} className="fill-[#008BFF] text-[#008BFF]" />
                <span className="text-xs font-bold text-gray-900">{place.aiRating.toFixed(1)}</span>
                <button
                  onClick={() => setShowAiPolicy(true)}
                  className="flex items-center gap-1 bg-[#008BFF]/10 hover:bg-[#008BFF]/20 text-[#008BFF] text-[10px] font-bold px-2 py-0.5 rounded-full transition-colors active:scale-95"
                >
                  <span className="flex items-center justify-center w-[12px] h-[12px] bg-[#008BFF] text-white rounded-full text-[8px] font-black leading-none">i</span>
                  AI 추천
                </button>
              </div>
            ) : (
              <span className="text-xs text-gray-400">🐾 첫 리뷰를 남겨주세요!</span>
            )}
          </div>

          {/* ──────────────────────────────────────────
              공공데이터 기반 정보
          ────────────────────────────────────────── */}
          <div className="py-1">
            <div className="flex items-center gap-2 py-3">
              <span className="text-[11px] font-bold text-gray-400 tracking-wider uppercase">공공데이터 기반 정보</span>
              <div className="flex-1 h-px bg-gray-100" />
              <span className="text-[10px] bg-green-50 text-green-600 border border-green-200 px-1.5 py-0.5 rounded-full font-medium">검증된 정보</span>
            </div>

            {/* 반려동물 동반 정보 */}
            {hasPetInfo && (
              <div className="pb-4 border-b border-gray-100">
                <h3 className="text-[15px] font-bold text-gray-900 mb-3">🐾 반려동물 동반 정보</h3>
                <div className="flex flex-wrap gap-2 mb-3">
                  {place.chkPetInside === 'Y' && (
                    <div className="flex items-center gap-1.5 bg-primary/5 border border-primary/20 px-3 py-2 rounded-xl">
                      <span className="text-sm">🏠</span>
                      <span className="text-[12px] font-medium text-primary">실내 동반가능</span>
                    </div>
                  )}
                  {place.chkPetInside === 'N' && (
                    <div className="flex items-center gap-1.5 bg-green-50 border border-green-200 px-3 py-2 rounded-xl">
                      <span className="text-sm">🌳</span>
                      <span className="text-[12px] font-medium text-green-700">실외 동반</span>
                    </div>
                  )}
                  {indoorTags.map((tag, idx) => !place.chkPetInside && (
                    <div key={idx} className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 px-3 py-2 rounded-xl">
                      <span className="text-sm">{tag.includes('실내') ? '🏠' : '🌳'}</span>
                      <span className="text-[12px] font-medium text-gray-700">{tag}</span>
                    </div>
                  ))}
                  {sizeTags.map((tag, idx) => (
                    <div key={idx} className="flex items-center gap-1.5 bg-amber-50 border border-amber-200 px-3 py-2 rounded-xl">
                      <span className="text-sm">🐕</span>
                      <span className="text-[12px] font-medium text-amber-700">{tag}</span>
                    </div>
                  ))}
                  {place.accomCountPet?.trim() && (
                    <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 px-3 py-2 rounded-xl">
                      <span className="text-sm">🐾</span>
                      <span className="text-[12px] font-medium text-gray-700">수용 {place.accomCountPet}</span>
                    </div>
                  )}
                </div>
                {place.petTurnAdroose && (
                  <div className="bg-gray-50 border border-gray-200 rounded-xl p-3">
                    <p className="text-[11px] font-bold text-gray-700 mb-1">📋 동반 규정</p>
                    <p className="text-xs text-gray-600 leading-relaxed whitespace-pre-line">{place.petTurnAdroose}</p>
                  </div>
                )}
                {place.petFacility && place.petFacility !== '정보 없음' && (
                  <div className="bg-primary/5 border border-primary/15 rounded-xl p-3 mt-2">
                    <div className="flex items-center gap-1.5 mb-1">
                      <p className="text-[11px] font-bold text-primary">🏗️ 반려동물 전용 시설</p>
                      <span className="text-[9px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1 py-0.5 rounded-full font-medium">AI 보강</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.petFacility}</p>
                  </div>
                )}
                {place.petPolicy && place.petPolicy !== '정보 없음' && (
                  <div className="bg-amber-50 border border-amber-100 rounded-xl p-3 mt-2">
                    <div className="flex items-center gap-1.5 mb-1">
                      <p className="text-[11px] font-bold text-amber-700">📌 반려동물 이용 규정</p>
                      <span className="text-[9px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1 py-0.5 rounded-full font-medium">AI 보강</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.petPolicy}</p>
                  </div>
                )}
              </div>
            )}

            {/* 운영 정보 */}
            {(place.operatingHours || place.operationPolicy) && (
              <div className="pb-4 border-b border-gray-100">
                <h3 className="text-[15px] font-bold text-gray-900 mb-3">⏰ 운영 정보</h3>
                {place.operatingHours && (
                  <div className="bg-gray-50 rounded-xl p-3 mb-2">
                    <p className="text-[11px] font-bold text-gray-700 mb-1">영업 시간</p>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.operatingHours}</p>
                  </div>
                )}
                {place.operationPolicy && place.operationPolicy !== '정보 없음' && (
                  <div className="bg-gray-50 rounded-xl p-3">
                    <div className="flex items-center gap-1.5 mb-1">
                      <p className="text-[11px] font-bold text-gray-700">운영 정책</p>
                      <span className="text-[9px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1 py-0.5 rounded-full font-medium">AI 보강</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{place.operationPolicy}</p>
                  </div>
                )}
              </div>
            )}

            {/* 업체 정보 */}
            <div className="pb-4 border-b border-gray-100">
              <h3 className="text-[15px] font-bold text-gray-900 mb-3">🏪 업체 정보</h3>
              <div className="space-y-2.5">
                <div className="flex items-start gap-2">
                  <MapPin size={13} className="text-gray-400 shrink-0 mt-0.5" />
                  <span className="text-xs text-gray-600 leading-relaxed">{fullAddress}</span>
                </div>
                {place.phone && (
                  <a href={`tel:${place.phone}`} className="flex items-center gap-2 text-xs text-gray-700 hover:text-primary transition-colors">
                    <Phone size={13} className="text-gray-400 shrink-0" />
                    {place.phone}
                  </a>
                )}
              </div>
            </div>
          </div>

          {/* ──────────────────────────────────────────
              AI 분석
          ────────────────────────────────────────── */}
          {hasAiContent && (
            <div className="py-1">
              <button
                onClick={() => setShowAiSection(!showAiSection)}
                className="flex items-center gap-2 w-full py-3"
              >
                <span className="text-[11px] font-bold text-gray-400 tracking-wider uppercase">AI 분석</span>
                <div className="flex-1 h-px bg-gray-100" />
                <span className="text-[10px] bg-[#008BFF]/10 text-[#008BFF] border border-[#008BFF]/20 px-1.5 py-0.5 rounded-full font-medium">AI 생성</span>
                {showAiSection
                  ? <ChevronUp size={14} className="text-gray-400" />
                  : <ChevronDown size={14} className="text-gray-400" />
                }
              </button>

              {showAiSection && (
                <div className="bg-blue-50/30 rounded-2xl p-4 mb-4 space-y-4">

                  {/* AI 추천 별점 근거 */}
                  {aiBreakdown && (
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <span className="text-[13px] font-bold text-gray-800">🤖 AI 추천 별점 근거</span>
                        <button
                          onClick={() => setShowAiPolicy(true)}
                          className="text-[10px] text-[#008BFF] underline"
                        >
                          산정 기준
                        </button>
                      </div>

                      {/* 총점 바 */}
                      <div className="bg-white border border-[#008BFF]/20 rounded-2xl px-3.5 py-3 mb-3">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-[11px] text-gray-500 font-medium">AI 종합 점수</span>
                          <span className="text-[15px] font-black text-[#008BFF]">
                            {place.aiRating!.toFixed(1)} <span className="text-[11px] font-normal text-gray-400">/ 5.0</span>
                          </span>
                        </div>
                        <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-gradient-to-r from-[#008BFF] to-blue-300 rounded-full"
                            style={{ width: `${(place.aiRating! / 5) * 100}%` }}
                          />
                        </div>
                        <p className="text-[10px] text-gray-400 mt-1.5">운영 안정성 + 반려동물 친화도 + 화제성 합산</p>
                      </div>

                      <div className="space-y-2.5">
                        {/* 1. 운영 안정성 */}
                        <div className="bg-white border border-green-100 rounded-xl p-3">
                          <div className="flex items-center justify-between mb-1.5">
                            <div className="flex items-center gap-1.5">
                              <span className="text-[13px]">🏢</span>
                              <span className="text-[12px] font-bold text-gray-800">운영 안정성</span>
                              <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">공공DB 등재</span>
                            </div>
                            <span className="text-[14px] font-black text-green-600">{aiBreakdown.aScore.toFixed(1)}</span>
                          </div>
                          <div className="w-full h-1.5 bg-gray-100 rounded-full mb-2.5 overflow-hidden">
                            <div className="h-full bg-green-400 rounded-full" style={{ width: `${(aiBreakdown.aScore / 5) * 100}%` }} />
                          </div>
                          <div className="space-y-1">
                            <p className="flex items-center gap-1.5 text-[11px] text-gray-600">
                              <span className="text-green-500 font-black text-[10px]">✓</span>
                              한국관광공사 공공DB 정식 등록 업소
                            </p>
                            <p className="flex items-center gap-1.5 text-[11px] text-gray-600">
                              <span className="text-green-500 font-black text-[10px]">✓</span>
                              폐업·운영 중단 이력 없음
                            </p>
                          </div>
                        </div>

                        {/* 2. 반려동물 친화도 */}
                        <div className="bg-white border border-[#008BFF]/20 rounded-xl p-3">
                          <div className="flex items-center justify-between mb-1.5">
                            <div className="flex items-center gap-1.5">
                              <span className="text-[13px]">🐾</span>
                              <span className="text-[12px] font-bold text-gray-800">반려동물 친화도</span>
                              <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">최대 1.0</span>
                            </div>
                            <span className="text-[14px] font-black text-[#008BFF]">{aiBreakdown.bScore.toFixed(1)}</span>
                          </div>
                          <div className="w-full h-1.5 bg-gray-100 rounded-full mb-2.5 overflow-hidden">
                            <div className="h-full bg-[#008BFF] rounded-full" style={{ width: `${aiBreakdown.bScore * 100}%` }} />
                          </div>
                          {(() => {
                            const petFriendly = place.chkPetInside === 'Y' || place.tags?.includes('대형견');
                            const hasMedia = !!(place.imageUrl && place.overview && place.overview.length >= 50);
                            const hasContact = !!(place.phone && place.homepage);
                            return (
                              <div className="space-y-1">
                                <p className={`flex items-center gap-1.5 text-[11px] ${petFriendly ? 'text-gray-600' : 'text-gray-400'}`}>
                                  <span className={`font-black text-[10px] ${petFriendly ? 'text-[#008BFF]' : 'text-gray-300'}`}>
                                    {petFriendly ? '✓' : '–'}
                                  </span>
                                  실내 동반 허용 또는 대형견 가능
                                  <span className="text-[10px] text-gray-400 ml-auto">+0.4</span>
                                </p>
                                <p className={`flex items-center gap-1.5 text-[11px] ${hasMedia ? 'text-gray-600' : 'text-gray-400'}`}>
                                  <span className={`font-black text-[10px] ${hasMedia ? 'text-[#008BFF]' : 'text-gray-300'}`}>
                                    {hasMedia ? '✓' : '–'}
                                  </span>
                                  사진 및 상세 소개글 등록
                                  <span className="text-[10px] text-gray-400 ml-auto">+0.3</span>
                                </p>
                                <p className={`flex items-center gap-1.5 text-[11px] ${hasContact ? 'text-gray-600' : 'text-gray-400'}`}>
                                  <span className={`font-black text-[10px] ${hasContact ? 'text-[#008BFF]' : 'text-gray-300'}`}>
                                    {hasContact ? '✓' : '–'}
                                  </span>
                                  전화번호 · 홈페이지 모두 등록
                                  <span className="text-[10px] text-gray-400 ml-auto">+0.3</span>
                                </p>
                              </div>
                            );
                          })()}
                        </div>

                        {/* 3. 화제성 */}
                        <div className="bg-white border border-orange-100 rounded-xl p-3">
                          <div className="flex items-center justify-between mb-1.5">
                            <div className="flex items-center gap-1.5">
                              <span className="text-[13px]">📰</span>
                              <span className="text-[12px] font-bold text-gray-800">화제성</span>
                              <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">블로그/SNS</span>
                            </div>
                            <span className="text-[14px] font-black text-orange-500">{aiBreakdown.cdScore.toFixed(1)}</span>
                          </div>
                          <div className="w-full h-1.5 bg-gray-100 rounded-full mb-2.5 overflow-hidden">
                            <div className="h-full bg-orange-400 rounded-full" style={{ width: `${Math.min((aiBreakdown.cdScore / 2) * 100, 100)}%` }} />
                          </div>
                          <div className="space-y-1">
                            <p className={`flex items-center gap-1.5 text-[11px] ${(place.blogCount ?? 0) > 0 ? 'text-gray-600' : 'text-gray-400'}`}>
                              <span className={`font-black text-[10px] ${(place.blogCount ?? 0) > 0 ? 'text-orange-400' : 'text-gray-300'}`}>
                                {(place.blogCount ?? 0) > 0 ? '✓' : '–'}
                              </span>
                              "애견동반" 블로그 후기{' '}
                              {(place.blogCount ?? 0) > 0
                                ? <span className="font-bold text-orange-500">{place.blogCount!.toLocaleString()}건</span>
                                : '후기 없음'}
                            </p>
                            <p className={`flex items-center gap-1.5 text-[11px] ${(place.blogCount ?? 0) >= 100 ? 'text-gray-600' : 'text-gray-400'}`}>
                              <span className={`font-black text-[10px] ${(place.blogCount ?? 0) >= 100 ? 'text-orange-400' : 'text-gray-300'}`}>
                                {(place.blogCount ?? 0) >= 100 ? '✓' : '–'}
                              </span>
                              100건 이상 → 높은 화제성 가산점
                            </p>
                          </div>
                        </div>
                      </div>
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
                        <p className="text-[13px] font-bold text-gray-800 mb-2">🏷️ 장소 태그</p>
                        <div className="flex flex-wrap gap-1.5">
                          {positive.map((tag, idx) => (
                            <span key={`pos-${idx}`} className="text-[12px] font-medium bg-green-50 text-green-700 border border-green-200 px-2.5 py-1 rounded-full">
                              #{tag}
                            </span>
                          ))}
                          {restricted.map((tag, idx) => (
                            <span key={`res-${idx}`} className="text-[12px] font-medium bg-orange-50 text-orange-600 border border-orange-200 px-2.5 py-1 rounded-full">
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
                      <p className="text-[13px] font-bold text-gray-800 mb-2">📝 장소 소개</p>
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
                      <p className="text-[13px] font-bold text-gray-800 mb-1">
                        📊 블로그 반응
                        <span className="text-[11px] font-normal text-gray-400 ml-1.5">
                          {place.blogCount.toLocaleString()}건 분석
                        </span>
                      </p>
                      <p className="text-[11px] text-gray-400 mb-2">"애견동반" 블로그 후기 키워드</p>
                      <div className="flex flex-wrap gap-1.5">
                        {place.blogPositiveTags?.split(',').filter(Boolean).map((tag, idx) => (
                          <span key={idx} className="text-[12px] font-medium bg-green-50 text-green-700 border border-green-200 px-2.5 py-1 rounded-full">
                            👍 {tag}
                          </span>
                        ))}
                        {place.blogNegativeTags?.split(',').filter(Boolean).map((tag, idx) => (
                          <span key={idx} className="text-[12px] font-medium bg-red-50 text-red-500 border border-red-200 px-2.5 py-1 rounded-full">
                            👎 {tag}
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
              )}
            </div>
          )}

          {/* ──────────────────────────────────────────
              리뷰
          ────────────────────────────────────────── */}
          <div className="py-5 border-t border-gray-100">
            <h3 className="text-[15px] font-bold text-gray-900 mb-4">
              리뷰 <span className="text-primary">{reviewCount}</span>
            </h3>

            {/* 리뷰 작성 */}
            <div className="border border-gray-200 rounded-2xl p-4 mb-5">
              <div className="flex items-center gap-1 mb-2">
                <span className="text-[11px] text-primary font-bold mr-1">✍️</span>
                <span className="text-xs font-bold text-gray-800">리뷰 작성하기</span>
              </div>
              <div className="flex items-center gap-1 mb-3">
                <span className="text-[12px] text-gray-500 mr-1">방문은 어떠셨나요?</span>
                <div className="flex gap-0.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button key={star} onClick={() => setReviewRating(star)} className="p-0.5">
                      <Star
                        size={18}
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
                className="w-full h-[72px] bg-gray-50 rounded-xl px-3 py-2.5 text-xs text-gray-700 placeholder:text-gray-400 border border-gray-100 resize-none outline-none focus:border-primary/50 transition-colors"
              />
              {!isLoggedIn && (
                <p className="text-[11px] text-gray-400 text-center mt-2">로그인 후 리뷰를 작성할 수 있습니다.</p>
              )}
              <button
                onClick={handleSubmitReview}
                disabled={!isLoggedIn || reviewRating === 0 || !reviewText.trim()}
                className={`w-full mt-3 py-2.5 rounded-xl text-sm font-bold transition-colors ${
                  isLoggedIn && reviewRating > 0 && reviewText.trim()
                    ? 'bg-primary text-white hover:bg-primary/90 active:scale-[0.98]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                리뷰 등록하기
              </button>
            </div>

            {/* 리뷰 목록 */}
            {isLoadingReviews ? (
              <div className="text-center py-8 text-sm text-gray-400">리뷰를 불러오는 중...</div>
            ) : (
              <div className="space-y-4">
                {reviews.length === 0 ? (
                  <p className="text-center py-6 text-xs text-gray-400">아직 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요!</p>
                ) : (
                  reviews.map((review) => (
                    <div key={review.reviewId} className="pb-4 border-b border-gray-50 last:border-0">
                      <div className="flex items-start gap-2.5">
                        <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-0.5">
                          <span className="text-[10px] font-bold text-primary">
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
        </div>
      </div>

      {/* 고정 하단 바 */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 px-5 py-3 pb-5 z-50 max-w-[600px] mx-auto">
        <div className="flex gap-3">
          <button
            onClick={() => toggleWishlist(id)}
            className={`flex-1 py-3.5 rounded-xl text-sm font-bold border-2 transition-all active:scale-[0.98] ${
              isWishlisted
                ? 'bg-destructive/5 border-destructive text-destructive'
                : 'bg-gray-50 border-gray-200 text-gray-700 hover:border-gray-300'
            }`}
          >
            {isWishlisted ? '❤️ 찜 완료' : '🤍 찜하기'}
          </button>
          <button
            onClick={() => onNavigate?.('map', { placeId: id })}
            className="flex-1 py-3.5 rounded-xl text-sm font-bold bg-primary text-white text-center hover:bg-primary/90 active:scale-[0.98] transition-all"
          >
            🗺 길찾기
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
