import React, { useState } from 'react';
import { Place } from '../data/places';
import { Star, MapPin } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface CategoryBestRankingProps {
  places: Place[];
  onNavigate: (page: string, params?: any) => void;
}

const CATEGORIES = [
  { id: 'place', label: '멍냥플레이스' },
  { id: 'stay', label: '멍냥스테이' },
  { id: 'dining', label: '멍냥다이닝' },
];

export function CategoryBestRanking({ places, onNavigate }: CategoryBestRankingProps) {
  const [activeCategory, setActiveCategory] = useState('place');

  const filteredPlaces = places
    .filter(p => p.cat === activeCategory)
    .sort((a, b) => b.rating - a.rating);

  return (
    <div className="mb-10 px-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-bold text-gray-800">카테고리별 베스트 👍</h3>
        <button 
          onClick={() => onNavigate('list', { category: activeCategory })}
          className="text-xs text-gray-400 flex items-center gap-1 hover:text-gray-600 transition-colors"
        >
          더보기 <span className="text-[10px]">›</span>
        </button>
      </div>
      
      {/* 카테고리 탭 */}
      <div className="flex gap-2 mb-4 overflow-x-auto scrollbar-hide">
        {CATEGORIES.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setActiveCategory(cat.id)}
            className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all duration-200 ${
              activeCategory === cat.id
                ? 'bg-primary text-white shadow-md'
                : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* 세로 리스트 */}
      <div className="flex flex-col gap-3">
        <AnimatePresence mode="popLayout">
          {filteredPlaces.length > 0 ? (
            (() => {
              // 데모: 5개 미만이면 반복 채워서 표시
              const displayList = [...filteredPlaces];
              while (displayList.length < 5) {
                const source = filteredPlaces[displayList.length % filteredPlaces.length];
                displayList.push({ ...source, id: source.id * 1000 + displayList.length });
              }
              return displayList;
            })()
            .slice(0, 5).map((place, idx) => (
              <motion.div 
                key={place.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: idx * 0.05 }}
                onClick={() => onNavigate('detail', { id: place.id })}
                className="bg-white p-3 rounded-2xl shadow-[0_2px_15px_rgba(0,0,0,0.03)] border border-gray-100 flex gap-4 cursor-pointer hover:border-primary/30 transition-colors"
              >
                {/* 이미지 & 순위 */}
                <div className="relative w-[80px] h-[80px] flex-shrink-0 rounded-xl overflow-hidden bg-gray-100">
                   <div className="absolute top-2 left-2 bg-primary text-white w-6 h-6 flex items-center justify-center rounded-md font-bold text-xs z-10 shadow-sm">
                    {idx + 1}
                  </div>
                  <img 
                    src={place.img} 
                    alt={place.title}
                    className="w-full h-full object-cover" 
                  />
                </div>

                {/* 콘텐츠 */}
                <div className="flex-1 flex flex-col justify-between py-0.5">
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded uppercase tracking-tight">
                      {place.cat}
                    </span>
                    <span className="text-xs text-gray-400">후기 {place.reviewCount}개</span>
                  </div>
                  
                  <h4 className="font-bold text-gray-800 truncate text-[15px] px-[0px] py-[3px]">{place.title}</h4>
                  
                  <div className="flex items-center justify-between mt-auto">
                    <span className="flex items-center gap-1 text-xs text-gray-500 truncate max-w-[120px]">
                      <MapPin size={12} className="text-gray-400" /> {place.loc}
                    </span>
                    <span className="flex items-center gap-1 font-bold text-sm text-gray-800">
                      <Star size={14} className="text-brand-point fill-brand-point" /> {place.rating}
                    </span>
                  </div>
                </div>
              </motion.div>
            ))
          ) : (
            <div className="w-full py-12 text-center text-gray-400 text-sm flex items-center justify-center bg-gray-50 rounded-2xl">
              해당 카테고리의 장소가 없습니다.
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}