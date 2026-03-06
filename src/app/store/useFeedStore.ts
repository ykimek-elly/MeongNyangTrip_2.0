import { create } from 'zustand';
import { persist } from 'zustand/middleware';

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
  dmList: DMItem[];
  isReported: boolean;
  isHidden: boolean;
}

export interface CommentItem {
  id: number;
  user: string;
  content: string;
  time: string;
  createdAt: string;
}

export interface DMItem {
  id: number;
  from: string;
  to: string;
  content: string;
  time: string;
  createdAt: string;
  isRead: boolean;
}

const INITIAL_POSTS: FeedPost[] = [
  {
    id: 1,
    user: '구름이네',
    userImg: 'https://images.unsplash.com/photo-1517849845537-4d257902454a?w=100&q=80',
    img: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800&q=80',
    content: '주말엔 역시 한강공원! 날씨가 너무 좋아서 구름이도 신났어요 🐶☀️',
    likes: 124,
    comments: 18,
    dms: 3,
    placeId: 2,
    time: '2시간 전',
    createdAt: '2026-03-05T10:00:00',
    isLiked: true,
    likedBy: ['초코맘', '두부아빠', '보리보리'],
    commentList: [
      { id: 1, user: '초코맘', content: '너무 귀여워요! 어디서 찍었어요?', time: '1시간 전', createdAt: '2026-03-05T11:00:00' },
      { id: 2, user: '두부아빠', content: '구름이 완전 행복해 보여요 ㅎㅎ', time: '30분 전', createdAt: '2026-03-05T11:30:00' },
    ],
    dmList: [
      { id: 1, from: '보리보리', to: '구름이네', content: '혹시 다음에 같이 산책해요!', time: '1시간 전', createdAt: '2026-03-05T11:00:00', isRead: true },
    ],
    isReported: false,
    isHidden: false,
  },
  {
    id: 2,
    user: '초코맘',
    userImg: 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=100&q=80',
    img: 'https://images.unsplash.com/photo-1530281700549-e82e7bf110d6?w=800&q=80',
    content: '가평 멍스테이 글램핑 다녀왔어요. 시설도 깨끗하고 운동장도 넓어서 강추! 👍',
    likes: 89,
    comments: 42,
    dms: 7,
    placeId: 1,
    time: '5시간 전',
    createdAt: '2026-03-05T07:00:00',
    isLiked: false,
    likedBy: ['해피해피', '망고주스'],
    commentList: [
      { id: 1, user: '해피해피', content: '가격 어느정도예요?', time: '4시간 전', createdAt: '2026-03-05T08:00:00' },
      { id: 2, user: '구름이네', content: '저도 다음에 가보고 싶어요!', time: '3시간 전', createdAt: '2026-03-05T09:00:00' },
    ],
    dmList: [
      { id: 1, from: '코코넨네', to: '초코맘', content: '예약 어떻게 하셨어요?', time: '4시간 전', createdAt: '2026-03-05T08:00:00', isRead: false },
      { id: 2, from: '망고주스', to: '초코맘', content: '반려동물 크기 제한 있나요?', time: '3시간 전', createdAt: '2026-03-05T09:00:00', isRead: false },
    ],
    isReported: false,
    isHidden: false,
  },
  {
    id: 3,
    user: '두부아빠',
    userImg: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=100&q=80',
    img: 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=800&q=80',
    content: '친구들이랑 뛰뛰하니까 세상 행복한 표정 ㅋㅋ 다음에도 또 오자!',
    likes: 256,
    comments: 31,
    dms: 5,
    placeId: 4,
    time: '1일 전',
    createdAt: '2026-03-04T14:00:00',
    isLiked: false,
    likedBy: ['구름이네', '초코맘', '해피해피', '보리보리'],
    commentList: [
      { id: 1, user: '보리보리', content: '완전 신나 보여요!', time: '23시간 전', createdAt: '2026-03-04T15:00:00' },
    ],
    dmList: [],
    isReported: false,
    isHidden: false,
  },
];

