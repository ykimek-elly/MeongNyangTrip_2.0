import { create } from 'zustand';
import { friendApi } from '../api/friendApi';

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

interface FriendState {
  friends: Friend[];
  suggestedFriends: Friend[];
  shareRecords: ShareRecord[];
  
  // Actions
  fetchFriends: () => Promise<void>;
  fetchSuggestedFriends: () => Promise<void>;
  addFriend: (friend: Friend) => Promise<void>;
  removeFriend: (friendId: string) => void;
  sharePost: (postId: number, friendIds: string[], message?: string) => Promise<void>;
  getShareCountForPost: (postId: number) => number;
}

export const useFriendStore = create<FriendState>()((set, get) => ({
  friends: [],
  suggestedFriends: [],
  shareRecords: [],

  fetchFriends: async () => {
    try {
      const friends = await friendApi.getFriends();
      set({ friends });
    } catch (err) {
      console.error('Failed to fetch friends:', err);
    }
  },

  fetchSuggestedFriends: async () => {
    try {
      const suggestedFriends = await friendApi.getSuggestedFriends();
      set({ suggestedFriends });
    } catch (err) {
      console.error('Failed to fetch suggested friends:', err);
    }
  },

  addFriend: async (friend) => {
    try {
      await friendApi.addFriend(friend.id);
      set((state) => ({
        friends: [...state.friends, friend],
        suggestedFriends: state.suggestedFriends.filter((f) => f.id !== friend.id),
      }));
    } catch (err) {
      console.error('Failed to add friend:', err);
    }
  },

  // 로컬 상태만 우선 삭제 - 향후 백엔드 연동 필요시 구현
  removeFriend: (friendId) =>
    set((state) => ({
      friends: state.friends.filter((f) => f.id !== friendId),
    })),

  sharePost: async (postId, friendIds, message) => {
    try {
      await friendApi.sharePost({ postId, friendIds, message });
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
      });
    } catch (err) {
      console.error('Failed to share post:', err);
    }
  },

  getShareCountForPost: (postId) => {
    return get().shareRecords.filter((r) => r.postId === postId).length;
  },
}));
