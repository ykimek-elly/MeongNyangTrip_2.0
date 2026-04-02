import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, Send } from 'lucide-react';
import { useDMStore } from '../store/useDMStore';
import { useAppStore } from '../store/useAppStore';

interface DMDetailProps {
  partner: string;
  onNavigate: (page: string, params?: any) => void;
}

export function DMDetail({ partner, onNavigate }: DMDetailProps) {
  const { username } = useAppStore();
  const { conversations, fetchMessages, sendMessage, markAllRead } = useDMStore();
  const [input, setInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const conv = conversations.find(c => c.partnerId === partner);

  // 진입 시: 메시지 미로드 상태면 fetch
  useEffect(() => {
    if (partner && !conv?.messagesLoaded) {
      fetchMessages(partner);
    }
  }, [partner, conv?.messagesLoaded, fetchMessages]);

  // 진입 시 읽음 처리
  useEffect(() => {
    if (partner && username) markAllRead(username, partner);
  }, [partner, username, markAllRead]);

  // 새 메시지 도착 시 스크롤 하단 이동
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [conv?.messages.length]);

  const handleSend = () => {
    const text = input.trim();
    if (!text) return;
    sendMessage(username, partner, text);
    setInput('');
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // textarea 높이 자동 조절
  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 112)}px`;
  };

  return (
    <div className="h-screen bg-gray-50 flex flex-col overflow-hidden">
      {/* 헤더 */}
      <div className="bg-white border-b border-gray-100 px-4 py-3 flex items-center gap-3 shrink-0">
        <button
          onClick={() => onNavigate('dm')}
          className="p-2 -ml-1 rounded-full hover:bg-gray-100 active:bg-gray-200 transition-spring"
        >
          <ArrowLeft size={22} className="text-gray-700" />
        </button>

        {/* 상대방 아바타 */}
        <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center overflow-hidden shrink-0">
          {conv?.partnerImg ? (
            <img src={conv.partnerImg} alt={partner} className="w-full h-full object-cover" />
          ) : (
            <span className="text-primary font-bold text-sm">{partner[0]}</span>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <h1 className="font-bold text-gray-900 text-base leading-tight truncate">{partner}</h1>
          <p className="text-[11px] text-gray-400">반려인</p>
        </div>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {!conv?.messagesLoaded ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin mb-4" />
            <p className="text-sm">메시지 불러오는 중...</p>
          </div>
        ) : !conv || conv.messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <Send size={36} className="mb-3 opacity-25" />
            <p className="text-sm font-medium">아직 메시지가 없어요</p>
            <p className="text-xs mt-1 text-gray-300">먼저 인사를 건네보세요!</p>
          </div>
        ) : (
          <>
            {/* 날짜 구분선 */}
            <div className="flex items-center gap-3 py-1">
              <div className="flex-1 h-px bg-gray-200" />
              <span className="text-[10px] text-gray-400 font-medium shrink-0">
                {new Date(conv.messages[0].createdAt).toLocaleDateString('ko-KR', {
                  month: 'long',
                  day: 'numeric',
                })}
              </span>
              <div className="flex-1 h-px bg-gray-200" />
            </div>

            {conv.messages.map((msg, idx) => {
              const isMine = msg.from === username;
              return (
                <motion.div
                  key={msg.id}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: Math.min(idx * 0.03, 0.3) }}
                  className={`flex items-end gap-2 ${isMine ? 'flex-row-reverse' : 'flex-row'}`}
                >
                  {/* 상대방 아바타 (왼쪽에만) */}
                  {!isMine && (
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mb-1 overflow-hidden">
                      {conv.partnerImg ? (
                        <img
                          src={conv.partnerImg}
                          alt={partner}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <span className="text-primary font-bold text-xs">{partner[0]}</span>
                      )}
                    </div>
                  )}

                  <div
                    className={`max-w-[72%] flex flex-col gap-1 ${
                      isMine ? 'items-end' : 'items-start'
                    }`}
                  >
                    {/* 말풍선 */}
                    <div
                      className={`px-3.5 py-2.5 text-sm leading-relaxed break-words ${
                        isMine
                          ? 'bg-primary text-white rounded-2xl rounded-br-sm shadow-sm'
                          : 'bg-white text-gray-800 rounded-2xl rounded-bl-sm shadow-sm border border-gray-100'
                      }`}
                    >
                      {msg.content}
                    </div>
                    <span className="text-[10px] text-gray-400 px-1">{msg.time}</span>
                  </div>
                </motion.div>
              );
            })}
          </>
        )}
        {/* 스크롤 앵커 */}
        <div ref={bottomRef} className="h-1" />
      </div>

      {/* 입력창 */}
      <div className="bg-white border-t border-gray-100 px-4 py-3 flex items-end gap-2 shrink-0">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder="메시지를 입력하세요..."
          rows={1}
          className="flex-1 resize-none bg-gray-50 rounded-2xl px-4 py-3 text-sm text-gray-800 placeholder-gray-400 outline-none focus:bg-gray-100 transition-spring overflow-y-auto"
          style={{ lineHeight: '1.5', maxHeight: '112px' }}
        />
        <button
          onClick={handleSend}
          disabled={!input.trim()}
          className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-spring active:scale-90 ${
            input.trim()
              ? 'bg-primary text-white shadow-md hover:bg-primary/90'
              : 'bg-gray-100 text-gray-300 cursor-not-allowed'
          }`}
        >
          <Send size={16} />
        </button>
      </div>
    </div>
  );
}
