import { create } from 'zustand';
import axios from 'axios';

export interface CommentItem {
  id: number;
  user: string;
  content: string;
  time: string;
  createdAt: string;
}

export interface FeedPost {
  id: number;
  user: string;
  userImg: string;
  img: string;
  content: string;
  likes: number;
  comments: number;
  dms: number;
  placeId?: number;
  time: string;
  createdAt: string;
  isLiked: boolean;
  likedBy: string[];
  commentList: CommentItem[];
  dmList: any[];
  isReported: boolean;
  isHidden: boolean;
}

interface FeedState {
  posts: FeedPost[];
  loading: boolean;
  fetchPosts: () => Promise<void>;
  addPost: (post: { content: string; imageUrl: string; placeId?: number }) => Promise<void>;
  toggleLike: (postId: number) => Promise<void>;
  addComment: (postId: number, user: string, content: string) => Promise<void>;
  deleteComment: (postId: number, commentId: number) => Promise<void>;
  editPost: (postId: number, content: any) => Promise<void>;
  deletePost: (postId: number) => Promise<void>;
  toggleHidePost: (postId: number) => void;
  reportPost: (postId: number) => void;
  addDM: (postId: number, from: string, to: string, content: string) => void;
  markDMRead: (postId: number, dmId: number) => void;
}

const API = '/api/v1/lounge';

export const useFeedStore = create<FeedState>((set, get) => ({
  posts: [],
  loading: false,

  // 피드 전체 조회
  fetchPosts: async () => {
    set({ loading: true });
    try {
      const res = await axios.get(`${API}/posts`);
      const data = res.data?.data ?? [];
      set({
        posts: data.map((p: any) => ({
          id: p.id,
          user: p.user,
          userImg: p.userImg ?? 'https://images.unsplash.com/photo-1517849845537-4d257902454a?w=100&q=80',
          img: p.img ?? '',
          content: p.content,
          likes: p.likes ?? 0,
          comments: p.comments ?? 0,
          dms: 0,
          placeId: p.placeId,
          time: p.time ?? '',
          createdAt: p.createdAt ?? '',
          isLiked: p.isLiked ?? false,
          likedBy: [],
          commentList: (p.commentList ?? []).map((c: any) => ({
            id: c.id,
            user: c.user,
            content: c.content,
            time: '',
            createdAt: c.createdAt ?? '',
          })),
          dmList: [],
          isReported: false,
          isHidden: false,
        })),
      });
    } catch (e) {
      console.error('피드 조회 실패', e);
    } finally {
      set({ loading: false });
    }
  },

  // 게시글 작성
  addPost: async ({ content, imageUrl, placeId }) => {
    try {
      await axios.post(`${API}/posts`, { content, imageUrl, placeId });
      await get().fetchPosts();
    } catch (e) {
      console.error('게시글 작성 실패', e);
    }
  },

  // 좋아요 토글
  toggleLike: async (postId) => {
    try {
      const res = await axios.post(`${API}/posts/${postId}/likes`);
      const updated = res.data?.data;
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId
            ? { ...p, likes: updated?.likes ?? p.likes, isLiked: updated?.isLiked ?? !p.isLiked }
            : p
        ),
      }));
    } catch (e) {
      console.error('좋아요 실패', e);
    }
  },

  // 댓글 작성
  addComment: async (postId, _user, content) => {
    try {
      const res = await axios.post(`${API}/posts/${postId}/comments`, { content });
      const newComment = res.data?.data;
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId
            ? {
                ...p,
                comments: p.comments + 1,
                commentList: [
                  ...p.commentList,
                  {
                    id: newComment?.id ?? Date.now(),
                    user: newComment?.user ?? _user,
                    content,
                    time: '방금 전',
                    createdAt: newComment?.createdAt ?? new Date().toISOString(),
                  },
                ],
              }
            : p
        ),
      }));
    } catch (e) {
      console.error('댓글 작성 실패', e);
    }
  },

  // 댓글 삭제
  deleteComment: async (postId, commentId) => {
    try {
      await axios.delete(`${API}/posts/${postId}/comments/${commentId}`);
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId
            ? {
                ...p,
                comments: Math.max(0, p.comments - 1),
                commentList: p.commentList.filter((c) => c.id !== commentId),
              }
            : p
        ),
      }));
    } catch (e) {
      console.error('댓글 삭제 실패', e);
    }
  },

  // 게시글 수정
  editPost: async (postId, content) => {
    // commentList 수정 (로컬)인 경우 바로 반영
    if (typeof content === 'object' && content !== null) {
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId ? { ...p, ...content } : p
        ),
      }));
      return;
    }
    try {
      await axios.patch(`${API}/posts/${postId}`, { content });
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId ? { ...p, content } : p
        ),
      }));
    } catch (e) {
      console.error('게시글 수정 실패', e);
    }
  },

  // 게시글 삭제
  deletePost: async (postId) => {
    try {
      await axios.delete(`${API}/posts/${postId}`);
      set((state) => ({
        posts: state.posts.filter((p) => p.id !== postId),
      }));
    } catch (e) {
      console.error('게시글 삭제 실패', e);
    }
  },

  // 아래는 로컬 전용 (어드민/DM 기능)
  toggleHidePost: (postId) =>
    set((state) => ({
      posts: state.posts.map((p) =>
        p.id === postId ? { ...p, isHidden: !p.isHidden } : p
      ),
    })),

  reportPost: (postId) =>
    set((state) => ({
      posts: state.posts.map((p) =>
        p.id === postId ? { ...p, isReported: true } : p
      ),
    })),

  addDM: (postId, from, to, content) =>
    set((state) => ({
      posts: state.posts.map((p) =>
        p.id === postId
          ? {
              ...p,
              dms: p.dms + 1,
              dmList: [
                ...p.dmList,
                { id: Date.now(), from, to, content, time: '방금 전', createdAt: new Date().toISOString(), isRead: false },
              ],
            }
          : p
      ),
    })),

  markDMRead: (postId, dmId) =>
    set((state) => ({
      posts: state.posts.map((p) =>
        p.id === postId
          ? { ...p, dmList: p.dmList.map((dm) => (dm.id === dmId ? { ...dm, isRead: true } : dm)) }
          : p
      ),
    })),
}));