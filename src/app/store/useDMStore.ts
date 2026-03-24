import { create } from 'zustand';
import { dmApi } from '../api/dmApi';

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
  lastMessage: string;
  lastActivity: string;
  unreadCount: number;
  messages: DMMessage[];
  messagesLoaded: boolean;
}

interface DMState {
  conversations: DMConversation[];
  isLoading: boolean;

  /** GET /dms — 대화 목록 fetch (DMList 진입 시 호출) */
  fetchConversations: () => Promise<void>;
  /** GET /dms/:partnerId — 메시지 목록 fetch (DMDetail 진입 시 호출) */
  fetchMessages: (partnerId: string) => Promise<void>;
  /** POST /dms/:partnerId — 메시지 전송 (낙관적 업데이트 + API) */
  sendMessage: (myUsername: string, partnerId: string, content: string) => Promise<void>;
  /** PATCH /dms/:partnerId/read — 읽음 처리 (로컬 즉시 반영 + API) */
  markAllRead: (myUsername: string, partnerId: string) => Promise<void>;
  /** 전체 읽지 않은 메시지 수 */
  getUnreadTotal: () => number;
}

function formatTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  return new Date(iso).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
}

export const useDMStore = create<DMState>()((set, get) => ({
  conversations: [],
  isLoading: false,

  fetchConversations: async () => {
    set({ isLoading: true });
    try {
      const list = await dmApi.getConversations();
      set((state) => ({
        isLoading: false,
        conversations: list.map((dto) => {
          const existing = state.conversations.find(c => c.partnerId === dto.partnerId);
          return {
            partnerId: dto.partnerId,
            partnerImg: dto.partnerImg,
            lastMessage: dto.lastMessage,
            lastActivity: dto.lastMessageAt,
            unreadCount: dto.unreadCount,
            messages: existing?.messages ?? [],
            messagesLoaded: existing?.messagesLoaded ?? false,
          };
        }),
      }));
    } catch {
      set({ isLoading: false });
    }
  },

  fetchMessages: async (partnerId) => {
    try {
      const msgs = await dmApi.getMessages(partnerId);
      set((state) => ({
        conversations: state.conversations.map(c =>
          c.partnerId === partnerId
            ? {
                ...c,
                messages: msgs.map(m => ({
                  id: m.id,
                  from: m.fromId,
                  content: m.content,
                  time: formatTime(m.createdAt),
                  createdAt: m.createdAt,
                  isRead: m.isRead,
                })),
                messagesLoaded: true,
              }
            : c
        ),
      }));
    } catch {
      // 메시지 로드 실패 시 빈 상태로 열림 처리
      set((state) => ({
        conversations: state.conversations.map(c =>
          c.partnerId === partnerId ? { ...c, messagesLoaded: true } : c
        ),
      }));
    }
  },

  sendMessage: async (myUsername, partnerId, content) => {
    const now = new Date().toISOString();
    const tempId = Date.now();
    const tempMsg: DMMessage = {
      id: tempId,
      from: myUsername,
      content,
      time: '방금 전',
      createdAt: now,
      isRead: true,
    };

    // 낙관적 업데이트
    set((state) => {
      const exists = state.conversations.find(c => c.partnerId === partnerId);
      if (exists) {
        return {
          conversations: state.conversations.map(c =>
            c.partnerId === partnerId
              ? { ...c, messages: [...c.messages, tempMsg], lastActivity: now, lastMessage: content }
              : c
          ),
        };
      }
      return {
        conversations: [
          ...state.conversations,
          {
            partnerId, partnerImg: '', lastMessage: content,
            lastActivity: now, unreadCount: 0,
            messages: [tempMsg], messagesLoaded: true,
          },
        ],
      };
    });

    // API 호출 — 성공 시 temp id를 실제 id로 교체
    try {
      const sent = await dmApi.sendMessage(partnerId, content);
      set((state) => ({
        conversations: state.conversations.map(c =>
          c.partnerId === partnerId
            ? {
                ...c,
                messages: c.messages.map(m =>
                  m.id === tempId
                    ? { ...m, id: sent.id, createdAt: sent.createdAt }
                    : m
                ),
              }
            : c
        ),
      }));
    } catch {
      // 실패해도 낙관적 업데이트 유지
    }
  },

  markAllRead: async (myUsername, partnerId) => {
    // 로컬 즉시 반영
    set((state) => ({
      conversations: state.conversations.map(c =>
        c.partnerId === partnerId
          ? {
              ...c,
              unreadCount: 0,
              messages: c.messages.map(m =>
                m.from !== myUsername ? { ...m, isRead: true } : m
              ),
            }
          : c
      ),
    }));
    try {
      await dmApi.markAllRead(partnerId);
    } catch {
      // 읽음 처리 실패 시 로컬 상태는 이미 반영됨
    }
  },

  getUnreadTotal: () =>
    get().conversations.reduce((sum, c) => sum + c.unreadCount, 0),
}));
