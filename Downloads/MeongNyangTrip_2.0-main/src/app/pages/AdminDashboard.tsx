import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft, Heart, MessageCircle, Send, Eye, EyeOff, Trash2,
  AlertTriangle, BarChart3, Users, TrendingUp, Mail, Image as ImageIcon,
  ChevronDown, ChevronUp, Search, Filter, Shield
} from 'lucide-react';
import { useFeedStore, type FeedPost } from '../store/useFeedStore';

interface AdminDashboardProps {
  onNavigate: (page: string, params?: any) => void;
}

type TabType = 'overview' | 'posts' | 'comments' | 'dms';
type SortType = 'latest' | 'likes' | 'comments' | 'dms' | 'reported';

export function AdminDashboard({ onNavigate }: AdminDashboardProps) {
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const { posts } = useFeedStore();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Admin Header */}
      <div className="bg-gray-900 text-white sticky top-0 z-50">
        <div className="flex items-center gap-3 px-4 py-3">
          <button onClick={() => onNavigate('home')} className="hover:opacity-70">
            <ArrowLeft size={20} />
          </button>
          <div className="flex items-center gap-2">
            <Shield size={18} className="text-primary" />
            <h1 className="text-base font-bold">관리자 대시보드</h1>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-t border-gray-800">
          {([
            { key: 'overview', label: '전체 현황', icon: BarChart3 },
            { key: 'posts', label: '게시글', icon: ImageIcon },
            { key: 'comments', label: '댓글', icon: MessageCircle },
            { key: 'dms', label: 'DM', icon: Mail },
          ] as { key: TabType; label: string; icon: any }[]).map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex-1 py-2.5 text-xs font-bold transition-colors relative flex items-center justify-center gap-1 ${
                activeTab === tab.key ? 'text-primary' : 'text-gray-500'
              }`}
            >
              <tab.icon size={14} />
              {tab.label}
              {activeTab === tab.key && (
                <motion.div layoutId="admin-tab" className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <AnimatePresence mode="wait">
        {activeTab === 'overview' && <OverviewTab key="overview" posts={posts} />}
        {activeTab === 'posts' && <PostsTab key="posts" posts={posts} />}
        {activeTab === 'comments' && <CommentsTab key="comments" posts={posts} />}
        {activeTab === 'dms' && <DMsTab key="dms" posts={posts} />}
      </AnimatePresence>
    </div>
  );
}

function OverviewTab({ posts }: { posts: FeedPost[] }) {
  const totalLikes = posts.reduce((acc, p) => acc + p.likes, 0);
  const totalComments = posts.reduce((acc, p) => acc + p.comments, 0);
  const totalDMs = posts.reduce((acc, p) => acc + p.dms, 0);
  const reportedPosts = posts.filter(p => p.isReported).length;
  const hiddenPosts = posts.filter(p => p.isHidden).length;
  const unreadDMs = posts.reduce((acc, p) => acc + p.dmList.filter(d => !d.isRead).length, 0);

  const topPost = [...posts].sort((a, b) => b.likes - a.likes)[0];
  const topCommentedPost = [...posts].sort((a, b) => b.comments - a.comments)[0];

  const stats = [
    { label: '전체 게시글', value: posts.length, icon: ImageIcon, color: 'bg-blue-50 text-blue-600' },
    { label: '전체 좋아요', value: totalLikes, icon: Heart, color: 'bg-pink-50 text-primary' },
    { label: '전체 댓글', value: totalComments, icon: MessageCircle, color: 'bg-green-50 text-green-600' },
    { label: '전체 DM', value: totalDMs, icon: Send, color: 'bg-purple-50 text-purple-600' },
    { label: '신고된 글', value: reportedPosts, icon: AlertTriangle, color: 'bg-red-50 text-red-600' },
    { label: '숨김 처리', value: hiddenPosts, icon: EyeOff, color: 'bg-gray-100 text-gray-600' },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-4">
      {/* Stats Grid */}
      <div className="grid grid-cols-3 gap-3">
        {stats.map((s, i) => (
          <div key={i} className="bg-white rounded-2xl p-3 text-center shadow-sm">
            <div className={`w-9 h-9 rounded-full ${s.color} flex items-center justify-center mx-auto mb-2`}>
              <s.icon size={16} />
            </div>
            <div className="text-lg font-bold text-gray-800">{s.value}</div>
            <div className="text-[10px] text-gray-500 font-medium">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Alert Banner */}
      {(reportedPosts > 0 || unreadDMs > 0) && (
        <div className="bg-red-50 border border-red-100 rounded-2xl p-4 flex items-start gap-3">
          <AlertTriangle size={20} className="text-red-500 shrink-0 mt-0.5" />
          <div>
            <h4 className="text-sm font-bold text-red-700">관리자 알림</h4>
            <div className="text-xs text-red-600 space-y-1 mt-1">
              {reportedPosts > 0 && <p>신고된 게시글 {reportedPosts}건이 있습니다.</p>}
              {unreadDMs > 0 && <p>읽지 않은 DM {unreadDMs}건이 있습니다.</p>}
            </div>
          </div>
        </div>
      )}

      {/* Top Posts */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-gray-800 flex items-center gap-1">
          <TrendingUp size={16} className="text-primary" /> 인기 게시글
        </h3>

        {topPost && (
          <div className="bg-white rounded-2xl overflow-hidden shadow-sm">
            <div className="flex gap-3 p-3">
              <div className="w-16 h-16 rounded-xl overflow-hidden bg-gray-100 shrink-0">
                <img src={topPost.img} alt="" className="w-full h-full object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-1">
                  <Heart size={12} className="text-primary fill-primary" />
                  <span className="text-xs font-bold text-primary">좋아요 1위</span>
                </div>
                <p className="text-xs text-gray-800 font-bold truncate">{topPost.user}</p>
                <p className="text-[11px] text-gray-500 truncate">{topPost.content}</p>
                <div className="flex items-center gap-3 mt-1.5">
                  <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                    <Heart size={10} /> {topPost.likes}
                  </span>
                  <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                    <MessageCircle size={10} /> {topPost.comments}
                  </span>
                  <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                    <Send size={10} /> {topPost.dms}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {topCommentedPost && topCommentedPost.id !== topPost?.id && (
          <div className="bg-white rounded-2xl overflow-hidden shadow-sm">
            <div className="flex gap-3 p-3">
              <div className="w-16 h-16 rounded-xl overflow-hidden bg-gray-100 shrink-0">
                <img src={topCommentedPost.img} alt="" className="w-full h-full object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-1">
                  <MessageCircle size={12} className="text-green-600" />
                  <span className="text-xs font-bold text-green-600">댓글 1위</span>
                </div>
                <p className="text-xs text-gray-800 font-bold truncate">{topCommentedPost.user}</p>
                <p className="text-[11px] text-gray-500 truncate">{topCommentedPost.content}</p>
                <div className="flex items-center gap-3 mt-1.5">
                  <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                    <Heart size={10} /> {topCommentedPost.likes}
                  </span>
                  <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                    <MessageCircle size={10} /> {topCommentedPost.comments}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Users Activity */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-gray-800 flex items-center gap-1">
          <Users size={16} className="text-blue-500" /> 사용자 활동
        </h3>
        <div className="bg-white rounded-2xl p-4 shadow-sm space-y-3">
          {getUniqueUsers(posts).map((user, i) => (
            <div key={user.name} className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-100">
                  <img src={user.img} alt="" className="w-full h-full object-cover" />
                </div>
                <span className="text-sm font-bold text-gray-800">{user.name}</span>
              </div>
              <div className="flex items-center gap-3 text-[10px] text-gray-500">
                <span className="flex items-center gap-0.5"><ImageIcon size={10} /> {user.posts}</span>
                <span className="flex items-center gap-0.5"><Heart size={10} /> {user.totalLikes}</span>
                <span className="flex items-center gap-0.5"><MessageCircle size={10} /> {user.totalComments}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

function getUniqueUsers(posts: FeedPost[]) {
  const userMap = new Map<string, { name: string; img: string; posts: number; totalLikes: number; totalComments: number }>();
  posts.forEach(p => {
    const existing = userMap.get(p.user);
    if (existing) {
      existing.posts++;
      existing.totalLikes += p.likes;
      existing.totalComments += p.comments;
    } else {
      userMap.set(p.user, {
        name: p.user,
        img: p.userImg,
        posts: 1,
        totalLikes: p.likes,
        totalComments: p.comments,
      });
    }
  });
  return Array.from(userMap.values()).sort((a, b) => b.totalLikes - a.totalLikes);
}

function PostsTab({ posts }: { posts: FeedPost[] }) {
  const { toggleHidePost, deletePost, reportPost } = useFeedStore();
  const [sortBy, setSortBy] = useState<SortType>('latest');
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedPost, setExpandedPost] = useState<number | null>(null);

  const filteredPosts = posts
    .filter(p => 
      searchTerm === '' ||
      p.user.includes(searchTerm) ||
      p.content.includes(searchTerm)
    )
    .sort((a, b) => {
      switch (sortBy) {
        case 'likes': return b.likes - a.likes;
        case 'comments': return b.comments - a.comments;
        case 'dms': return b.dms - a.dms;
        case 'reported': return (b.isReported ? 1 : 0) - (a.isReported ? 1 : 0);
        default: return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      {/* Search & Filter */}
      <div className="flex gap-2">
        <div className="flex-1 bg-white rounded-xl flex items-center gap-2 px-3 shadow-sm">
          <Search size={16} className="text-gray-400" />
          <input
            type="text"
            placeholder="사용자명 또는 내용 검색..."
            className="flex-1 py-2.5 text-sm outline-none bg-transparent placeholder:text-gray-400"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as SortType)}
          className="bg-white rounded-xl px-3 py-2.5 text-xs font-bold text-gray-600 shadow-sm outline-none"
        >
          <option value="latest">최신순</option>
          <option value="likes">좋아요순</option>
          <option value="comments">댓글순</option>
          <option value="dms">DM순</option>
          <option value="reported">신고순</option>
        </select>
      </div>

      <div className="text-xs text-gray-500 font-medium">총 {filteredPosts.length}개 게시글</div>

      {/* Post List */}
      {filteredPosts.map(post => (
        <div
          key={post.id}
          className={`bg-white rounded-2xl overflow-hidden shadow-sm border ${
            post.isReported ? 'border-red-200' : post.isHidden ? 'border-gray-300 opacity-60' : 'border-transparent'
          }`}
        >
          <div
            className="flex gap-3 p-3 cursor-pointer"
            onClick={() => setExpandedPost(expandedPost === post.id ? null : post.id)}
          >
            <div className="w-14 h-14 rounded-xl overflow-hidden bg-gray-100 shrink-0 relative">
              <img src={post.img} alt="" className="w-full h-full object-cover" />
              {post.isHidden && (
                <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                  <EyeOff size={14} className="text-white" />
                </div>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between mb-0.5">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-gray-800">{post.user}</span>
                  {post.isReported && (
                    <span className="text-[10px] bg-red-100 text-red-600 px-1.5 py-0.5 rounded-full font-bold">신고됨</span>
                  )}
                  {post.isHidden && (
                    <span className="text-[10px] bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded-full font-bold">숨김</span>
                  )}
                </div>
                {expandedPost === post.id ? <ChevronUp size={14} className="text-gray-400" /> : <ChevronDown size={14} className="text-gray-400" />}
              </div>
              <p className="text-[11px] text-gray-600 truncate">{post.content}</p>
              <div className="flex items-center gap-3 mt-1">
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <Heart size={10} className={post.likes > 0 ? 'text-primary' : ''} /> {post.likes}
                </span>
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <MessageCircle size={10} /> {post.comments}
                </span>
                <span className="text-[10px] text-gray-500 flex items-center gap-0.5">
                  <Send size={10} /> {post.dms}
                </span>
                <span className="text-[10px] text-gray-400 ml-auto">{post.time}</span>
              </div>
            </div>
          </div>

          <AnimatePresence>
            {expandedPost === post.id && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="overflow-hidden"
              >
                <div className="px-3 pb-3 border-t border-gray-100 pt-3">
                  {/* Liked By */}
                  {post.likedBy.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">좋아요 ({post.likedBy.length})</h5>
                      <div className="flex flex-wrap gap-1">
                        {post.likedBy.map(u => (
                          <span key={u} className="text-[10px] bg-pink-50 text-primary px-2 py-0.5 rounded-full">{u}</span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Recent Comments */}
                  {post.commentList.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">최근 댓글 ({post.commentList.length})</h5>
                      <div className="space-y-1">
                        {post.commentList.slice(-3).map(c => (
                          <div key={c.id} className="bg-gray-50 rounded-lg px-2.5 py-1.5">
                            <span className="text-[11px] font-bold text-gray-700">{c.user}</span>
                            <span className="text-[11px] text-gray-600 ml-1">{c.content}</span>
                            <span className="text-[10px] text-gray-400 ml-1">{c.time}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* DM summary */}
                  {post.dmList.length > 0 && (
                    <div className="mb-3">
                      <h5 className="text-[10px] font-bold text-gray-500 mb-1 uppercase">DM ({post.dmList.length})</h5>
                      <div className="space-y-1">
                        {post.dmList.slice(-3).map(d => (
                          <div key={d.id} className={`rounded-lg px-2.5 py-1.5 flex items-center gap-1 ${d.isRead ? 'bg-gray-50' : 'bg-blue-50'}`}>
                            {!d.isRead && <div className="w-1.5 h-1.5 rounded-full bg-blue-500 shrink-0" />}
                            <span className="text-[11px] font-bold text-gray-700">{d.from}</span>
                            <span className="text-[11px] text-gray-500">→</span>
                            <span className="text-[11px] font-bold text-gray-700">{d.to}</span>
                            <span className="text-[11px] text-gray-600 ml-1 truncate">{d.content}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Admin Actions */}
                  <div className="flex gap-2 pt-2 border-t border-gray-100">
                    <button
                      onClick={() => toggleHidePost(post.id)}
                      className={`flex-1 text-xs font-bold py-2 rounded-xl transition-colors flex items-center justify-center gap-1 ${
                        post.isHidden
                          ? 'bg-green-50 text-green-600 hover:bg-green-100'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {post.isHidden ? <><Eye size={14} /> 공개</> : <><EyeOff size={14} /> 숨김</>}
                    </button>
                    <button
                      onClick={() => {
                        if (confirm('정말 이 게시글을 삭제하시겠습니까?')) {
                          deletePost(post.id);
                        }
                      }}
                      className="flex-1 text-xs font-bold py-2 rounded-xl bg-red-50 text-red-600 hover:bg-red-100 transition-colors flex items-center justify-center gap-1"
                    >
                      <Trash2 size={14} /> 삭제
                    </button>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      ))}
    </motion.div>
  );
}

