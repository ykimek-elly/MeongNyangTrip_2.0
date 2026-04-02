import { create } from 'zustand';
import api from '../api/axios';

export interface CommentItem {
  id: number;
  user: string;
  content: string;
  time: string;
  createdAt: string;
  isOwner: boolean;   // ← 추가: 댓글 소유자 여부
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
  postType: string;
  time: string;
  createdAt: string;
  isLiked: boolean;
  isOwner: boolean;
  likedBy: string[];
  commentList: CommentItem[];
  dmList: any[];
  isReported: boolean;
  isHidden: boolean;
}

interface FeedState {
  posts: FeedPost[];
  talks: FeedPost[];
  loading: boolean;
  fetchPosts: () => Promise<void>;
  fetchTalks: () => Promise<void>;
  addPost: (post: { content: string; imageUrl: string; placeId?: number }) => Promise<void>;
  addTalk: (content: string) => Promise<void>;
  toggleLike: (postId: number) => Promise<void>;
  addComment: (postId: number, user: string, content: string) => Promise<void>;
  addTalkComment: (talkId: number, content: string) => Promise<void>;
  editComment: (postId: number, commentId: number, content: string) => Promise<void>;  // ← 추가
  deleteComment: (postId: number, commentId: number) => Promise<void>;
  editPost: (postId: number, content: any) => Promise<void>;
  deletePost: (postId: number) => Promise<void>;
  toggleHidePost: (postId: number) => void;
  reportPost: (postId: number) => void;
  addDM: (postId: number, from: string, to: string, content: string) => void;
  markDMRead: (postId: number, dmId: number) => void;
}

const API = '/lounge';

const mapPost = (p: any): FeedPost => ({
  id: p.id,
  user: p.user,
  userImg: p.userImg ?? 'https://images.unsplash.com/photo-1517849845537-4d257902454a?w=100&q=80',
  img: p.img ?? '',
  content: p.content,
  likes: p.likes ?? 0,
  comments: p.comments ?? 0,
  dms: 0,
  placeId: p.placeId,
  postType: p.postType ?? 'FEED',
  time: p.time ?? '',
  createdAt: p.createdAt ?? '',
  isLiked: p.isLiked ?? false,
  isOwner: p.isOwner ?? false,
  likedBy: [],
  commentList: (p.commentList ?? []).map((c: any) => ({
    id: c.id,
    user: c.user,
    content: c.content,
    time: '',
    createdAt: c.createdAt ?? '',
    isOwner: c.isOwner ?? false,   // ← 추가
  })),
  dmList: [],
  isReported: false,
  isHidden: false,
});

