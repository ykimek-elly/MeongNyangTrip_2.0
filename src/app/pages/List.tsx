import React, { useState, useEffect, useRef } from 'react';
import { Star, MapPin, Search, LayoutGrid, List as ListIcon, ArrowLeft, ChevronDown } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { motion } from 'motion/react';
import { PlaceImage } from '../components/PlaceImage';

import { DatePickerPopup } from '../components/DatePickerPopup';

interface ListProps {
  onNavigate: (page: string, params?: any) => void;
  initialParams?: { region?: string; date?: string; category?: string };
}

type SortKey = 'latest' | 'rating' | 'review';
const SORT_LABELS: Record<SortKey, string> = {
  latest: '최신순',
  rating: '평점순',
  review: '리뷰순',
};

export function List({ onNavigate, initialParams }: ListProps) {
  const places = useAppStore((s) => s.places);
  const fetchPlaces = useAppStore((s) => s.fetchPlaces);
  const [activeFilter, setActiveFilter] = useState('all');
  const [filteredPlaces, setFilteredPlaces] = useState(places);
  const [displayCount, setDisplayCount] = useState(50);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const [searchMsg, setSearchMsg] = useState('');
  const [viewMode, setViewMode] = useState<'list' | 'grid'>('list');
  const [sortKey, setSortKey] = useState<SortKey>('latest');
  const [showSortMenu, setShowSortMenu] = useState(false);
  // 헤더 검색 상태
  const [searchRegion, setSearchRegion] = useState(initialParams?.region ?? '');
  const [searchDate, setSearchDate] = useState(initialParams?.date ?? '');

  useEffect(() => {
    if (places.length === 0) fetchPlaces();
  }, []);

  useEffect(() => {
    if (initialParams) {
      const { region = '', date, category = 'all' } = initialParams;
      
      const filtered = places.filter(p => {
        const matchRegion = region === '' || p.address.toLowerCase().includes(region.toLowerCase()) || p.title.toLowerCase().includes(region.toLowerCase());
        const matchCategory = category === 'all' || p.category === category;
        return matchRegion && matchCategory;
      });

      setFilteredPlaces(filtered);
      
      if (region || date || category !== 'all') {
        let msg = `검색 결과: ${filtered.length}건`;
        if (date) msg += ` • ${date} 예약가능`;
        setSearchMsg(msg);
      }
      
      if (category !== 'all') setActiveFilter(category);
    } else {
      setFilteredPlaces(places);
    }
  }, [initialParams, places]);

  // 필터/정렬 변경 시 displayCount 리셋
  useEffect(() => {
    setDisplayCount(50);
  }, [filteredPlaces]);

  // 무한 스크롤: sentinel이 뷰포트에 들어오면 50건 추가 로드
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setDisplayCount(prev => Math.min(prev + 50, filteredPlaces.length));
        }
      },
      { threshold: 0.1 }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [filteredPlaces.length, displayCount]);

  const getEffectiveRating = (p: typeof places[0]) =>
    p.reviewCount > 0 ? p.rating : (p.aiRating ?? 0);

  const getEffectiveReviewCount = (p: typeof places[0]) => p.reviewCount;

  const applySortAndFilter = (list: typeof places, sort: SortKey) => {
    const sorted = [...list].sort((a, b) => {
      if (sort === 'rating') return getEffectiveRating(b) - getEffectiveRating(a);
      if (sort === 'review') return getEffectiveReviewCount(b) - getEffectiveReviewCount(a);
      return b.id - a.id; // latest: id 내림차순
    });
    return sorted;
  };

  const handleSearch = () => {
    const region = searchRegion.trim();
    const filtered = places.filter(p => {
      const matchRegion = region === '' || p.address.toLowerCase().includes(region.toLowerCase()) || p.title.toLowerCase().includes(region.toLowerCase());
      const matchCategory = activeFilter === 'all' || p.category === activeFilter;
      return matchRegion && matchCategory;
    });
    setFilteredPlaces(applySortAndFilter(filtered, sortKey));
    setSearchMsg(`검색 결과: ${filtered.length}건${searchDate ? ` • ${searchDate} 예약가능` : ''}`);
  };

  const handleSortChange = (key: SortKey) => {
    setSortKey(key);
    setShowSortMenu(false);
    setFilteredPlaces(prev => applySortAndFilter(prev, key));
  };

  const handleFilterClick = (cat: string) => {
    setActiveFilter(cat);
    if (cat === 'all') {
      setFilteredPlaces(applySortAndFilter(places, sortKey));
      setSearchMsg('');
    } else {
      const filtered = places.filter(p => p.category === cat);
      setFilteredPlaces(applySortAndFilter(filtered, sortKey));
      setSearchMsg(`검색 결과: ${filtered.length}건`);
    }
  };

  const toggleView = () => {
    setViewMode(prev => prev === 'list' ? 'grid' : 'list');
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-screen bg-white pb-24"
    >
      {/* 검색 헤더 */}
      <div className="sticky top-0 z-10 bg-white border-b border-gray-100">
        <div className="flex items-center gap-2 px-4 py-2.5">
          <button
            onClick={() => onNavigate('home')}
            className="p-1.5 -ml-1 text-gray-700 hover:bg-gray-100 rounded-full shrink-0"
            aria-label="뒤로가기"
          >
            <ArrowLeft size={20} />
          </button>
          <div className="flex gap-2 flex-1 h-10">
            <div className="bg-gray-50 border border-gray-200 rounded-xl px-3 flex items-center gap-2 flex-1 min-w-0">
              <Search size={14} className="text-gray-400 shrink-0" />
              <input
                type="text"
                placeholder="지역/숙소명"
                className="bg-transparent w-full outline-none text-gray-800 placeholder:text-gray-400 font-medium text-sm"
                value={searchRegion}
                onChange={(e) => setSearchRegion(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <DatePickerPopup value={searchDate} onChange={setSearchDate} />
            <button
              onClick={handleSearch}
              className="bg-primary text-white rounded-xl w-10 h-10 flex items-center justify-center active:scale-95 transition-all shrink-0"
              aria-label="검색"
            >
              <Search size={16} strokeWidth={2.5} />
            </button>
          </div>
        </div>

        {/* 1행: 필터 탭 */}
        <div className="flex gap-2 overflow-x-auto px-4 pb-3 scrollbar-hide">
          <FilterButton label="전체" active={activeFilter === 'all'} onClick={() => handleFilterClick('all')} />
          <FilterButton label="🏞️ 멍냥플레이스" active={activeFilter === 'PLACE'} onClick={() => handleFilterClick('PLACE')} />
          <FilterButton label="🏡 멍냥스테이" active={activeFilter === 'STAY'} onClick={() => handleFilterClick('STAY')} />
          <FilterButton label="🍽️ 멍냥다이닝" active={activeFilter === 'DINING'} onClick={() => handleFilterClick('DINING')} />
        </div>

        {/* 2행: 정렬 + 카운트 + 뷰토글 */}
        <div className="flex items-center justify-between px-4 pb-2.5">
          <div className="relative">
            <button
              onClick={() => setShowSortMenu(prev => !prev)}
              className="flex items-center gap-1 text-xs font-medium text-gray-600 hover:text-gray-900"
            >
              {SORT_LABELS[sortKey]} <ChevronDown size={13} />
            </button>
            {showSortMenu && (
              <div className="absolute top-full left-0 mt-1 bg-white border border-gray-100 rounded-xl shadow-lg z-20 py-1 min-w-[90px]">
                {(Object.keys(SORT_LABELS) as SortKey[]).map(key => (
                  <button
                    key={key}
                    onClick={() => handleSortChange(key)}
                    className={`w-full text-left px-3 py-2 text-xs font-medium hover:bg-gray-50 ${sortKey === key ? 'text-primary' : 'text-gray-700'}`}
                  >
                    {SORT_LABELS[key]}
                  </button>
                ))}
              </div>
            )}
          </div>
          <span className="text-xs text-gray-400 flex-1 text-center">
            {searchMsg || `전체 ${filteredPlaces.length}건`}
          </span>
          <button
            onClick={toggleView}
            className="p-1.5 text-gray-500 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
            aria-label={viewMode === 'list' ? "그리드 뷰로 전환" : "리스트 뷰로 전환"}
          >
            {viewMode === 'list' ? <LayoutGrid size={18} /> : <ListIcon size={18} />}
          </button>
        </div>
      </div>

      {/* 목록 컨테이너 */}
      <div className={viewMode === 'list' ? "px-4 py-4 space-y-3" : "px-4 py-4 grid grid-cols-2 gap-3"}>
        {filteredPlaces.length === 0 ? (
          <div className="col-span-full flex flex-col items-center justify-center py-20 text-gray-400 opacity-75">
            <Search size={48} className="mb-4 text-gray-300" />
            <p>조건에 맞는 장소가 없어요.</p>
          </div>
        ) : (
          filteredPlaces.slice(0, displayCount).map(place => (
            viewMode === 'list' ? (
              // 리스트 뷰 아이템
              <div
                key={place.id}
                className="flex items-center bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer"
                onClick={() => onNavigate('detail', { id: place.id })}
              >
                <PlaceImage
                  imageUrl={place.imageUrl}
                  category={place.category}
                  className="w-[90px] h-[90px] rounded-2xl object-cover flex-shrink-0"
                />
                <div className="ml-4 flex-1 min-w-0">
                  <div className="flex justify-between items-start">
                    <h6 className="font-bold text-gray-900 mb-1 truncate">{place.title}</h6>
                    {place.reviewCount > 0 ? (
                      <span className="flex items-center text-brand-point text-xs font-bold gap-0.5 shrink-0">
                        <Star size={12} className="fill-brand-point" /> {place.rating.toFixed(1)}
                      </span>
                    ) : place.aiRating ? (
                      <span className="flex items-center text-gray-900 text-xs font-bold gap-0.5 shrink-0">
                        <Star size={12} className="fill-[#008BFF] text-[#008BFF]" /> {place.aiRating.toFixed(1)}
                        <span className="text-[9px] font-normal text-gray-400">[AI]</span>
                      </span>
                    ) : (
                      <span className="text-gray-400 text-[10px] shrink-0">🐾</span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mb-2 flex items-center gap-1">
                    <MapPin size={10} /> {place.address}
                  </p>
                  <span className="inline-block bg-primary/10 text-primary border border-primary/30 text-[10px] font-medium px-2 py-0.5 rounded-full">
                    {place.category.toUpperCase()}
                  </span>
                </div>
              </div>
            ) : (
              // 그리드 뷰 아이템
              <div
                key={place.id}
                onClick={() => onNavigate('detail', { id: place.id })}
                className="aspect-square bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer flex flex-col overflow-hidden"
              >
                <PlaceImage
                  imageUrl={place.imageUrl}
                  category={place.category}
                  className="w-full flex-1 min-h-0 rounded-2xl object-cover mb-2"
                />
                <div className="px-1 shrink-0">
                  <div className="flex justify-between items-center mb-0.5">
                    <h6 className="font-bold text-gray-900 text-sm truncate flex-1 pr-1">{place.title}</h6>
                    {place.reviewCount > 0 ? (
                      <span className="flex items-center text-brand-point text-[10px] font-bold gap-0.5 flex-shrink-0">
                        <Star size={10} className="fill-brand-point" /> {place.rating.toFixed(1)}
                      </span>
                    ) : place.aiRating ? (
                      <span className="flex items-center text-gray-900 text-[10px] font-bold gap-0.5 flex-shrink-0">
                        <Star size={10} className="fill-[#008BFF] text-[#008BFF]" /> {place.aiRating.toFixed(1)}
                        <span className="text-[8px] font-normal text-gray-400">[AI]</span>
                      </span>
                    ) : (
                      <span className="text-gray-400 text-[10px] flex-shrink-0">🐾</span>
                    )}
                  </div>
                  <p className="text-[10px] text-gray-500 mb-1.5 flex items-center gap-1 truncate">
                    <MapPin size={10} className="flex-shrink-0" /> {place.address}
                  </p>
                  <span className="inline-block bg-primary/10 text-primary border border-primary/30 text-[10px] font-medium px-2 py-0.5 rounded-full">
                    {place.category.toUpperCase()}
                  </span>
                </div>
              </div>
            )
          ))
        )}
        {/* 무한 스크롤 sentinel */}
        {filteredPlaces.length > displayCount && (
          <div ref={sentinelRef} className="col-span-full h-10 flex items-center justify-center">
            <span className="text-xs text-gray-400">불러오는 중...</span>
          </div>
        )}
      </div>
    </motion.div>
  );
}


function FilterButton({ label, active, onClick }: { label: string, active: boolean, onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`whitespace-nowrap px-4 py-1.5 rounded-full text-xs font-medium transition-colors ${
        active 
          ? 'bg-primary text-white shadow-md shadow-primary/20' 
          : 'bg-gray-100 text-gray-500 border border-transparent hover:bg-gray-200'
      }`}
    >
      {label}
    </button>
  );
}