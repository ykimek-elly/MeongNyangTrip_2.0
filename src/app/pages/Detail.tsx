import React, { useState } from 'react';
import { 
  ArrowLeft, Heart, Share2, MapPin, Star, ChevronRight, 
  Instagram, X, Copy, ExternalLink, Navigation
} from 'lucide-react';
// 더미 데이터 제거
import { AMENITIES, DETAIL_EXTRA, MOCK_REVIEWS, type Review } from '../data/detail-mock';
import { motion, AnimatePresence } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { ShareSheet } from '../components/ShareSheet';

interface DetailProps {
  id: number;
  onNavigate?: (page: string, params?: any) => void;
}

export function Detail({ id, onNavigate }: DetailProps) {
  const places = useAppStore((s) => s.places);
  const place = places.find(p => p.id === id);
  const [showShare, setShowShare] = useState(false);
  const [showMapSheet, setShowMapSheet] = useState(false);
  const [addressCopied, setAddressCopied] = useState(false);
  const [showReserved, setShowReserved] = useState(false);
  // 리뷰 관련 상태
  const [reviewRating, setReviewRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviews, setReviews] = useState<Review[]>(MOCK_REVIEWS);
  const [showFullDesc, setShowFullDesc] = useState(false);

  const { wishlist, toggleWishlist } = useAppStore();
  const isWishlisted = place ? wishlist.includes(id) : false;

  if (!place) return null;

  const extra = DETAIL_EXTRA[id] || {
    address: `${place.address} 상세 주소`,
    tags: ['#반려동물', '#여행'],
    description: `${place.title}은 반려동물과 함께 특별한 시간을 보낼 수 있는 곳입니다.`,
    instagram: '@meongnyang_trip',
  };

  // 리뷰 등록 핸들러
  const handleSubmitReview = () => {
    if (reviewRating === 0 || !reviewText.trim()) return;
    const newReview: Review = {
      id: Date.now(),
      author: '나',
      date: new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
      rating: reviewRating,
      content: reviewText.trim(),
    };
    setReviews([newReview, ...reviews]);
    setReviewRating(0);
    setReviewText('');
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
        <h1 className="text-[15px] font-bold text-gray-900 truncate max-w-[200px]">{place.title}</h1>
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
        <div className="w-full aspect-[4/3] bg-gray-100 overflow-hidden">
          <img 
            src={place.imageUrl || ""} 
            alt={place.title} 
            className="w-full h-full object-cover" 
          />
        </div>

        <div className="px-5">
          {/* 제목 + 평점 */}
          <div className="pt-5 pb-4 border-b border-gray-100">
            <h2 className="text-[20px] font-bold text-gray-900 mb-1.5">{place.title}</h2>
            <div className="flex items-center gap-1.5">
              <Star size={14} className="text-brand-point fill-brand-point" />
              <span className="text-xs font-bold text-gray-900">{place.rating}</span>
              <span className="text-xs text-gray-400">({place.reviewCount}개 리뷰)</span>
            </div>
          </div>

          {/* 시설 정보 */}
          <div className="py-4 border-b border-gray-100">
            <h3 className="text-[15px] font-bold text-gray-900 mb-3">시설 정보</h3>
            <div className="flex flex-wrap gap-2">
              {AMENITIES.map((item, idx) => (
                <div key={idx} className="flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-100">
                  <item.icon size={13} className="text-gray-400" />
                  <span className="text-[12px] text-gray-600">{item.label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* 소개 */}
          <div className="py-4 border-b border-gray-100">
            <h3 className="text-[15px] font-bold text-gray-900 mb-3">소개</h3>
            <div className="flex flex-wrap gap-1.5 mb-3">
              {extra.tags.map((tag, idx) => (
                <span key={idx} className="text-[12px] text-primary font-medium">{tag}</span>
              ))}
            </div>
            {showFullDesc ? (
              <p className="text-xs text-gray-600 leading-relaxed">{extra.description}</p>
            ) : null}
            <button
              onClick={() => setShowFullDesc(!showFullDesc)}
              className="text-primary text-xs font-bold mt-1 flex items-center gap-0.5 hover:underline"
            >
              {showFullDesc ? '접기' : '더보기'} <ChevronRight size={13} className={showFullDesc ? 'rotate-90' : ''} />
            </button>
          </div>

          {/* 주소 + 지도보기 */}
          <div className="py-4 border-b border-gray-100">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 min-w-0">
                <MapPin size={16} className="text-gray-400 shrink-0" />
                <span className="text-xs text-gray-600 truncate">{extra.address}</span>
              </div>
              <button 
                onClick={() => setShowMapSheet(true)}
                className="text-primary text-xs font-bold whitespace-nowrap flex items-center gap-0.5 ml-2 hover:underline"
              >
                지도보기 <ChevronRight size={13} />
              </button>
            </div>
          </div>

          {/* 인스타그램 */}
          {extra.instagram && (
            <div className="py-5 border-b border-gray-100">
              <button 
                onClick={() => alert(`인스타그램: ${extra.instagram}\n(데모용 정보입니다)`)}
                className="w-full flex flex-col items-center gap-2 py-2 hover:bg-gray-50 rounded-xl transition-colors"
              >
                <Instagram size={24} className="text-gray-500" />
                <span className="text-xs text-gray-600">인스타그램</span>
              </button>
            </div>
          )}

          {/* 리뷰 섹션 */}
          <div className="py-5">
            <h3 className="text-[15px] font-bold text-gray-900 mb-4">
              리뷰 <span className="text-primary">{reviews.length + place.reviewCount - MOCK_REVIEWS.length}</span>
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
              <button
                onClick={handleSubmitReview}
                disabled={reviewRating === 0 || !reviewText.trim()}
                className={`w-full mt-3 py-2.5 rounded-xl text-sm font-bold transition-colors ${
                  reviewRating > 0 && reviewText.trim()
                    ? 'bg-primary text-white hover:bg-primary/90 active:scale-[0.98]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                리뷰 등록하기
              </button>
            </div>

            {/* 리뷰 목록 */}
            <div className="space-y-4">
              {reviews.map((review) => (
                <div key={review.id} className="pb-4 border-b border-gray-50 last:border-0">
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-gray-800">{review.author}</span>
                    <span className="text-[11px] text-gray-400">{review.date}</span>
                  </div>
                  <div className="flex gap-0.5 mb-2">
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
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 고정 하단 바 */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 px-5 py-3 pb-5 z-50 max-w-[600px] mx-auto">
        <button
          onClick={() => {
            setShowReserved(true);
            setTimeout(() => setShowReserved(false), 2500);
          }}
          className="w-full bg-primary text-white font-bold text-[15px] py-3.5 rounded-xl hover:bg-primary/90 active:scale-[0.98] transition-all"
        >
          {showReserved ? '✅ 예약 요청이 접수되었습니다!' : '예약 / 문의하기'}
        </button>
      </div>

      {/* 공유 시트 */}
      <ShareSheet
        isOpen={showShare}
        onClose={() => setShowShare(false)}
        postId={id}
        postImage={place.imageUrl || ""}
        postUser={place.title}
      />

      {/* 위치 바텀시트 */}
      <AnimatePresence>
        {showMapSheet && (
          <div className="fixed inset-0 z-[60] flex items-end justify-center">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/50 backdrop-blur-sm"
              onClick={() => setShowMapSheet(false)}
            />
            <motion.div
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ type: 'spring', damping: 30, stiffness: 350 }}
              className="relative z-10 bg-white w-full max-w-[600px] rounded-t-3xl shadow-2xl overflow-hidden"
            >
              {/* 핸들바 */}
              <div className="flex justify-center pt-3 pb-1">
                <div className="w-10 h-1 bg-gray-300 rounded-full" />
              </div>

              {/* 헤더 */}
              <div className="flex items-center justify-between px-5 pb-3">
                <h3 className="text-[15px] font-bold text-gray-900">위치 정보</h3>
                <button
                  onClick={() => setShowMapSheet(false)}
                  className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
                >
                  <X size={20} />
                </button>
              </div>

              {/* 지도 미리보기 영역 */}
              <div className="mx-5 mb-4 relative h-[180px] rounded-2xl overflow-hidden bg-green-50 border border-gray-100">
                <div className="absolute inset-0 bg-gradient-to-br from-green-50 via-blue-50 to-green-50">
                  <div className="absolute inset-0 opacity-[0.15]">
                    <div className="absolute top-[20%] left-0 w-full h-px bg-gray-500" />
                    <div className="absolute top-[40%] left-[10%] w-[80%] h-px bg-gray-500" />
                    <div className="absolute top-[60%] left-0 w-full h-px bg-gray-500" />
                    <div className="absolute top-[80%] left-[5%] w-[70%] h-px bg-gray-500" />
                    <div className="absolute top-0 left-[30%] w-px h-full bg-gray-500" />
                    <div className="absolute top-0 left-[55%] w-px h-full bg-gray-500" />
                    <div className="absolute top-0 left-[80%] w-px h-full bg-gray-500" />
                  </div>
                  <div className="absolute top-[30%] left-[40%] w-[25%] h-[30%] bg-blue-200/40 rounded-full blur-sm" />
                </div>
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-full flex flex-col items-center">
                  <div className="w-10 h-10 bg-primary rounded-full flex items-center justify-center shadow-lg shadow-primary/30 border-2 border-white">
                    <MapPin size={20} className="text-white" />
                  </div>
                  <div className="w-2 h-2 bg-primary/40 rounded-full mt-1" />
                </div>
                <div className="absolute bottom-3 left-1/2 -translate-x-1/2 bg-white/90 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-sm border border-gray-100">
                  <span className="text-[11px] font-bold text-gray-800">{place.title}</span>
                </div>
              </div>

              {/* 주소 정보 */}
              <div className="mx-5 mb-4 bg-gray-50 rounded-2xl p-4">
                <div className="flex items-start gap-3">
                  <MapPin size={18} className="text-primary shrink-0 mt-0.5" />
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-bold text-gray-800 mb-0.5">{place.address}</p>
                    <p className="text-[12px] text-gray-500">{extra.address}</p>
                  </div>
                </div>
              </div>

              {/* 액션 버튼들 */}
              <div className="px-5 pb-3 grid grid-cols-3 gap-2">
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(extra.address);
                    setAddressCopied(true);
                    setTimeout(() => setAddressCopied(false), 2000);
                  }}
                  className="flex flex-col items-center gap-1.5 py-3 bg-gray-50 rounded-2xl border border-gray-100 hover:bg-gray-100 active:scale-95 transition-all"
                >
                  {addressCopied ? (
                    <>
                      <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                        <span className="text-green-600 text-xs">✓</span>
                      </div>
                      <span className="text-[11px] font-bold text-green-600">복사됨!</span>
                    </>
                  ) : (
                    <>
                      <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                        <Copy size={15} className="text-gray-600" />
                      </div>
                      <span className="text-[11px] font-bold text-gray-600">주소 복사</span>
                    </>
                  )}
                </button>
                <button
                  onClick={() => window.open(`https://map.kakao.com/link/search/${encodeURIComponent(extra.address)}`, '_blank')}
                  className="flex flex-col items-center gap-1.5 py-3 bg-yellow-50 rounded-2xl border border-yellow-100 hover:bg-yellow-100 active:scale-95 transition-all"
                >
                  <div className="w-8 h-8 bg-yellow-100 rounded-full flex items-center justify-center">
                    <ExternalLink size={15} className="text-yellow-700" />
                  </div>
                  <span className="text-[11px] font-bold text-yellow-700">카카오맵</span>
                </button>
                <button
                  onClick={() => window.open(`https://map.naver.com/v5/search/${encodeURIComponent(extra.address)}`, '_blank')}
                  className="flex flex-col items-center gap-1.5 py-3 bg-green-50 rounded-2xl border border-green-100 hover:bg-green-100 active:scale-95 transition-all"
                >
                  <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                    <Navigation size={15} className="text-green-700" />
                  </div>
                  <span className="text-[11px] font-bold text-green-700">네이버지도</span>
                </button>
              </div>

              <div className="h-6" />
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}