import React from 'react';
import Slider from 'react-slick';
import mainBanner1 from '../../assets/main_banner_1080_1.jpg';
import mainBanner2 from '../../assets/main_banner_1080_2.jpg';
import mainBanner3 from '../../assets/main_banner_1080_3.jpg';
import 'slick-carousel/slick/slick.css';
import 'slick-carousel/slick/slick-theme.css';
import { Search, MapPin, Calendar, Layers, TreeDeciduous, PawPrint, Dog, Cat, Tent, Coffee, Bed, ChevronRight, Star, Gift, Flame, Map, MessageCircle, Sparkles, Heart, Award, Camera, X, LogIn } from 'lucide-react';

import { PlaceImage } from '../components/PlaceImage';
// 더미 데이터 제거
import { motion, AnimatePresence } from 'motion/react';
import { CategoryBestRanking } from '../components/CategoryBestRanking';
import { DatePickerPopup } from '../components/DatePickerPopup';
import { useAppStore } from '../store/useAppStore';

interface HomeProps {
  onNavigate: (page: string, params?: any) => void;
}

export function Home({ onNavigate }: HomeProps) {
  const [searchRegion, setSearchRegion] = React.useState('');
  const [searchDate, setSearchDate] = React.useState('');
  const [searchCategory, setSearchCategory] = React.useState('all');
  const [showSignupPrompt, setShowSignupPrompt] = React.useState(false);
  const [showPetPrompt, setShowPetPrompt] = React.useState(false);
  const isLoggedIn = useAppStore((s) => s.isLoggedIn);
  const pets = useAppStore((s) => s.pets);
  const places = useAppStore((s) => s.places);

  // 로그인 상태인데 펫이 없으면 알림 표시
  React.useEffect(() => {
    if (isLoggedIn && pets.length === 0) {
      const timer = setTimeout(() => setShowPetPrompt(true), 800);
      return () => clearTimeout(timer);
    }
  }, [isLoggedIn, pets.length]);

  const handleBannerClick = (page: string) => {
    if (isLoggedIn) {
      onNavigate(page);
    } else {
      setShowSignupPrompt(true);
    }
  };

  const handleSearch = () => {
    onNavigate('list', { region: searchRegion, date: searchDate, category: searchCategory });
  };

  const sliderSettings = {
    dots: true,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 4000,
    arrows: false,
    appendDots: (dots: any) => (
      <div style={{ position: 'absolute', bottom: '16px', width: '100%', display: 'flex', justifyContent: 'center', zIndex: 10, pointerEvents: 'none' }}>
        <ul style={{ display: 'flex', gap: '6px', margin: 0, padding: 0, listStyle: 'none', pointerEvents: 'auto' }}>{dots}</ul>
      </div>
    ),
    customPaging: (i: number) => (
      <div className="w-2 h-2 rounded-full bg-white/50 transition-all duration-300 cursor-pointer [.slick-active_&]:bg-white [.slick-active_&]:w-5" />
    ),
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ type: 'spring', damping: 28, stiffness: 320 }}
    >
      {/* 검색 & 배너 영역 */}
      <div className="mx-[0px] mt-[0px] mb-[12px] px-[16px] py-[0px]">
        {/* 히어로 배너 */}
        <div className="relative h-[280px] rounded-[2rem] overflow-hidden shadow-xl mb-6 bg-black [&_.slick-slider]:!mb-0 [&_.slick-slider]:h-full [&_.slick-list]:h-full [&_.slick-track]:flex [&_.slick-track]:h-full [&_.slick-slide>div]:h-full [&_.slick-slide]:h-full">
          <Slider {...sliderSettings} className="h-full">
            {/* 슬라이드 1: 플레이스 */}
            <div className="relative h-[280px] w-full outline-none group cursor-pointer" onClick={() => onNavigate('list', { category: 'PLACE' })}>
              <img
                src={mainBanner1}
                className="block w-full h-full object-cover transition-spring group-hover:scale-105"
                alt="멍냥플레이스"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent flex flex-col justify-end px-7 pt-7 pb-12">
                <span className="inline-flex items-center gap-1 bg-green-500 text-white text-[10px] font-bold px-2.5 py-1 rounded-full w-fit mb-3 shadow-sm">
                  <TreeDeciduous size={12} strokeWidth={2.5} /> 멍냥플레이스
                </span>
                <h3 className="text-white text-2xl font-bold leading-tight">
                  자연 속에서 즐기는<br />
                  <span className="text-white/80">신나는 야외 산책</span> 코스
                </h3>
              </div>
            </div>

            {/* 슬라이드 2: 스테이 */}
            <div className="relative h-[280px] w-full outline-none group cursor-pointer" onClick={() => onNavigate('list', { category: 'STAY' })}>
              <img
                src={mainBanner2}
                className="block w-full h-full object-cover transition-spring group-hover:scale-105"
                alt="멍냥스테이"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent flex flex-col justify-end px-7 pt-7 pb-12">
                <span className="inline-flex items-center gap-1 bg-brand-point text-black text-[10px] font-bold px-2.5 py-1 rounded-full w-fit mb-3 shadow-sm">
                  <Bed size={12} strokeWidth={2.5} /> 멍냥스테이
                </span>
                <h3 className="text-white text-2xl font-bold leading-tight">
                  반려동물과 함께 머무는<br />
                  <span className="text-brand-point">프리미엄 힐링</span> 숙소
                </h3>
              </div>
            </div>

            {/* 슬라이드 3: 다이닝 */}
            <div className="relative h-[280px] w-full outline-none group cursor-pointer" onClick={() => onNavigate('list', { category: 'DINING' })}>
              <img
                src={mainBanner3}
                className="block w-full h-full object-cover transition-spring group-hover:scale-105"
                alt="멍냥다이닝"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent flex flex-col justify-end px-7 pt-7 pb-12">
                <span className="inline-flex items-center gap-1 bg-primary text-white text-[10px] font-bold px-2.5 py-1 rounded-full w-fit mb-3 shadow-sm">
                  <Coffee size={12} strokeWidth={2.5} /> 멍냥다이닝
                </span>
                <h3 className="text-white text-2xl font-bold leading-tight">
                  눈치 보지 않고 즐기는<br />
                  <span className="text-white/80">동반 가능</span> 맛집 & 카페
                </h3>
              </div>
            </div>
          </Slider>
        </div>

        {/* 검색박스 */}
        <div className="bg-white rounded-[2rem] px-[24px] py-[20px]">

          <div className="flex gap-2 h-[3.25rem]">
            <div className="bg-gray-50 border border-gray-200 rounded-2xl px-3 flex items-center gap-2 flex-1 min-w-0 focus-within:border-primary focus-within:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] transition-spring">
              <Search className="text-gray-400 shrink-0" size={18} />
              <input
                type="text"
                placeholder="지역 검색 (서울·경기)"
                className="bg-transparent w-full outline-none text-gray-800 placeholder:text-gray-400 font-medium text-sm focus:placeholder:text-gray-300"
                value={searchRegion}
                onChange={(e) => setSearchRegion(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <DatePickerPopup
              value={searchDate}
              onChange={(date) => setSearchDate(date)}
            />
            <button
              className="bg-primary text-white rounded-2xl aspect-square h-full flex items-center justify-center shadow-md hover:scale-[1.05] active:scale-[0.95] transition-spring shrink-0"
              onClick={handleSearch}
            >
              <Search size={20} strokeWidth={2.5} />
            </button>
          </div>
        </div>
      </div>

      {/* 카테고리 네비게이션 */}
      <div className="px-4 mb-8">
        <div className="flex justify-start md:justify-center gap-4 overflow-x-auto scrollbar-hide snap-x px-[4px] pt-[0px] pb-[10px]">
          <div className="snap-start shrink-0">
            <CategoryItem
              icon={Sparkles}
              label="AI 추천산책"
              onClick={() => onNavigate('ai-walk-guide')}
              className="[&>div]:!bg-green-50 [&>div]:!text-green-500 [&>span]:!text-gray-800 [&>span]:!font-bold"
            />
          </div>
          <div className="snap-start shrink-0"><CategoryItem icon={PawPrint} label="멍냥플레이스" onClick={() => onNavigate('list', { category: 'PLACE' })} /></div>
          <div className="snap-start shrink-0"><CategoryItem icon={Dog} label="멍냥스테이" onClick={() => onNavigate('list', { category: 'STAY' })} /></div>
          <div className="snap-start shrink-0"><CategoryItem icon={Cat} label="멍냥다이닝" onClick={() => onNavigate('list', { category: 'DINING' })} /></div>
          <div className="snap-start shrink-0"><CategoryItem icon={Map} label="멍냥지도" onClick={() => onNavigate('map')} /></div>
          <div className="snap-start shrink-0"><CategoryItem icon={MessageCircle} label="멍냥라운지" onClick={() => onNavigate('lounge')} /></div>
        </div>
      </div>

      {/* 주말 추천 */}
      <div className="pl-4 mb-10 animate-fade-in-up" style={{ animationDelay: '0.1s' }}>
        <div className="flex justify-between items-end pr-4 mb-4">
          <h3 className="text-lg font-bold text-gray-800 leading-snug">이번 주말, 여기 어때요?</h3>
          <span className="text-gray-500 text-xs flex items-center cursor-pointer transition-spring hover:text-gray-800" onClick={() => onNavigate('list')}>
            더보기 <ChevronRight size={12} />
          </span>
        </div>

        <Slider
          dots={false}
          infinite={true}
          speed={500}
          slidesToShow={1}
          slidesToScroll={1}
          autoplay={true}
          autoplaySpeed={3000}
          arrows={false}
          variableWidth={true}
          swipeToSlide={true}
          cssEase="cubic-bezier(0.16, 1, 0.3, 1)"
          className="[&_.slick-track]:flex [&_.slick-track]:gap-4 [&_.slick-slide]:!w-auto [&_.slick-slide>div]:h-full"
        >
          {places.slice(0, 8).map((place, idx) => (
            <div
              key={place.id}
              className="!w-[160px] pr-4 cursor-pointer hover:scale-[1.02] active:scale-[0.97] transition-spring"
              onClick={() => onNavigate('detail', { id: place.id })}
            >
              <div className="relative mb-2">
                <div className="absolute top-2 left-2 bg-primary text-white w-6 h-6 flex items-center justify-center rounded-md font-bold text-xs shadow-md z-10">
                  {idx + 1}
                </div>
                <PlaceImage imageUrl={place.imageUrl} category={place.category} className="w-full aspect-square object-cover rounded-2xl" iconSize={32} alt={place.title} />
              </div>
              <h6 className="text-gray-800 truncate text-[15px] font-bold">{place.title}</h6>
              <div className="text-xs flex items-center gap-0.5">
                {place.reviewCount > 0 ? (
                  <span className="flex items-center text-gray-800 font-bold gap-0.5">
                    <Star size={10} className="fill-brand-point text-brand-point" /> {place.rating}
                    <span className="text-gray-400 font-normal">({place.reviewCount})</span>
                  </span>
                ) : place.aiRating ? (
                  <span className="flex items-center text-gray-900 font-bold gap-0.5">
                    <Star size={10} className="fill-[#008BFF] text-[#008BFF]" /> {place.aiRating.toFixed(1)}
                    <span className="text-[8px] font-normal text-gray-400">[AI]</span>
                  </span>
                ) : (
                  <PawPrint size={10} className="text-gray-300" />
                )}
              </div>
            </div>
          ))}
        </Slider>
      </div>

      {/* 개인 맞춤 서비스 (Pill 버튼) */}


      {/* 카테고리별 베스트 랭킹 */}
      <CategoryBestRanking places={places} onNavigate={onNavigate} />

      {/* 프로모션 배너 — Double-Bezel */}
      <div className="px-4 grid grid-cols-2 gap-2.5 animate-fade-in-up" style={{ animationDelay: '0.2s' }}>
        {/* 첫 리뷰 이벤트 */}
        <div
          className="p-1 bg-primary/5 rounded-[1.6rem] ring-1 ring-primary/10 cursor-pointer active:scale-[0.97] transition-spring"
          onClick={() => handleBannerClick('lounge')}
        >
          <div className="bg-primary/10 hover:bg-primary/15 transition-spring rounded-[1.25rem] p-3.5 flex flex-col items-start h-full">
            <div className="w-full flex justify-end mb-2">
              <Gift size={28} className="text-primary" />
            </div>
            <h6 className="font-bold text-primary text-xs mb-1 leading-snug">첫 리뷰 이벤트</h6>
            <p className="m-0 text-[12px] text-gray-500 leading-snug">산책 인증샷 올리면 간식 쿠폰 100% 증정!</p>
          </div>
        </div>

        {/* 방문인증센터 배너 */}
        <div
          className="p-1 bg-brand-point/5 rounded-[1.6rem] ring-1 ring-brand-point/10 cursor-pointer active:scale-[0.97] transition-spring"
          onClick={() => handleBannerClick('visit-checkin')}
        >
          <div className="bg-brand-point/10 hover:bg-brand-point/15 transition-spring rounded-[1.25rem] p-3.5 flex flex-col items-start h-full">
            <div className="w-full flex justify-end mb-2">
              <Camera size={28} className="text-brand-point" />
            </div>
            <h6 className="font-bold text-brand-point text-xs mb-1 leading-snug">방문인증센터</h6>
            <p className="m-0 text-[12px] text-gray-500 leading-snug flex items-center gap-1">방문 인증하고 포인트 받자! <Gift size={12} className="text-brand-point" /></p>
          </div>
        </div>
      </div>

      {/* 회원가입 유도 모달 */}
      <AnimatePresence>
        {showSignupPrompt && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center px-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/50 backdrop-blur-sm"
              onClick={() => setShowSignupPrompt(false)}
            />
            <motion.div
              initial={{ scale: 0.85, opacity: 0, y: 30 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.85, opacity: 0, y: 30 }}
              transition={{ type: 'spring', damping: 25, stiffness: 350 }}
              className="bg-white w-full max-w-[320px] rounded-3xl p-6 relative z-10 shadow-2xl text-center"
            >
              <button
                onClick={() => setShowSignupPrompt(false)}
                className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>

              <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                <LogIn size={28} className="text-primary" />
              </div>

              <h3 className="text-lg font-bold text-gray-800 mb-2">회원가입 후 이용 가능합니다</h3>
              <p className="text-sm text-gray-500 mb-6 leading-relaxed">
                이 서비스는 회원 전용 기능입니다.<br />
                간편 가입 후 바로 이용해 보세요!
              </p>

              <div className="flex gap-2">
                <button
                  onClick={() => setShowSignupPrompt(false)}
                  className="flex-1 py-3 rounded-xl bg-gray-100 text-gray-500 font-bold text-sm hover:bg-gray-200 hover:scale-[1.02] active:scale-[0.98] transition-spring"
                >
                  닫기
                </button>
                <button
                  onClick={() => {
                    setShowSignupPrompt(false);
                    onNavigate('signup');
                  }}
                  className="flex-1 py-3 rounded-xl bg-primary text-white font-bold text-sm hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.98] transition-spring"
                >
                  회원가입하기
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 반려동물 미등록 알림 토스트 */}
      <AnimatePresence>
        {showPetPrompt && (
          <motion.div
            initial={{ opacity: 0, y: 60 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 60 }}
            transition={{ type: 'spring', damping: 26, stiffness: 300 }}
            className="fixed bottom-28 inset-x-0 flex justify-center px-4 z-50 pointer-events-none"
          >
            <div className="w-full max-w-[560px] bg-gray-900 text-white rounded-2xl px-4 py-3.5 shadow-xl flex items-center gap-3 pointer-events-auto">
              <div className="w-9 h-9 bg-primary/20 rounded-full flex items-center justify-center shrink-0">
                <PawPrint size={18} className="text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold">반려동물을 등록해보세요!</p>
                <p className="text-xs text-gray-400 mt-0.5">맞춤 장소 추천을 받을 수 있어요</p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => { setShowPetPrompt(false); onNavigate('mypage'); }}
                  className="text-xs font-bold text-primary bg-primary/10 px-3 py-1.5 rounded-full hover:bg-primary/20 transition-colors"
                >
                  등록하기
                </button>
                <button onClick={() => setShowPetPrompt(false)} className="text-gray-400 hover:text-white transition-colors">
                  <X size={16} />
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function CategoryItem({ icon: Icon, label, onClick, className }: { icon: any, label: string, onClick: () => void, className?: string }) {
  return (
    <div className={`flex flex-col items-center gap-2 group cursor-pointer flex-shrink-0 min-w-[70px] ${className || ''}`} onClick={onClick}>
      <div className="w-14 h-14 bg-primary/10 text-primary rounded-[28px] flex items-center justify-center transition-spring group-hover:scale-[1.08] group-hover:bg-primary/20 group-active:scale-[0.92]">
        <Icon size={28} strokeWidth={1.5} />
      </div>
      <span className="text-[11px] font-medium text-gray-500 transition-spring group-hover:text-gray-800 leading-snug">{label}</span>
    </div>
  );
}