function CommentsTab({ posts }: { posts: FeedPost[] }) {
  const allComments = posts
    .flatMap(p => p.commentList.map(c => ({ ...c, postId: p.id, postUser: p.user, postImg: p.img })))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-gray-800">전체 댓글</h3>
        <span className="text-xs text-gray-500 font-medium">{allComments.length}개</span>
      </div>

      {allComments.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <MessageCircle size={32} className="mx-auto mb-2 opacity-30" />
          <p className="text-sm">아직 댓글이 없습니다.</p>
        </div>
      ) : (
        allComments.map(c => (
          <div key={`${c.postId}-${c.id}`} className="bg-white rounded-2xl p-3 shadow-sm">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-xl overflow-hidden bg-gray-100 shrink-0">
                <img src={c.postImg} alt="" className="w-full h-full object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-0.5">
                  <span className="text-[10px] text-gray-400">on</span>
                  <span className="text-[10px] font-bold text-gray-500">{c.postUser}의 게시글</span>
                </div>
                <div className="text-sm">
                  <span className="font-bold text-gray-800 mr-1">{c.user}</span>
                  <span className="text-gray-700">{c.content}</span>
                </div>
                <span className="text-[10px] text-gray-400">{c.time}</span>
              </div>
            </div>
          </div>
        ))
      )}
    </motion.div>
  );
}