interface FeedState {
  posts: FeedPost[];
  nextId: number;
  toggleLike: (postId: number) => void;
  addComment: (postId: number, user: string, content: string) => void;
  addDM: (postId: number, from: string, to: string, content: string) => void;
  addPost: (post: Omit<FeedPost, 'id' | 'likes' | 'comments' | 'dms' | 'isLiked' | 'likedBy' | 'commentList' | 'dmList' | 'isReported' | 'isHidden' | 'createdAt'>) => void;
  editPost: (postId: number, content: string) => void;
  toggleHidePost: (postId: number) => void;
  deletePost: (postId: number) => void;
  markDMRead: (postId: number, dmId: number) => void;
  reportPost: (postId: number) => void;
}

export const useFeedStore = create<FeedState>()(
  persist(
    (set, get) => ({
      // TODO: [DB 연동] INITIAL_POSTS 대신 Supabase에서 fetch → posts 테이블 SELECT
      posts: INITIAL_POSTS,
      nextId: 4,
      // TODO: [DB 연동] supabase.from('likes').upsert/delete → 낙관적 업데이트 후 서버 동기화
      toggleLike: (postId) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId
              ? {
                  ...p,
                  isLiked: !p.isLiked,
                  likes: p.isLiked ? p.likes - 1 : p.likes + 1,
                  likedBy: p.isLiked
                    ? p.likedBy.filter((u) => u !== '나')
                    : [...p.likedBy, '나'],
                }
              : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('comments').insert → 실시간 구독(Realtime) 적용 가능
      addComment: (postId, user, content) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId
              ? {
                  ...p,
                  comments: p.comments + 1,
                  commentList: [
                    ...p.commentList,
                    {
                      id: Date.now(),
                      user,
                      content,
                      time: '방금 전',
                      createdAt: new Date().toISOString(),
                    },
                  ],
                }
              : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('dms').insert → 실시간 구독(Realtime) 적용 가능
      addDM: (postId, from, to, content) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId
              ? {
                  ...p,
                  dms: p.dms + 1,
                  dmList: [
                    ...p.dmList,
                    {
                      id: Date.now(),
                      from,
                      to,
                      content,
                      time: '방금 전',
                      createdAt: new Date().toISOString(),
                      isRead: false,
                    },
                  ],
                }
              : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('posts').insert → 이미지는 Supabase Storage 업로드 후 URL 저장
      addPost: (postData) =>
        set((state) => ({
          nextId: state.nextId + 1,
          posts: [
            {
              ...postData,
              id: state.nextId,
              likes: 0,
              comments: 0,
              dms: 0,
              isLiked: false,
              likedBy: [],
              commentList: [],
              dmList: [],
              isReported: false,
              isHidden: false,
              createdAt: new Date().toISOString(),
            },
            ...state.posts,
          ],
        })),
      // TODO: [DB 연동] supabase.from('posts').update({ is_hidden }) → 관리자 권한 체크(RLS) 필요
      toggleHidePost: (postId) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId ? { ...p, isHidden: !p.isHidden } : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('posts').delete → 관리자 권한 체크(RLS) 필요
      deletePost: (postId) =>
        set((state) => ({
          posts: state.posts.filter((p) => p.id !== postId),
        })),
      // TODO: [DB 연동] supabase.from('dms').update({ is_read: true })
      markDMRead: (postId, dmId) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId
              ? {
                  ...p,
                  dmList: p.dmList.map((dm) =>
                    dm.id === dmId ? { ...dm, isRead: true } : dm
                  ),
                }
              : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('posts').update({ is_reported: true }) → 신고 누적 시 자동 숨김 로직 추가
      reportPost: (postId) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId ? { ...p, isReported: true } : p
          ),
        })),
      // TODO: [DB 연동] supabase.from('posts').update({ content }) → 관리자 권한 체크(RLS) 필요
      editPost: (postId, content) =>
        set((state) => ({
          posts: state.posts.map((p) =>
            p.id === postId ? { ...p, content } : p
          ),
        })),
    }),
    {
      // TODO: [DB 연동] persist 미들웨어 제거 → Supabase 실시간 동기화로 대체
      name: 'meongnyang-feed-storage',
    }
  )
);