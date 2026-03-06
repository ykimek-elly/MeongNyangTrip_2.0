import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface Friend {
  id: string;
  name: string;
  profileImg: string;
  petName: string;
  petType: '강아지' | '고양이';
  isVerified: boolean;
}

export interface ShareRecord {
  id: number;
  postId: number;
  friendId: string;
  friendName: string;
  sharedAt: string;
  message?: string;
}

// TODO: [DB 연동] supabase.from('friends').select() → 초기 목업 대체
const INITIAL_FRIENDS: Friend[] = [
  {
    id: 'f1',
    name: '구름이네',
    profileImg: 'https://images.unsplash.com/photo-1665918577658-c7cddc5fd53c?w=100&q=80',
    petName: '구름이',
    petType: '강아지',
    isVerified: true,
  },
  {
    id: 'f2',
    name: '초코맘',
    profileImg: 'https://images.unsplash.com/photo-1651212508936-dfb6f6ea3d81?w=100&q=80',
    petName: '초코',
    petType: '강아지',
    isVerified: true,
  },
  {
    id: 'f3',
    name: '두부아빠',
    profileImg: 'https://images.unsplash.com/photo-1730402739842-fbfe757d417e?w=100&q=80',
    petName: '두부',
    petType: '강아지',
    isVerified: false,
  },
  {
    id: 'f4',
    name: '뽀삐네',
    profileImg: 'https://images.unsplash.com/photo-1629098932831-6a58b4307b2a?w=100&q=80',
    petName: '뽀삐',
    petType: '강아지',
    isVerified: false,
  },
  {
    id: 'f5',
    name: '냥이맘',
    profileImg: 'https://images.unsplash.com/photo-1719305406153-b0d36aa305ac?w=100&q=80',
    petName: '나비',
    petType: '고양이',
    isVerified: true,
  },
  {
    id: 'f6',
    name: '해피해피',
    profileImg: 'https://images.unsplash.com/photo-1737699430579-3f20b8abc613?w=100&q=80',
    petName: '해피',
    petType: '강아지',
    isVerified: false,
  },
  {
    id: 'f7',
    name: '코코넨네',
    profileImg: 'https://images.unsplash.com/photo-1748992341389-fdc5c204ad13?w=100&q=80',
    petName: '코코',
    petType: '고양이',
    isVerified: true,
  },
  {
    id: 'f8',
    name: '보리보리',
    profileImg: 'https://images.unsplash.com/photo-1560968811-154ed258e610?w=100&q=80',
    petName: '보리',
    petType: '강아지',
    isVerified: false,
  },
];

// Suggested friends to add (not yet added)
const SUGGESTED_FRIENDS: Friend[] = [
  {
    id: 's1',
    name: '망고주스',
    profileImg: 'https://images.unsplash.com/photo-1517849845537-4d257902454a?w=100&q=80',
    petName: '망고',
    petType: '강아지',
    isVerified: false,
  },
  {
    id: 's2',
    name: '치즈냥',
    profileImg: 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=100&q=80',
    petName: '치즈',
    petType: '고양이',
    isVerified: false,
  },
  {
    id: 's3',
    name: '라떼네',
    profileImg: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=100&q=80',
    petName: '라떼',
    petType: '강아지',
    isVerified: true,
  },
];

interface FriendState {
  friends: Friend[];
  suggestedFriends: Friend[];
  shareRecords: ShareRecord[];
  addFriend: (friend: Friend) => void;
  removeFriend: (friendId: string) => void;
  sharePost: (postId: number, friendIds: string[], message?: string) => void;
  getShareCountForPost: (postId: number) => number;
}

export const useFriendStore = create<FriendState>()(
  persist(
    (set, get) => ({
      // TODO: [DB 연동] supabase.from('friends').select() → 친구 목록 fetch
      friends: INITIAL_FRIENDS,
      suggestedFriends: SUGGESTED_FRIENDS,
      shareRecords: [],

      // TODO: [DB 연동] supabase.from('friends').insert → 상대방에게 알림 발송 로직 추가
      addFriend: (friend) =>
        set((state) => ({
          friends: [...state.friends, friend],
          suggestedFriends: state.suggestedFriends.filter((f) => f.id !== friend.id),
        })),

      // TODO: [DB 연동] supabase.from('friends').delete
      removeFriend: (friendId) =>
        set((state) => ({
          friends: state.friends.filter((f) => f.id !== friendId),
        })),

      // TODO: [DB 연동] supabase.from('share_records').insert → DM 또는 푸시 알림 연동
      sharePost: (postId, friendIds, message) =>
        set((state) => {
          const newRecords: ShareRecord[] = friendIds.map((fId) => ({
            id: Date.now() + Math.random(),
            postId,
            friendId: fId,
            friendName: state.friends.find((f) => f.id === fId)?.name || '',
            sharedAt: new Date().toISOString(),
            message,
          }));
          return { shareRecords: [...state.shareRecords, ...newRecords] };
        }),

      getShareCountForPost: (postId) => {
        return get().shareRecords.filter((r) => r.postId === postId).length;
      },
    }),
    {
      // TODO: [DB 연동] persist 제거 → Supabase 실시간 동기화로 대체
      name: 'meongnyang-friend-storage',
    }
  )
);
