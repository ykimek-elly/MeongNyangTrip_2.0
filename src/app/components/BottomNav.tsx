import React from 'react';
import { Home, Map, Sparkles, User, MessageCircle } from 'lucide-react';

interface BottomNavProps {
  activeTab: string;
  onNavigate: (tab: string) => void;
}

/** 하단 고정 네비게이션 (GNB) */
export function BottomNav({ activeTab, onNavigate }: BottomNavProps) {
  return (
    <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px] h-[70px] bg-white/95 backdrop-blur-md border-t border-gray-100 rounded-t-3xl flex justify-around items-center z-[1000] shadow-[0_-5px_20px_rgba(0,0,0,0.03)]">
      {[
        { id: 'home', label: '홈', icon: Home },
        { 
          id: 'list', 
          label: '목록보기', 
          icon: (props: any) => (
            <svg xmlns="http://www.w3.org/2000/svg" width={props.size} height={props.size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={props.strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={props.className}>
              <line x1="8" y1="6" x2="21" y2="6"></line>
              <line x1="8" y1="12" x2="21" y2="12"></line>
              <line x1="8" y1="18" x2="21" y2="18"></line>
              <line x1="3" y1="6" x2="3.01" y2="6"></line>
              <line x1="3" y1="12" x2="3.01" y2="12"></line>
              <line x1="3" y1="18" x2="3.01" y2="18"></line>
            </svg>
          )
        },
        { id: 'ai-walk-guide', label: 'AI산책', icon: Sparkles },
        { id: 'map', label: '지도', icon: Map },
        { id: 'lounge', label: '라운지', icon: MessageCircle },
        { id: 'mypage', label: '마이', icon: User },
      ].map((item) => (
        <div
          key={item.id}
          className={`flex flex-col items-center justify-center cursor-pointer transition-colors duration-300 flex-1 ${
            activeTab === item.id ? 'text-primary font-bold' : 'text-muted-foreground'
          }`}
          onClick={() => onNavigate(item.id)}
          role="button"
          aria-label={`${item.label} 탭으로 이동`}
        >
          <item.icon size={22} className="mb-1" strokeWidth={activeTab === item.id ? 2.5 : 2} />
          <span className="text-[10px]">{item.label}</span>
        </div>
      ))}
    </nav>
  );
}