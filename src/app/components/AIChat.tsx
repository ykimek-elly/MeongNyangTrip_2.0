import React, { useState, useRef, useEffect } from 'react';
import { X, Send, Bot } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export function AIChat() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<{ id: number; text: string; isBot: boolean }[]>([
    { id: 1, text: "안녕하세요! 🐶🐱\n반려동물과의 완벽한 여행을 도와드릴게요.\n\"경기도 가평 캠핑장 추천해줘\" 처럼 물어보세요!", isBot: true }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isOpen]);

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMsg = { id: Date.now(), text: input, isBot: false };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsLoading(true);

    // AI 응답 시뮬레이션
    setTimeout(() => {
      const botMsg = { 
        id: Date.now() + 1, 
        text: getMockResponse(userMsg.text), 
        isBot: true 
      };
      setMessages(prev => [...prev, botMsg]);
      setIsLoading(false);
    }, 1500);
  };

  const getMockResponse = (query: string) => {
    if (query.includes('캠핑')) return "가평의 '멍스테이 글램핑'은 어떠신가요? ⛺\n넓은 잔디밭이 있어 아이들이 뛰어놀기 좋아요!";
    if (query.includes('카페')) return "용인의 '우드무드 카페'를 추천해요! ☕\n숲속 뷰가 정말 멋지고 반려동물 전용 메뉴도 있어요.";
    if (query.includes('서울')) return "서울숲 반려동물 구역이 산책하기 최고죠! 🌳\n주변에 펫 프렌들리 맛집도 많답니다.";
    return "좋은 질문이네요! ✨\n더 구체적인 지역이나 취향을 말씀해 주시면 딱 맞는 곳을 찾아드릴게요.";
  };

  return (
    <>
      {/* 전역 AI 챗봇 버튼 — 컨테이너 우측 외부 고정 */}
      <div className="fixed bottom-24 inset-x-0 flex justify-center pointer-events-none z-50">
        <div className="relative w-full max-w-[600px]">
          <div className="absolute bottom-0 left-full pl-3">
            <button
              onClick={() => setIsOpen(true)}
              className="w-14 h-14 bg-primary text-white rounded-full shadow-[0_4px_20px_rgba(227,99,148,0.4)] flex items-center justify-center hover:scale-105 active:scale-[0.97] transition-transform pointer-events-auto"
              title="AI 챗봇"
            >
              <Bot size={22} />
            </button>
          </div>
        </div>
      </div>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 z-[2000] flex justify-center items-end"
            onClick={(e) => { if (e.target === e.currentTarget) setIsOpen(false); }}
          >
            <motion.div
              initial={{ y: "100%" }}
              animate={{ y: 0 }}
              exit={{ y: "100%" }}
              transition={{ type: "spring", damping: 25, stiffness: 300 }}
              className="w-full max-w-[600px] h-[85vh] bg-white rounded-t-3xl flex flex-col overflow-hidden shadow-[0_-5px_30px_rgba(0,0,0,0.2)]"
            >
              {/* Header */}
              <div className="p-4 bg-white border-b border-gray-100 flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center">
                    <Bot size={18} />
                  </div>
                  <h5 className="m-0 font-bold text-lg">AI 멍냥 플래너 ✨</h5>
                </div>
                <button onClick={() => setIsOpen(false)} className="text-gray-400 hover:text-gray-600">
                  <X size={24} />
                </button>
              </div>

              {/* Messages */}
              <div className="flex-1 p-5 overflow-y-auto bg-gray-50 flex flex-col gap-3">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`max-w-[80%] p-3 px-4 rounded-2xl text-sm leading-relaxed relative ${
                      msg.isBot
                        ? "bg-white text-gray-800 self-start rounded-bl-sm border border-gray-100 shadow-sm"
                        : "bg-primary text-white self-end rounded-br-sm"
                    }`}
                  >
                    {msg.text.split('\n').map((line, i) => (
                      <React.Fragment key={i}>
                        {line}
                        <br />
                      </React.Fragment>
                    ))}
                  </div>
                ))}
                {isLoading && (
                  <div className="self-start bg-white p-3 rounded-2xl rounded-bl-sm border border-gray-100 shadow-sm">
                    <div className="flex gap-1">
                      <span className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '0s' }}></span>
                      <span className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></span>
                      <span className="w-1.5 h-1.5 bg-gray-300 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></span>
                    </div>
                  </div>
                )}
                <div ref={chatEndRef} />
              </div>

              {/* Input */}
              <div className="p-4 bg-white border-t border-gray-100">
                <div className="flex items-center gap-2 bg-gray-100 rounded-full p-1 pl-4">
                  <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                    placeholder="무엇이든 물어보세요..."
                    className="flex-1 bg-transparent border-none outline-none text-sm py-2"
                  />
                  <button
                    onClick={handleSend}
                    className="bg-primary text-white p-2.5 rounded-full hover:bg-primary/90 transition-spring"
                  >
                    <Send size={16} />
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}