import React, { useState, useEffect } from 'react';
import {
  ArrowLeft, Heart, Share2, MapPin, Star, ChevronRight,
  Globe, Phone
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
  // 리뷰 관련 상태
  const [reviewRating, setReviewRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [reviewCount, setReviewCount] = useState(0);
  const [avgRating, setAvgRating] = useState(0);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [showFullDesc, setShowFullDesc] = useState(false);

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
  
  // 주소: addr2 있으면 합산
  const fullAddress = place.addr2
    ? `${place.address} ${place.addr2}`.trim()
    : place.address;

  // 반려동물 정책 카드 표시 여부
  const hasPetPolicy = place.chkPetInside || place.accomCountPet?.trim() || place.petTurnAdroose || place.tags;

  // 하이라이트 3가지 (Airbnb 방식, DB 필드 기반 자동 생성)
  const highlights: string[] = [];
  if (place.chkPetInside === 'Y') highlights.push('실내 반려동물 동반 가능');
  else if (place.chkPetInside === 'N') highlights.push('전용 야외 공간 동반 가능');
  if (place.tags?.toLowerCase().includes('대형견')) highlights.push('대형견 환영');
  else if (place.tags?.toLowerCase().includes('소형견')) highlights.push('소형견 동반 가능');
  if (place.phone || place.homepage) highlights.push('문의 및 예약 가능');
  if (place.aiRating && place.aiRating >= 4.0) highlights.push('AI 추천 우수 장소');
  if (reviewCount > 0) highlights.push(`${reviewCount}개의 실방문 리뷰`);
  if (place.overview) highlights.push('상세 소개 정보 제공');

  // 소개 태그: place.tags(DB) 우선, 없으면 카테고리 기반 1개
  const descTags: string[] = place.tags
    ? place.tags.split(',').map((t: string) => t.trim()).filter(Boolean)
    : [CATEGORY_TAG[place.category] ?? '#반려동물여행'];

  const extra = {
    address: fullAddress,
    overview: place.overview || null,
  };

  // 좌표: PlaceDto(latitude/longitude) 또는 mock 데이터(lat/lng) 모두 처리
  const placeLat: number | undefined = place.latitude ?? (place as any).lat;
  const placeLng: number | undefined = place.longitude ?? (place as any).lng;
  const hasCoords = typeof placeLat === 'number' && typeof placeLng === 'number';

  // 지도 앱 바로가기 URL
  const mapLinks = {
    // 좌표 있으면 정확한 핀, 없으면 상호명 검색 (주소 제외 — 주소 포함 시 "결과 없음" 가능)
    kakao: hasCoords
      ? `https://map.kakao.com/link/map/${encodeURIComponent(displayTitle)},${placeLat},${placeLng}`
      : `https://map.kakao.com/link/search/${encodeURIComponent(displayTitle)}`,
    naver: hasCoords
      ? `https://map.naver.com/v5/search/${encodeURIComponent(displayTitle)}?c=${placeLng},${placeLat},15,0,0,0,dh`
      : `https://map.naver.com/v5/search/${encodeURIComponent(displayTitle)}`,
    google: hasCoords
      ? `https://www.google.com/maps/search/${encodeURIComponent(displayTitle)}/@${placeLat},${placeLng},15z`
      : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(displayTitle)}`,
  };

  // 리뷰 등록 핸들러
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
      {/* 상단 헤더 */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-12 flex items-center justify-between max-w-[600px] mx-auto w-full">
        <button 
          onClick={() => onNavigate && onNavigate('home')} 
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
        {/* 메인 이미지 — 16:9 */}
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
                <span className="text-[10px] text-gray-400">[AI 추천]</span>
              </div>
            ) : (
              <div className="flex items-center gap-1.5">
                <span className="text-xs text-gray-400">🐾 첫 리뷰를 남겨주세요!</span>
              </div>
            )}
          </div>

          {/* 하이라이트 */}
          {highlights.length > 0 && (
            <div className="py-4 border-b border-gray-100">
              <div className="space-y-2">
                {highlights.slice(0, 3).map((item, idx) => (
                  <div key={idx} className="flex items-center gap-2.5">
                    <div className="w-4 h-4 rounded-full bg-primary flex items-center justify-center shrink-0">
                      <span className="text-white text-[10px] font-bold">✓</span>
                    </div>
                    <span className="text-[13px] text-gray-700">{item}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 반려동물 정책 카드 */}
          {hasPetPolicy && (
            <div className="py-4 border-b border-gray-100">
              <h3 className="text-[15px] font-bold text-gray-900 mb-3">🐾 반려동물 정책</h3>
              <div className="flex flex-wrap gap-2 mb-2">
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
                {place.accomCountPet?.trim() && (
                  <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 px-3 py-2 rounded-xl">
                    <span className="text-sm">🐾</span>
                    <span className="text-[12px] font-medium text-gray-700">수용 {place.accomCountPet}</span>
                  </div>
                )}
              </div>
              {place.tags && (
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {place.tags.split(',').map((t: string, i: number) => (
                    <span key={i} className="text-[11px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{t.trim()}</span>
                  ))}
                </div>
              )}
              {place.petTurnAdroose && (
                <div className="bg-primary/5 border border-primary/10 rounded-xl p-3 mt-2">
                  <p className="text-[11px] font-bold text-primary mb-1">📋 동반 안내</p>
                  <p className="text-xs text-gray-600 leading-relaxed">{place.petTurnAdroose}</p>
                </div>
              )}
            </div>
          )}

          {/* 소개 */}
          <div className="py-4 border-b border-gray-100">
            <h3 className="text-[15px] font-bold text-gray-900 mb-3">소개</h3>
            <div className="flex flex-wrap gap-1.5 mb-3">
              {descTags.map((tag, idx) => (
                <span key={idx} className="text-[12px] text-primary font-medium">{tag}</span>
              ))}
            </div>
            {extra.overview && (
              <div className="space-y-2">
                <p className={`text-xs text-gray-600 leading-relaxed ${!showFullDesc ? 'line-clamp-2' : ''}`}>
                  {extra.overview}
                </p>
                {extra.overview.length > 80 && (
                  <button
                    onClick={() => setShowFullDesc(!showFullDesc)}
                    className="text-primary text-xs font-bold flex items-center gap-0.5 hover:underline"
                  >
                    {showFullDesc ? '접기' : '더보기'} <ChevronRight size={13} className={showFullDesc ? 'rotate-90' : ''} />
                  </button>
                )}
              </div>
            )}
          </div>

          {/* 오시는길 */}
          <div className="py-4 border-b border-gray-100">
            <h3 className="text-[15px] font-bold text-gray-900 mb-3">오시는길</h3>
            <div className="flex items-start gap-2 mb-3">
              <MapPin size={14} className="text-gray-400 shrink-0 mt-0.5" />
              <span className="text-xs text-gray-600 leading-relaxed">{extra.address}</span>
            </div>
            <div className="flex gap-2">
              <a
                href={mapLinks.kakao}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 text-center py-2 text-[11px] font-bold rounded-lg bg-[#FEE500] text-gray-900 active:scale-95 transition-transform"
              >
                카카오맵
              </a>
              <a
                href={mapLinks.naver}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 text-center py-2 text-[11px] font-bold rounded-lg bg-[#03C75A] text-white active:scale-95 transition-transform"
              >
                네이버맵
              </a>
              <a
                href={mapLinks.google}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 text-center py-2 text-[11px] font-bold rounded-lg bg-[#4285F4] text-white active:scale-95 transition-transform"
              >
                구글맵
              </a>
            </div>
          </div>

          {/* 연락처 */}
          {(place.homepage || place.phone) && (
            <div className="py-4 border-b border-gray-100">
              <h3 className="text-[15px] font-bold text-gray-900 mb-3">연락처</h3>
              <div className="flex gap-3">
                {place.homepage && (
                  <button
                    onClick={() => window.open(place.homepage!, '_blank')}
                    className="flex-1 flex flex-col items-center gap-1.5 py-3 bg-primary/5 rounded-2xl border border-primary/10 hover:bg-primary/10 active:scale-95 transition-all"
                  >
                    <Globe size={20} className="text-primary" />
                    <span className="text-[11px] font-bold text-primary">홈페이지</span>
                  </button>
                )}
                {place.phone && (
                  <button
                    onClick={() => window.open(`tel:${place.phone}`)}
                    className="flex-1 flex flex-col items-center gap-1.5 py-3 bg-gray-50 rounded-2xl border border-gray-100 hover:bg-gray-100 active:scale-95 transition-all"
                  >
                    <Phone size={20} className="text-gray-600" />
                    <span className="text-[11px] font-bold text-gray-600">{place.phone}</span>
                  </button>
                )}
              </div>
            </div>
          )}

          {/* 리뷰 섹션 */}
          <div className="py-5">
            <h3 className="text-[15px] font-bold text-gray-900 mb-4">
              리뷰 <span className="text-primary">{reviewCount}</span>
            </h3>

            {/* 리뷰 작성 카드 */}
            <div className="border border-gray-200 rounded-2xl p-4 mb-5">
              <div className="flex items-center gap-1 mb-2">
                <span className="text-[11px] text-primary font-bold mr-1">✍️</span>
                <span className="text-xs font-bold text-gray-800">리뷰 작성하기</span>
              </div>

              {/* 별점 선택 */}
              <div className="flex items-center gap-1 mb-3">
                <span className="text-[12px] text-gray-500 mr-1">방문은 어떠셨나요?</span>
                <div className="flex gap-0.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      onClick={() => setReviewRating(star)}
                      className="p-0.5"
                    >
                      <Star 
                        size={18} 
                        className={star <= reviewRating 
                          ? "text-brand-point fill-brand-point" 
                          : "text-gray-300"
                        } 
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* 텍스트 입력 */}
              <textarea
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="이 장소에 대한 솔직한 후기를 남겨주세요."
                className="w-full h-[72px] bg-gray-50 rounded-xl px-3 py-2.5 text-xs text-gray-700 placeholder:text-gray-400 border border-gray-100 resize-none outline-none focus:border-primary/50 transition-colors"
              />

              {/* 등록 버튼 */}
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
                                className={star <= review.rating
                                  ? "text-brand-point fill-brand-point"
                                  : "text-gray-200"
                                }
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

      {/* 고정 하단 바 — 항상 표시 */}
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
          <a
            href={mapLinks.kakao}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 py-3.5 rounded-xl text-sm font-bold bg-primary text-white text-center hover:bg-primary/90 active:scale-[0.98] transition-all"
          >
            🗺 길찾기
          </a>
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

    </motion.div>
  );
}