import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface DMMessage {
  id: number;
  from: string;
  content: string;
  time: string;
  createdAt: string;
  isRead: boolean;
}

export interface DMConversation {
  partnerId: string;
  partnerImg: string;
  messages: DMMessage[];
  lastActivity: string; // ISO for sorting
}

// TODO: [DB 연동] GET /api/dms → 나에게 온 DM 목록 (WebSocket STOMP 실시간)
const INITIAL_CONVERSATIONS: DMConversation[] = [
  {
    partnerId: '보리보리',
    partnerImg: 'https://images.unsplash.com/photo-1569163139394-de4e5f43e5ca?w=100&q=80',
    lastActivity: '2026-03-05T11:02:00',
    messages: [
      { id: 1, from: '보리보리', content: '혹시 다음에 같이 산책해요!', time: '1시간 전', createdAt: '2026-03-05T11:00:00', isRead: false },
      { id: 2, from: '보리보리', content: '저도 강아지 키우는데 한강 자주 가요 ☺️', time: '58분 전', createdAt: '2026-03-05T11:02:00', isRead: false },
    ],
  },
  {
    partnerId: '망고주스',
    partnerImg: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&q=80',
    lastActivity: '2026-03-05T10:00:00',
    messages: [
      { id: 3, from: '망고주스', content: '반려동물 크기 제한 있나요?', time: '4시간 전', createdAt: '2026-03-05T08:00:00', isRead: true },
      { id: 4, from: '망고주스', content: '글램핑장 사진도 찍어오셨나요? 참고하고 싶어서요!', time: '2시간 전', createdAt: '2026-03-05T10:00:00', isRead: false },
    ],
  },
  {
    partnerId: '코코넨네',
    partnerImg: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&q=80',
    lastActivity: '2026-03-05T08:00:00',
    messages: [
      { id: 5, from: '코코넨네', content: '예약 어떻게 하셨어요?', time: '어제', createdAt: '2026-03-05T08:00:00', isRead: true },
    ],
  },
];

interface DMState {
  conversations: DMConversation[];
  // TODO: [DB 연동] POST /api/dms → WebSocket STOMP publish
  sendMessage: (myUsername: string, partnerId: string, content: string) => void;
  // TODO: [DB 연동] PATCH /api/dms/read?partner={id} → is_read=true bulk UPDATE
  markAllRead: (myUsername: string, partnerId: string) => void;
  getUnreadTotal: (myUsername: string) => number;
}

export const useDMStore = create<DMState>()(
  persist(
    (set, get) => ({
      conversations: INITIAL_CONVERSATIONS,

      sendMessage: (myUsername, partnerId, content) => {
        const newMsg: DMMessage = {
          id: Date.now(),
          from: myUsername,
          content,
          time: '방금 전',
          createdAt: new Date().toISOString(),
          isRead: true,
        };
        set((state) => {
          const exists = state.conversations.find(c => c.partnerId === partnerId);
          if (exists) {
            return {
              conversations: state.conversations.map(c =>
                c.partnerId === partnerId
                  ? { ...c, messages: [...c.messages, newMsg], lastActivity: newMsg.createdAt }
                  : c
              ),
            };
          }
          return {
            conversations: [
              ...state.conversations,
              { partnerId, partnerImg: '', messages: [newMsg], lastActivity: newMsg.createdAt },
            ],
          };
        });
      },

      markAllRead: (myUsername, partnerId) => {
        set((state) => ({
          conversations: state.conversations.map(c =>
            c.partnerId === partnerId
              ? { ...c, messages: c.messages.map(m => m.from !== myUsername ? { ...m, isRead: true } : m) }
              : c
          ),
        }));
      },

      getUnreadTotal: (myUsername) => {
        const { conversations } = get();
        return conversations.reduce((total, c) =>
          total + c.messages.filter(m => m.from !== myUsername && !m.isRead).length
        , 0);
      },
    }),
    { name: 'meongnyang-dm-storage' }
  )
);
