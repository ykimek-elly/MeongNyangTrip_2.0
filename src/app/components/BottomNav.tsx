import React from 'react';
import { Home, BotMessageSquare, User } from 'lucide-react';
import {
  MapNavOutlineIcon, MapNavFilledIcon,
  ChatBubbleOutlineIcon, ChatBubbleFilledIcon,
  UserCircleOutlineIcon, UserCircleFilledIcon,
} from './CustomIcon';

interface BottomNavProps {
  activeTab: string;
  onNavigate: (tab: string) => void;
}

interface NavItem {
  id: string;
  label: string;
  icon: React.ComponentType<any>;
  iconActive?: React.ComponentType<any>;
}

const ListIcon = (props: any) => (
  <svg xmlns="http://www.w3.org/2000/svg" width={props.size} height={props.size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={props.strokeWidth ?? 2} strokeLinecap="round" strokeLinejoin="round" className={props.className}>
    <line x1="8" y1="6" x2="21" y2="6" />
    <line x1="8" y1="12" x2="21" y2="12" />
    <line x1="8" y1="18" x2="21" y2="18" />
    <line x1="3" y1="6" x2="3.01" y2="6" />
    <line x1="3" y1="12" x2="3.01" y2="12" />
    <line x1="3" y1="18" x2="3.01" y2="18" />
  </svg>
);

const TABS: NavItem[] = [
  { id: 'home',          label: '홈',     icon: Home },
  { id: 'list',          label: '목록',   icon: ListIcon },
  { id: 'ai-walk-guide', label: 'AI산책', icon: BotMessageSquare },
  { id: 'map',           label: '지도',   icon: MapNavOutlineIcon },
  { id: 'lounge',        label: '라운지', icon: ChatBubbleOutlineIcon },
  { id: 'mypage',        label: '마이',   icon: UserCircleOutlineIcon },
];

/** 하단 고정 네비게이션 (GNB) */
export function BottomNav({ activeTab, onNavigate }: BottomNavProps) {
  return (
    <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px] h-[70px] bg-white/95 backdrop-blur-md border-t border-gray-100 rounded-t-3xl flex justify-around items-center z-[1000] shadow-[0_-5px_20px_rgba(0,0,0,0.03)]">
      {TABS.map((item) => {
        const isActive = activeTab === item.id;
        const IconComponent = isActive && item.iconActive ? item.iconActive : item.icon;
        return (
          <div
            key={item.id}
            className={`flex flex-col items-center justify-center cursor-pointer transition-spring hover:scale-[1.12] active:scale-[0.92] flex-1 ${
              isActive ? 'text-primary font-bold' : 'text-muted-foreground'
            }`}
            onClick={() => onNavigate(item.id)}
            role="button"
            aria-label={`${item.label} 탭으로 이동`}
          >
            <IconComponent size={22} className="mb-1" strokeWidth={isActive ? 2.5 : 2} />
            <span className="text-[10px]">{item.label}</span>
          </div>
        );
      })}
    </nav>
  );
}
