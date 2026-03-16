import React, { useState } from 'react';
import { PlaceDto } from '../api/types';
import { Star, MapPin } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { PlaceImage } from './PlaceImage';

const GoogleG = ({ size = 12 }: { size?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
  </svg>
);

interface CategoryBestRankingProps {
  places: PlaceDto[];
  onNavigate: (page: string, params?: any) => void;
}

const CATEGORIES = [
  { id: 'PLACE', label: '멍냥플레이스' },
  { id: 'STAY', label: '멍냥스테이' },
  { id: 'DINING', label: '멍냥다이닝' },
];

export function CategoryBestRanking({ places, onNavigate }: CategoryBestRankingProps) {
  const [activeCategory, setActiveCategory] = useState('PLACE');

  const filteredPlaces = places
    .filter(p => p.category === activeCategory)
    .sort((a, b) => b.rating - a.rating)
    .slice(0, 4);

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
            filteredPlaces.map((place, idx) => (
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
                  <PlaceImage
                    imageUrl={place.imageUrl}
                    category={place.category}
                    className="w-full h-full object-cover"
                    iconSize={24}
                    alt={place.title}
                  />
                </div>

                {/* 콘텐츠 */}
                <div className="flex-1 flex flex-col justify-between py-0.5">
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded uppercase tracking-tight">
                      {place.category}
                    </span>
                    <span className="text-xs text-gray-400">후기 {place.reviewCount}개</span>
                  </div>

                  <h4 className="font-bold text-gray-800 truncate text-[15px] px-[0px] py-[3px]">{place.title}</h4>

                  <div className="flex items-center justify-between mt-auto">
                    <span className="flex items-center gap-1 text-xs text-gray-500 truncate max-w-[120px]">
                      <MapPin size={12} className="text-gray-400" /> {place.address}
                    </span>
                    {place.reviewCount > 0 ? (
                      <span className="flex items-center gap-0.5 font-bold text-sm text-gray-800">
                        <Star size={14} className="fill-brand-point text-brand-point" /> {place.rating.toFixed(1)}
                      </span>
                    ) : (place as any).googleRating ? (
                      <span className="flex items-center gap-0.5 font-bold text-sm text-gray-800">
                        <Star size={14} className="fill-brand-point text-brand-point" />
                        {((place as any).googleRating as number).toFixed(1)}
                        <GoogleG size={13} />
                      </span>
                    ) : (
                      <span className="flex items-center gap-0.5 font-bold text-sm text-gray-400">
                        <Star size={14} className="fill-gray-300 text-gray-300" /> 0
                      </span>
                    )}
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
