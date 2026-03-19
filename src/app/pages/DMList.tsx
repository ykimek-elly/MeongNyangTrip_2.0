import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, Search, Send, X } from 'lucide-react';
import { useDMStore } from '../store/useDMStore';
import { useAppStore } from '../store/useAppStore';

interface DMListProps {
  onNavigate: (page: string, params?: any) => void;
}

export function DMList({ onNavigate }: DMListProps) {
  const { username } = useAppStore();
  const { conversations, getUnreadTotal } = useDMStore();
  const [searchQuery, setSearchQuery] = useState('');

  const sorted = [...conversations].sort(
    (a, b) => new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime()
  );

  const filtered = searchQuery.trim()
    ? sorted.filter(c => c.partnerId.includes(searchQuery.trim()))
    : sorted;

  const totalUnread = getUnreadTotal(username);

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* 헤더 */}
      <div className="sticky top-0 z-50 bg-white border-b border-gray-100 px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => onNavigate('mypage')}
          className="p-2 -ml-1 rounded-full hover:bg-gray-100 active:bg-gray-200 transition-colors"
        >
          <ArrowLeft size={22} className="text-gray-700" />
        </button>
        <div className="flex-1">
          <h1 className="font-bold text-gray-900 text-lg leading-tight">메시지</h1>
          {totalUnread > 0 && (
            <p className="text-xs text-primary font-semibold mt-0.5">
              읽지 않은 메시지 {totalUnread}개
            </p>
          )}
        </div>
        <div className="w-9 h-9 bg-primary/10 rounded-full flex items-center justify-center">
          <Send size={16} className="text-primary" />
        </div>
      </div>

      {/* 검색창 */}
      <div className="px-4 py-3 border-b border-gray-50">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="이름 검색..."
            className="w-full pl-9 pr-8 py-2.5 bg-gray-50 rounded-xl text-sm text-gray-800 placeholder-gray-400 outline-none focus:bg-gray-100 transition-colors"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2"
            >
              <X size={14} className="text-gray-400" />
            </button>
          )}
        </div>
      </div>

      {/* 대화 목록 */}
      <div className="flex-1 divide-y divide-gray-50 pb-4">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 text-gray-400">
            <Send size={40} className="mb-4 opacity-25" />
            <p className="text-sm font-medium">메시지가 없습니다</p>
            <p className="text-xs mt-1.5 text-gray-300">
              라운지에서 다른 반려인과 대화해보세요!
            </p>
          </div>
        ) : (
          filtered.map((conv, idx) => {
            const unreadCount = conv.messages.filter(
              m => m.from !== username && !m.isRead
            ).length;
            const lastMsg = conv.messages[conv.messages.length - 1];

            return (
              <motion.button
                key={conv.partnerId}
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                onClick={() => onNavigate('dm-detail', { partner: conv.partnerId })}
                className="w-full flex items-center gap-3.5 px-4 py-4 hover:bg-gray-50 active:bg-gray-100 transition-colors text-left"
              >
                {/* 아바타 */}
                <div className="relative shrink-0">
                  <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center overflow-hidden">
                    {conv.partnerImg ? (
                      <img
                        src={conv.partnerImg}
                        alt={conv.partnerId}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <span className="text-primary font-bold text-base">
                        {conv.partnerId[0]}
                      </span>
                    )}
                  </div>
                  {unreadCount > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-primary text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1">
                      {unreadCount}
                    </span>
                  )}
                </div>

                {/* 내용 */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-0.5">
                    <span
                      className={`font-bold text-sm ${unreadCount > 0 ? 'text-gray-900' : 'text-gray-600'}`}
                    >
                      {conv.partnerId}
                    </span>
                    <span className="text-[11px] text-gray-400 shrink-0 ml-2">
                      {lastMsg?.time}
                    </span>
                  </div>
                  <p
                    className={`text-xs truncate ${
                      unreadCount > 0 ? 'text-gray-700 font-medium' : 'text-gray-400'
                    }`}
                  >
                    {lastMsg?.from === username
                      ? `나: ${lastMsg.content}`
                      : lastMsg?.content}
                  </p>
                </div>
              </motion.button>
            );
          })
        )}
      </div>
    </div>
  );
}