function DMsTab({ posts }: { posts: FeedPost[] }) {
  const { markDMRead } = useFeedStore();
  const allDMs = posts
    .flatMap(p => p.dmList.map(d => ({ ...d, postId: p.id, postUser: p.user })))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const unreadCount = allDMs.filter(d => !d.isRead).length;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-4 pb-24 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-gray-800">전체 DM</h3>
        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <span className="text-[10px] bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-bold">
              읽지않음 {unreadCount}
            </span>
          )}
          <span className="text-xs text-gray-500 font-medium">{allDMs.length}개</span>
        </div>
      </div>

      {allDMs.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <Mail size={32} className="mx-auto mb-2 opacity-30" />
          <p className="text-sm">아직 DM이 없습니다.</p>
        </div>
      ) : (
        allDMs.map(d => (
          <div
            key={`${d.postId}-${d.id}`}
            className={`bg-white rounded-2xl p-3 shadow-sm border transition-colors ${
              d.isRead ? 'border-transparent' : 'border-blue-200 bg-blue-50/30'
            }`}
          >
            <div className="flex items-start gap-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                d.isRead ? 'bg-gray-100' : 'bg-blue-100'
              }`}>
                <Mail size={14} className={d.isRead ? 'text-gray-400' : 'text-blue-600'} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-0.5">
                  <span className="text-xs font-bold text-gray-800">{d.from}</span>
                  <span className="text-[10px] text-gray-400">→</span>
                  <span className="text-xs font-bold text-gray-800">{d.to}</span>
                  {!d.isRead && <div className="w-1.5 h-1.5 rounded-full bg-blue-500 ml-1" />}
                </div>
                <p className="text-sm text-gray-700">{d.content}</p>
                <div className="flex items-center justify-between mt-1">
                  <span className="text-[10px] text-gray-400">{d.time}</span>
                  {!d.isRead && (
                    <button
                      onClick={() => markDMRead(d.postId, d.id)}
                      className="text-[10px] text-blue-600 font-bold hover:underline"
                    >
                      읽음 처리
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))
      )}
    </motion.div>
  );
}