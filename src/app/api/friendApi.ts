import api from './axios';
import { Friend } from '../store/useFriendStore';

export interface ShareRequestDto {
  postId: number;
  friendIds: string[];
  message?: string;
}

export const friendApi = {
  getFriends: async (): Promise<Friend[]> => {
    const { data } = await api.get<Friend[]>('/friends');
    return data;
  },

  getSuggestedFriends: async (): Promise<Friend[]> => {
    const { data } = await api.get<Friend[]>('/friends/suggested');
    return data;
  },

  addFriend: async (friendUserId: string): Promise<void> => {
    await api.post(`/friends/${friendUserId}`);
  },

  sharePost: async (request: ShareRequestDto): Promise<void> => {
    await api.post('/friends/share', request);
  }
};