export const useFeedStore = create<FeedState>((set, get) => ({
  posts: [],
  talks: [],
  loading: false,

  // 일반 피드 조회
  fetchPosts: async () => {
    set({ loading: true });
    try {
      const res = await api.get(`${API}/posts?type=FEED`);
      const data = res.data?.data ?? [];
      set({ posts: data.map(mapPost) });
    } catch (e) {
      console.error('피드 조회 실패', e);
    } finally {
      set({ loading: false });
    }
  },

  // 산책 톡 조회 (24시간 이내)
  fetchTalks: async () => {
    try {
      const res = await api.get(`${API}/posts?type=TALK`);
      const data = res.data?.data ?? [];
      set({ talks: data.map(mapPost) });
    } catch (e) {
      console.error('산책 톡 조회 실패', e);
    }
  },

  // 일반 게시글 작성
  addPost: async ({ content, imageUrl, placeId }) => {
    try {
      await api.post(`${API}/posts`, { content, imageUrl, placeId, postType: 'FEED' });
      await get().fetchPosts();
    } catch (e) {
      console.error('게시글 작성 실패', e);
    }
  },

  // 산책 톡 작성
  addTalk: async (content) => {
    try {
      const res = await api.post(`${API}/posts`, { content, postType: 'TALK' });
      const newTalk = mapPost(res.data?.data);
      set((state) => ({ talks: [newTalk, ...state.talks] }));
    } catch (e) {
      console.error('산책 톡 작성 실패', e);
      throw e;
    }
  },

  // 좋아요 토글
  toggleLike: async (postId) => {
    try {
      const res = await api.post(`${API}/posts/${postId}/likes`);
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

  // 일반 피드 댓글 작성
  addComment: async (postId, _user, content) => {
    try {
      const res = await api.post(`${API}/posts/${postId}/comments`, { content });
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
                    isOwner: true,   // 본인이 작성한 댓글이므로 항상 true
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

  // 산책 톡 댓글 작성
  addTalkComment: async (talkId, content) => {
    try {
      const res = await api.post(`${API}/posts/${talkId}/comments`, { content });
      const newComment = res.data?.data;
      set((state) => ({
        talks: state.talks.map((t) =>
          t.id === talkId
            ? {
                ...t,
                comments: t.comments + 1,
                commentList: [
                  ...t.commentList,
                  {
                    id: newComment?.id ?? Date.now(),
                    user: newComment?.user ?? '나',
                    content,
                    time: '방금 전',
                    createdAt: newComment?.createdAt ?? new Date().toISOString(),
                    isOwner: true,
                  },
                ],
              }
            : t
        ),
      }));
    } catch (e) {
      console.error('산책 톡 댓글 작성 실패', e);
      throw e;
    }
  },

  // 댓글 수정 (API 연동)
  editComment: async (postId, commentId, content) => {
    try {
      await api.patch(`${API}/posts/${postId}/comments/${commentId}`, { content });
      const updateCommentList = (list: any[]) =>
        list.map((c) => c.id === commentId ? { ...c, content } : c);
      set((state) => ({
        posts: state.posts.map((p) =>
          p.id === postId ? { ...p, commentList: updateCommentList(p.commentList) } : p
        ),
        talks: state.talks.map((t) =>
          t.id === postId ? { ...t, commentList: updateCommentList(t.commentList) } : t
        ),
      }));
    } catch (e) {
      console.error('댓글 수정 실패', e);
      throw e;
    }
  },

  // 댓글 삭제 (API 연동)
  deleteComment: async (postId, commentId) => {
    try {
      await api.delete(`${API}/posts/${postId}/comments/${commentId}`);
      const removeComment = (p: any) => ({
        ...p,
        comments: Math.max(0, p.comments - 1),
        commentList: p.commentList.filter((c: any) => c.id !== commentId),
      });
      set((state) => ({
        posts: state.posts.map((p) => p.id === postId ? removeComment(p) : p),
        talks: state.talks.map((t) => t.id === postId ? removeComment(t) : t),
      }));
    } catch (e) {
      console.error('댓글 삭제 실패', e);
    }
  },

  // 게시글 수정
  editPost: async (postId, content) => {
    if (typeof content === 'object' && content !== null) {
      set((state) => ({
        posts: state.posts.map((p) => p.id === postId ? { ...p, ...content } : p),
        talks: state.talks.map((t) => t.id === postId ? { ...t, ...content } : t),
      }));
      return;
    }
    try {
      await api.patch(`${API}/posts/${postId}`, { content });
      set((state) => ({
        posts: state.posts.map((p) => p.id === postId ? { ...p, content } : p),
        talks: state.talks.map((t) => t.id === postId ? { ...t, content } : t),
      }));
    } catch (e) {
      console.error('게시글 수정 실패', e);
    }
  },

  // 게시글 삭제
  deletePost: async (postId) => {
    try {
      await api.delete(`${API}/posts/${postId}`);
      set((state) => ({
        posts: state.posts.filter((p) => p.id !== postId),
        talks: state.talks.filter((t) => t.id !== postId),
      }));
    } catch (e) {
      console.error('게시글 삭제 실패', e);
    }
  },

  // 로컬 전용
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