import React, { useState, useEffect } from 'react';
import { Star, MapPin, Search, LayoutGrid, List as ListIcon } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { motion } from 'motion/react';

interface ListProps {
  onNavigate: (page: string, params?: any) => void;
  initialParams?: { region?: string; date?: string; category?: string };
}

export function List({ onNavigate, initialParams }: ListProps) {
  const places = useAppStore((s) => s.places);
  const [activeFilter, setActiveFilter] = useState('all');
  const [filteredPlaces, setFilteredPlaces] = useState(places);
  const [searchMsg, setSearchMsg] = useState('');
  const [viewMode, setViewMode] = useState<'list' | 'grid'>('list');

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

  const handleFilterClick = (cat: string) => {
    setActiveFilter(cat);
    setSearchMsg(''); // Clear advanced search msg when manual filter is used
    if (cat === 'all') {
      setFilteredPlaces(places);
    } else {
      setFilteredPlaces(places.filter(p => p.category === cat));
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
      {/* 필터 탭 & 뷰 전환 */}
      <div className="sticky top-0 z-10 bg-white/95 backdrop-blur-sm px-4 py-3 border-b border-gray-50">
        <div className="flex items-center justify-between gap-3">
          <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide flex-1">
            <FilterButton label="전체" active={activeFilter === 'all'} onClick={() => handleFilterClick('all')} />
            <FilterButton label="🏞️ 멍냥플레이스" active={activeFilter === 'place'} onClick={() => handleFilterClick('place')} />
            <FilterButton label="🏡 멍냥스테이" active={activeFilter === 'stay'} onClick={() => handleFilterClick('stay')} />
            <FilterButton label="🍽️ 멍냥다이닝" active={activeFilter === 'dining'} onClick={() => handleFilterClick('dining')} />
          </div>
          <button 
            onClick={toggleView}
            className="p-2 text-gray-500 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors flex-shrink-0"
            aria-label={viewMode === 'list' ? "그리드 뷰로 전환" : "리스트 뷰로 전환"}
          >
            {viewMode === 'list' ? <LayoutGrid size={20} /> : <ListIcon size={20} />}
          </button>
        </div>
        
        {searchMsg && (
          <div className="mt-3 px-1 text-xs font-medium text-gray-500 bg-gray-50 p-2 rounded-lg inline-block">
            {searchMsg}
          </div>
        )}
      </div>

      {/* 목록 컨테이너 */}
      <div className={viewMode === 'list' ? "px-4 py-4 space-y-3" : "px-4 py-4 grid grid-cols-2 gap-3"}>
        {filteredPlaces.length === 0 ? (
          <div className="col-span-full flex flex-col items-center justify-center py-20 text-gray-400 opacity-75">
            <Search size={48} className="mb-4 text-gray-300" />
            <p>조건에 맞는 장소가 없어요.</p>
          </div>
        ) : (
          filteredPlaces.map(place => (
            viewMode === 'list' ? (
              // 리스트 뷰 아이템
              <div 
                key={place.id}
                className="flex items-center bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer"
                onClick={() => onNavigate('detail', { id: place.id })}
              >
                <img 
                  src={place.imageUrl || ""} 
                  className="w-[90px] h-[90px] rounded-2xl object-cover bg-gray-100" 
                  alt={place.title} 
                />
                <div className="ml-4 flex-1 min-w-0">
                  <div className="flex justify-between items-start">
                    <h6 className="font-bold text-gray-900 mb-1 truncate">{place.title}</h6>
                    <span className="flex items-center text-brand-point text-xs font-bold gap-0.5">
                      <Star size={12} className="fill-brand-point" /> {place.rating}
                    </span>
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
                className="bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm active:scale-[0.98] transition-transform cursor-pointer flex flex-col"
              >
                <img 
                  src={place.imageUrl || ""} 
                  className="w-full aspect-square rounded-2xl object-cover bg-gray-100 mb-2.5" 
                  alt={place.title} 
                />
                <div className="px-1 flex-1 flex flex-col">
                  <div className="flex justify-between items-start mb-1">
                    <h6 className="font-bold text-gray-900 text-sm truncate flex-1 pr-1">{place.title}</h6>
                    <span className="flex items-center text-brand-point text-[10px] font-bold gap-0.5 flex-shrink-0 pt-0.5">
                      <Star size={10} className="fill-brand-point" /> {place.rating}
                    </span>
                  </div>
                  <p className="text-[10px] text-gray-500 mb-2 flex items-center gap-1 truncate">
                    <MapPin size={10} className="flex-shrink-0" /> {place.address}
                  </p>
                  <div className="mt-auto">
                    <span className="inline-block bg-primary/10 text-primary border border-primary/30 text-[10px] font-medium px-2 py-0.5 rounded-full">
                      {place.category.toUpperCase()}
                    </span>
                  </div>
                </div>
              </div>
            )
          ))
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