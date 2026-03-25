import React from 'react';
import { motion } from 'motion/react';
import { Home, AlertCircle } from 'lucide-react';

interface NotFoundProps {
  onNavigate: (page: string) => void;
}

/** 404 페이지 — 잘못된 URL 접근 시 표시 */
export function NotFound({ onNavigate }: NotFoundProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-screen flex flex-col items-center justify-center px-6 text-center pb-24"
    >
      <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mb-6">
        <AlertCircle size={40} className="text-primary" />
      </div>

      <h2 className="text-xl font-bold text-gray-800 mb-2">페이지를 찾을 수 없어요</h2>
      <p className="text-sm text-gray-500 mb-8 leading-relaxed">
        요청하신 페이지가 존재하지 않거나<br />
        주소가 변경되었을 수 있습니다.
      </p>

      <button
        onClick={() => onNavigate('home')}
        className="flex items-center gap-2 bg-primary text-white px-6 py-3 rounded-2xl font-bold text-sm shadow-md hover:bg-primary/90 active:scale-[0.97] transition-spring"
      >
        <Home size={18} />
        홈으로 돌아가기
      </button>
    </motion.div>
  );
}
