import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Heart,
  MessageCircle,
  Send,
  MapPin,
  MoreHorizontal,
  Image as ImageIcon,
  Camera,
  X,
  Clock,
  Pencil,
  Trash2,
  Search,
} from "lucide-react";
import { useFeedStore, type FeedPost } from "../store/useFeedStore";
import { useAppStore } from "../store/useAppStore";
import { ShareSheet } from "../components/ShareSheet";

interface LoungeProps {
  onNavigate: (page: string, params?: any) => void;
}

const SAMPLE_IMAGES = [
  "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&q=80",
  "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=800&q=80",
  "https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=800&q=80",
  "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=800&q=80",
];

export function Lounge({ onNavigate }: LoungeProps) {
  const [activeTab, setActiveTab] = useState<"feed" | "talk">("feed");
  const [isWriteModalOpen, setIsWriteModalOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isLiveTalkModalOpen, setIsLiveTalkModalOpen] = useState(false);
  const [selectedTalk, setSelectedTalk] = useState<any | null>(null);
  const [isCommentSheetOpen, setIsCommentSheetOpen] = useState(false);

  const { posts, talks, addPost, addTalk, addTalkComment, fetchPosts, fetchTalks } = useFeedStore();

  useEffect(() => {
    fetchPosts();
    fetchTalks();
  }, []);

  const places = useAppStore((s) => s.places);

  const handleCreatePost = (
    content: string,
    imgSource: any,
    placeId?: number,
  ) => {
    const finalImg =
      typeof imgSource === "number"
        ? SAMPLE_IMAGES[imgSource % SAMPLE_IMAGES.length]
        : imgSource;

    addPost({
      content,
      imageUrl: finalImg,
      placeId,
    });
    setIsWriteModalOpen(false);
  };

  const handleCreateTalk = async (content: string, _location: string) => {
    try {
      await addTalk(content);
      setIsLiveTalkModalOpen(false);
    } catch (e) {
      alert("산책 톡 등록에 실패했어요. 다시 시도해줘요.");
    }
  };

  const handleCreateTalkComment = async (talkId: number, content: string) => {
    if (!content.trim()) return;
    try {
      await addTalkComment(talkId, content);
    } catch (e) {
      alert("댓글 등록에 실패했어요.");
    }
  };

  return (
    <div className="px-[0px] pt-[0px] pb-[96px]">
      {/* 탭 전환 */}
      <div className="sticky top-[0px] z-40 bg-white/95 backdrop-blur-sm border-b border-gray-100 flex">
        <button
          onClick={() => setActiveTab("feed")}
          className={`flex-1 py-3 text-sm font-bold transition-spring relative ${
            activeTab === "feed" ? "text-gray-900" : "text-gray-400"
          }`}
        >
          {" "}
          피드
          <ImageIcon className="inline-block mr-1" size={16} />
          {activeTab === "feed" && (
            <motion.div
              layoutId="tab-indicator"
              className="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900"
            />
          )}
        </button>
        <button
          onClick={() => setActiveTab("talk")}
          className={`flex-1 py-3 text-sm font-bold transition-spring relative ${
            activeTab === "talk" ? "text-gray-900" : "text-gray-400"
          }`}
        >
          <MessageCircle className="inline-block mr-1" size={16} /> 실시간 톡
          {activeTab === "talk" && (
            <motion.div
              layoutId="tab-indicator"
              className="absolute bottom-0 left-0 right-0 h-0.5 bg-gray-900"
            />
          )}
        </button>
      </div>

      <div className="min-h-screen bg-gray-50">
        <AnimatePresence mode="wait">
          {activeTab === "feed" ? (
            <FeedView
              key="feed"
              posts={posts.filter((p) => !p.isHidden && p.img)}
              onNavigate={onNavigate}
            />
          ) : (
            <WalkTalkView
              key="talk"
              talks={talks}
              onTalkClick={(talkData) => {
                if (talkData.currentInput) {
                  handleCreateTalkComment(talkData.id, talkData.currentInput);
                } else {
                  setSelectedTalk(talkData);
                  setIsCommentSheetOpen(true);
                }
              }}
            />
          )}
        </AnimatePresence>
      </div>

      {/* 메인 플로팅 버튼 */}
      <div className="fixed bottom-40 inset-x-0 flex justify-center pointer-events-none z-50">
        <div className="relative w-full max-w-[600px]">
          <div className="absolute bottom-0 left-full ml-3 pointer-events-auto">
            <button
              onClick={() => setIsMenuOpen(true)}
              className="w-14 h-14 bg-primary text-white rounded-full shadow-[0_4px_20px_rgba(227,99,148,0.4)] flex items-center justify-center hover:scale-105 active:scale-[0.97] transition-transform pointer-events-auto"
            >
              <Camera size={22} />
            </button>
          </div>
        </div>
      </div>

      {/* 메뉴 선택 팝업 */}
      <AnimatePresence>
        {isMenuOpen && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm pointer-events-auto">
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="bg-white rounded-[32px] p-8 w-full max-w-[320px] shadow-2xl relative"
            >
              <button
                onClick={() => setIsMenuOpen(false)}
                className="absolute top-5 right-5 text-gray-400"
              >
                <X size={24} />
              </button>

              <h3 className="text-center font-bold text-gray-900 mb-8 text-lg">
                어떤 글을 남길까요?
              </h3>

              <div className="flex flex-col gap-4">
                <button
                  onClick={() => {
                    setActiveTab("feed");
                    setIsWriteModalOpen(true);
                    setIsMenuOpen(false);
                  }}
                  className="flex items-center gap-4 p-4 bg-pink-50 rounded-2xl border border-pink-100 hover:bg-pink-100 transition-colors"
                >
                  <div className="w-12 h-12 bg-primary rounded-full flex items-center justify-center text-white shadow-lg">
                    <Camera size={24} />
                  </div>
                  <div className="text-left">
                    <p className="font-bold text-gray-900">게시물 작성</p>
                    <p className="text-xs text-pink-400">오늘의 산책을 공유해요</p>
                  </div>
                </button>

                <button
                  onClick={() => {
                    setActiveTab("talk");
                    setIsMenuOpen(false);
                    setIsLiveTalkModalOpen(true);
                  }}
                  className="flex items-center gap-4 p-4 bg-blue-50 rounded-2xl border border-blue-100 hover:bg-blue-100 transition-colors"
                >
                  <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center text-white shadow-lg">
                    <MessageCircle size={24} />
                  </div>
                  <div className="text-left">
                    <p className="font-bold text-gray-900">실시간 톡</p>
                    <p className="text-xs text-blue-400">이웃과 바로 대화해요</p>
                  </div>
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 게시물 작성 모달 */}
      <AnimatePresence>
        {isWriteModalOpen && (
          <WriteModal
            onClose={() => setIsWriteModalOpen(false)}
            onSubmit={handleCreatePost}
          />
        )}
      </AnimatePresence>

      {/* 실시간 톡 작성 모달 */}
      <AnimatePresence>
        {isLiveTalkModalOpen && (
          <LiveTalkWriteModal
            onClose={() => setIsLiveTalkModalOpen(false)}
            onSubmit={handleCreateTalk}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function WriteModal({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (content: string, imgIndex: any, placeId?: number) => void;
}) {
  const [content, setContent] = useState("");
  const [myImages, setMyImages] = useState<string[]>([]);
  const [s3Urls, setS3Urls] = useState<string[]>([]);
  const [selectedImg, setSelectedImg] = useState<number | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      alert("이미지 파일만 업로드 가능합니다.");
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setMyImages([reader.result as string]);
      setSelectedImg(0);
    };
    reader.readAsDataURL(file);

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const token = localStorage.getItem("accessToken");
      const res = await fetch("/api/v1/upload/image", {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      const data = await res.json();
      if (data?.data?.url) {
        setS3Urls([data.data.url]);
      } else {
        alert("이미지 업로드에 실패했어요. 다시 시도해주세요.");
        setMyImages([]);
        setSelectedImg(null);
      }
    } catch (err) {
      alert("이미지 업로드 중 오류가 발생했어요.");
      setMyImages([]);
      setSelectedImg(null);
    } finally {
      setUploading(false);
    }
  };

  const [showPlaceSearch, setShowPlaceSearch] = useState(false);
  const [placeQuery, setPlaceQuery] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | undefined>(undefined);
  const places = useAppStore((s) => s.places);

  const filteredPlaces = placeQuery.trim()
    ? places.filter(
        (p) => p.title.includes(placeQuery) || p.address.includes(placeQuery),
      )
    : places;

  const selectedPlace = selectedPlaceId
    ? places.find((p) => p.id === selectedPlaceId)
    : undefined;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        className="bg-white w-full max-w-sm rounded-3xl p-6 relative shadow-2xl z-10"
      >
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
        >
          <X size={24} />
        </button>

        <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
          <Camera className="text-primary" /> 게시글 작성
        </h3>

        <div className="space-y-4">
          <div className="grid grid-cols-4 gap-2">
            <div
              onClick={() => fileInputRef.current?.click()}
              className="aspect-square rounded-xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center cursor-pointer hover:bg-gray-50 transition-all"
            >
              <Camera size={20} className="text-gray-400" />
              <span className="text-[10px] text-gray-400 mt-1">사진 추가</span>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                accept="image/*"
                onChange={handleFileChange}
              />
            </div>

            {myImages.map((img, i) => (
              <div
                key={i}
                onClick={() => setSelectedImg(i)}
                className={`relative aspect-square rounded-xl overflow-hidden cursor-pointer border-2 transition-spring ${
                  selectedImg === i ? "border-primary scale-95" : "border-transparent"
                }`}
              >
                <img src={img} alt="upload" className="w-full h-full object-cover" />
                {selectedImg === i && (
                  <div className="absolute inset-0 bg-primary/10 flex items-center justify-center" />
                )}
              </div>
            ))}
          </div>

          <textarea
            placeholder="오늘의 산책 이야기를 들려주세요!"
            className="w-full h-24 bg-gray-50 rounded-xl p-3 text-sm outline-none resize-none placeholder:text-gray-400"
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />

          <div className="relative">
            {selectedPlace ? (
              <div
                className="flex items-center justify-between p-3 bg-primary/5 rounded-xl cursor-pointer border border-primary/20"
                onClick={() => {
                  setSelectedPlaceId(undefined);
                  setShowPlaceSearch(false);
                  setPlaceQuery("");
                }}
              >
                <div className="flex items-center gap-2">
                  <MapPin size={16} className="text-primary" />
                  <span className="text-sm text-primary font-bold">{selectedPlace.title}</span>
                  <span className="text-[11px] text-gray-400">{selectedPlace.address}</span>
                </div>
                <X size={14} className="text-gray-400" />
              </div>
            ) : (
              <div
                className="flex items-center gap-2 p-3 bg-gray-50 rounded-xl text-gray-500 cursor-pointer hover:bg-gray-100"
                onClick={() => setShowPlaceSearch(!showPlaceSearch)}
              >
                <MapPin size={16} className="text-primary" />
                <span className="text-sm">장소 태그하기</span>
              </div>
            )}

            <AnimatePresence>
              {showPlaceSearch && !selectedPlace && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden mt-2"
                >
                  <div className="flex items-center gap-2 bg-gray-50 rounded-xl px-3 py-2 mb-2">
                    <Search size={14} className="text-gray-400" />
                    <input
                      type="text"
                      placeholder="장소명 또는 지역으로 검색"
                      className="flex-1 text-sm bg-transparent outline-none placeholder:text-gray-400"
                      value={placeQuery}
                      onChange={(e) => setPlaceQuery(e.target.value)}
                      autoFocus
                    />
                    {placeQuery && (
                      <button onClick={() => setPlaceQuery("")}>
                        <X size={14} className="text-gray-400" />
                      </button>
                    )}
                  </div>
                  <div className="max-h-[140px] overflow-y-auto space-y-1">
                    {filteredPlaces.length > 0 ? (
                      filteredPlaces.map((place) => (
                        <button
                          key={place.id}
                          className="flex items-center gap-2 w-full p-2 rounded-lg hover:bg-gray-50 transition-spring text-left"
                          onClick={() => {
                            setSelectedPlaceId(place.id);
                            setShowPlaceSearch(false);
                            setPlaceQuery("");
                          }}
                        >
                          <div className="w-8 h-8 rounded-lg overflow-hidden bg-gray-200 flex-shrink-0 flex items-center justify-center">
                            {place.imageUrl ? (
                              <img
                                src={place.imageUrl}
                                alt={place.title}
                                className="w-full h-full object-cover"
                                onError={(e) => {
                                  (e.target as HTMLImageElement).style.display = "none";
                                }}
                              />
                            ) : (
                              <MapPin size={14} className="text-gray-400" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <div className="text-sm font-bold text-gray-800 truncate">{place.title}</div>
                            <div className="text-[11px] text-gray-400">{place.address}</div>
                          </div>
                        </button>
                      ))
                    ) : (
                      <div className="text-center py-3 text-xs text-gray-400">검색 결과가 없습니다.</div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            className="w-full bg-primary text-white font-bold py-3 rounded-xl hover:bg-primary/90 active:scale-[0.97] transition-spring disabled:opacity-50"
            disabled={!content.trim() || selectedImg === null || uploading || s3Urls.length === 0}
            onClick={() => {
              if (selectedImg !== null && s3Urls[selectedImg]) {
                onSubmit(content, s3Urls[selectedImg] as any, selectedPlaceId);
              }
            }}
          >
            {uploading ? "이미지 업로드 중..." : "등록하기"}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

function FeedView({
  posts,
  onNavigate,
}: {
  posts: FeedPost[];
  onNavigate: (page: string, params?: any) => void;
}) {
  const { toggleLike, addComment, deletePost, editPost, deleteComment, editComment } = useFeedStore();
  const places = useAppStore((s) => s.places);
  const isAdmin = useAppStore((s) => s.isAdmin);

  const [commentingPostId, setCommentingPostId] = useState<number | null>(null);
  const [commentText, setCommentText] = useState("");
  const [sharePostData, setSharePostData] = useState<{
    id: number;
    img: string;
    user: string;
  } | null>(null);

  // ─── 게시글 메뉴/수정 state (게시글 전용) ───────────────────────
  const [postMenuId, setPostMenuId] = useState<number | null>(null);
  const [editingPost, setEditingPost] = useState<{ id: number; content: string } | null>(null);

  // ─── 댓글 메뉴/수정 state (댓글 전용, 게시글과 완전히 분리) ──────
  const [commentMenuId, setCommentMenuId] = useState<number | null>(null);
  const [editingComment, setEditingComment] = useState<{
    postId: number;
    commentId: number;
    content: string;
  } | null>(null);
  const [editCommentValue, setEditCommentValue] = useState("");

  const handleAddComment = (postId: number) => {
    if (!commentText.trim()) return;
    addComment(postId, "나", commentText.trim());
    setCommentText("");
    setCommentingPostId(null);
  };

  // 댓글 삭제 — store의 deleteComment(API 호출) 사용
  const onDeleteComment = (postId: number, commentId: number) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      deleteComment(postId, commentId);
    }
    setCommentMenuId(null);
  };

  // 댓글 수정 저장 — store의 editComment(API 호출) 사용
  const onSaveComment = async (postId: number, commentId: number) => {
    if (!editCommentValue.trim()) return;
    try {
      await editComment(postId, commentId, editCommentValue.trim());
    } catch {
      alert("댓글 수정에 실패했어요.");
    }
    setEditingComment(null);
    setEditCommentValue("");
  };

  const feedContent = (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="pb-4"
    >
      {posts.map((post) => (
        <article key={post.id} className="bg-white mb-4 border-b border-gray-100 last:border-0">
          {/* 게시글 헤더 */}
          <div className="flex items-center justify-between p-3 relative">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-200">
                <img src={post.userImg} alt={post.user} className="w-full h-full object-cover" />
              </div>
              <span className="text-sm font-bold text-gray-800">{post.user}</span>
            </div>

            {/* 게시글 ··· 버튼 — 본인 or 관리자만 */}
            {(post.isOwner || isAdmin) && (
              <button
                className="text-gray-400 hover:text-gray-600 relative"
                onClick={() => setPostMenuId(postMenuId === post.id ? null : post.id)}
              >
                <MoreHorizontal size={20} />
              </button>
            )}

            <AnimatePresence>
              {postMenuId === post.id && (post.isOwner || isAdmin) && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setPostMenuId(null)} />
                  <motion.div
                    initial={{ opacity: 0, scale: 0.9, y: -4 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -4 }}
                    transition={{ duration: 0.15 }}
                    className="absolute right-3 top-12 z-20 bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden min-w-[120px]"
                  >
                    <button
                      className="flex items-center gap-2 w-full px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                      onClick={() => {
                        setEditingPost({ id: post.id, content: post.content });
                        setPostMenuId(null);
                      }}
                    >
                      <Pencil size={15} className="text-gray-500" />
                      수정
                    </button>
                    <div className="h-px bg-gray-100" />
                    <button
                      className="flex items-center gap-2 w-full px-4 py-3 text-sm text-red-500 hover:bg-red-50 transition-spring"
                      onClick={() => {
                        if (confirm("정말 이 게시물을 삭제할까요?")) {
                          deletePost(post.id);
                        }
                        setPostMenuId(null);
                      }}
                    >
                      <Trash2 size={15} />
                      삭제
                    </button>
                  </motion.div>
                </>
              )}
            </AnimatePresence>
          </div>

          {/* 게시글 이미지 */}
          <div
            className="relative aspect-square bg-gray-100 cursor-pointer"
            onDoubleClick={() => toggleLike(post.id)}
          >
            <img src={post.img} alt="Feed" className="w-full h-full object-cover" />
          </div>

          {/* 액션 버튼 */}
          <div className="p-3">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-4">
                <button className="hover:scale-110 active:scale-95 transition-transform" onClick={() => toggleLike(post.id)}>
                  <Heart
                    size={24}
                    className={post.isLiked ? "fill-primary text-primary transition-colors" : "text-gray-800 hover:text-primary transition-colors"}
                  />
                </button>
                <button
                  className="text-gray-800 hover:text-gray-600"
                  onClick={() => setCommentingPostId(commentingPostId === post.id ? null : post.id)}
                >
                  <MessageCircle size={24} />
                </button>
                <button
                  className="text-gray-800 hover:text-gray-600"
                  onClick={() => setSharePostData({ id: post.id, img: post.img, user: post.user })}
                >
                  <Send size={24} />
                </button>
              </div>
            </div>

            <div className="font-bold text-sm mb-1">좋아요 {post.likes}개</div>
            <div className="text-sm text-gray-800 mb-2">
              <span className="font-bold mr-2">{post.user}</span>
              {post.content}
            </div>

            {post.comments > 0 && (
              <button
                className="text-xs text-gray-400 mb-1"
                onClick={() => setCommentingPostId(commentingPostId === post.id ? null : post.id)}
              >
                댓글 {post.comments}개 모두 보기
              </button>
            )}

            <AnimatePresence>
              {commentingPostId === post.id && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden"
                >
                  {post.commentList.slice(-3).map((c) => (
                    <div key={c.id} className="flex items-start justify-between mb-1 relative text-sm min-h-[24px]">
                      <div className="flex-1 pr-2">
                        <span className="font-bold mr-1">{c.user}</span>
                        {/* 댓글 인라인 수정 — editingComment로 판별 (게시글 수정 state와 완전히 분리) */}
                        {editingComment?.commentId === c.id ? (
                          <div className="inline-flex items-center gap-2">
                            <input
                              className="border-b border-primary outline-none bg-transparent py-0 h-auto text-sm"
                              value={editCommentValue}
                              onChange={(e) => setEditCommentValue(e.target.value)}
                              autoFocus
                              onKeyDown={(e) => {
                                if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                                  onSaveComment(post.id, c.id);
                                }
                              }}
                            />
                            <button
                              onClick={() => onSaveComment(post.id, c.id)}
                              className="text-[10px] text-primary font-bold"
                            >
                              저장
                            </button>
                            <button
                              onClick={() => { setEditingComment(null); setEditCommentValue(""); }}
                              className="text-[10px] text-gray-400"
                            >
                              취소
                            </button>
                          </div>
                        ) : (
                          <span className="text-gray-700">{c.content}</span>
                        )}
                      </div>

                      {/* 댓글 ··· 버튼 — 본인 댓글만 표시 */}
                      {editingComment?.commentId !== c.id && c.isOwner && (
                        <div className="relative flex-shrink-0">
                          <button
                            onClick={() => setCommentMenuId(commentMenuId === c.id ? null : c.id)}
                            className="p-1 text-gray-300 hover:text-gray-600 transition-colors"
                          >
                            <MoreHorizontal size={14} />
                          </button>
                          {commentMenuId === c.id && (
                            <>
                              <div className="fixed inset-0 z-10" onClick={() => setCommentMenuId(null)} />
                              <div className="absolute right-0 top-6 z-20 bg-white shadow-xl border border-gray-100 rounded-lg overflow-hidden min-w-[70px]">
                                <button
                                  className="w-full px-3 py-2 text-[11px] text-left hover:bg-gray-50 flex items-center gap-2 text-gray-700"
                                  onClick={() => {
                                    setEditingComment({ postId: post.id, commentId: c.id, content: c.content });
                                    setEditCommentValue(c.content);
                                    setCommentMenuId(null);
                                  }}
                                >
                                  <Pencil size={10} /> 수정
                                </button>
                                <button
                                  className="w-full px-3 py-2 text-[11px] text-left hover:bg-red-50 text-red-500 flex items-center gap-2"
                                  onClick={() => onDeleteComment(post.id, c.id)}
                                >
                                  <Trash2 size={10} /> 삭제
                                </button>
                              </div>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  ))}

                  <div className="flex items-center gap-2 mt-2 border-t border-gray-100 pt-2">
                    <input
                      type="text"
                      placeholder="댓글 달기..."
                      className="flex-1 text-sm outline-none bg-transparent placeholder:text-gray-400"
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      onKeyDown={(e) => e.key === "Enter" && handleAddComment(post.id)}
                    />
                    <button
                      className="text-primary text-sm font-bold disabled:opacity-30"
                      disabled={!commentText.trim()}
                      onClick={() => handleAddComment(post.id)}
                    >
                      게시
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {post.placeId && (
              <div
                className="inline-flex items-center gap-1 bg-primary/5 text-primary px-2 py-1 rounded text-xs font-bold cursor-pointer hover:bg-primary/10 transition-spring mb-2 mt-1"
                onClick={() => {
                  const place = places.find((p) => p.id === post.placeId);
                  if (place) onNavigate("detail", { id: place.id });
                }}
              >
                <MapPin size={12} />
                {places.find((p) => p.id === post.placeId)?.title || "알 수 없는 장소"}
              </div>
            )}

            <div className="text-[10px] text-gray-400 uppercase tracking-wide">{post.time}</div>
          </div>
        </article>
      ))}
    </motion.div>
  );

  return (
    <>
      {feedContent}
      <ShareSheet
        isOpen={!!sharePostData}
        onClose={() => setSharePostData(null)}
        postId={sharePostData?.id ?? 0}
        postImage={sharePostData?.img ?? ""}
        postUser={sharePostData?.user ?? ""}
      />
      {/* 게시글 수정 모달 — editingPost state만 사용 (댓글 수정 state와 완전히 분리) */}
      <AnimatePresence>
        {editingPost && (
          <EditPostModal
            content={editingPost.content}
            onClose={() => setEditingPost(null)}
            onSave={(newContent) => {
              editPost(editingPost.id, newContent);
              setEditingPost(null);
            }}
          />
        )}
      </AnimatePresence>
    </>
  );
}

function EditPostModal({
  content,
  onClose,
  onSave,
}: {
  content: string;
  onClose: () => void;
  onSave: (content: string) => void;
}) {
  const [text, setText] = useState(content);

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        className="bg-white w-full max-w-sm rounded-3xl p-6 relative shadow-2xl z-10"
      >
        <button onClick={onClose} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
          <X size={24} />
        </button>
        <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
          <Pencil className="text-primary" size={20} /> 게시글 수정
        </h3>
        <textarea
          placeholder="내용을 입력하세요"
          className="w-full h-32 bg-gray-50 rounded-xl p-3 text-sm outline-none resize-none placeholder:text-gray-400"
          value={text}
          onChange={(e) => setText(e.target.value)}
          autoFocus
        />
        <button
          className="w-full mt-4 bg-primary text-white font-bold py-3 rounded-xl hover:bg-primary/90 active:scale-[0.97] transition-spring disabled:opacity-50"
          disabled={!text.trim()}
          onClick={() => onSave(text.trim())}
        >
          수정 완료
        </button>
      </motion.div>
    </div>
  );
}

function WalkTalkView({
  talks,
  onTalkClick,
}: {
  talks: FeedPost[];
  onTalkClick: (talk: any) => void;
}) {
  const [activeTalkId, setActiveTalkId] = React.useState<number | null>(null);
  const [commentValues, setCommentValues] = useState<{ [key: number]: string }>({});

  // 댓글 수정/삭제 state (피드와 동일한 구조)
  const [commentMenuId, setCommentMenuId] = useState<number | null>(null);
  const [editingComment, setEditingComment] = useState<{
    postId: number;
    commentId: number;
    content: string;
  } | null>(null);
  const [editCommentValue, setEditCommentValue] = useState("");

  const { editComment, deleteComment } = useFeedStore();
  const places = useAppStore((s) => s.places);

  const onDeleteTalkComment = (talkId: number, commentId: number) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      deleteComment(talkId, commentId);
    }
    setCommentMenuId(null);
  };

  const onSaveTalkComment = async (talkId: number, commentId: number) => {
    if (!editCommentValue.trim()) return;
    try {
      await editComment(talkId, commentId, editCommentValue.trim());
    } catch {
      alert("댓글 수정에 실패했어요.");
    }
    setEditingComment(null);
    setEditCommentValue("");
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="space-y-3 px-[16px] pt-[78px] pb-[16px]"
    >
      <div className="bg-blue-50/50 p-4 rounded-2xl flex items-center justify-between mb-4 border border-blue-100">
        <div>
          <h4 className="font-bold text-blue-600 text-sm">실시간 산책 톡 💬</h4>
          <p className="text-xs text-gray-500 mt-1 leading-relaxed">
            지금 우리 동네 산책 상황을 공유해요!
            <br />
            작성된 글은 24시간 뒤 사라집니다.
          </p>
        </div>
        <Clock size={32} className="text-blue-200" />
      </div>

      <div className="space-y-3">
        {talks.length === 0 && (
          <div className="text-center py-12 text-gray-400 text-sm">
            <p>아직 산책 톡이 없어요!</p>
            <p className="text-xs mt-1">첫 번째로 산책 소식을 공유해보세요 🐾</p>
          </div>
        )}
        {talks.map((talk) => {
          const isExpanded = activeTalkId === talk.id;
          const hasText = (commentValues[talk.id]?.length ?? 0) > 0;
          // 장소 이름 — placeId 있으면 장소명, 없으면 "내 주변"
          const locationLabel = talk.placeId
            ? places.find((p) => p.id === talk.placeId)?.title ?? "내 주변"
            : "내 주변";

          return (
            <div
              key={talk.id}
              className="p-4 rounded-[28px] border border-black/[0.03] bg-blue-50/50 relative transition-all shadow-sm"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-sm text-gray-800">{talk.user}</span>
                  <span className="text-[10px] text-gray-400">{talk.time}</span>
                </div>
                {/* 위치 — 하드코딩 제거 */}
                <div className="flex items-center gap-1 text-[10px] text-gray-500 font-medium bg-white/30 px-2 py-0.5 rounded-full">
                  <MapPin size={10} className="text-gray-400" /> {locationLabel}
                </div>
              </div>

              <p className="text-sm text-gray-700 leading-relaxed mb-3 px-1">{talk.content}</p>

              <div className="mt-2 px-1">
                {!isExpanded ? (
                  <button
                    onClick={() => setActiveTalkId(talk.id)}
                    className="text-[11px] text-gray-500/70 font-medium"
                  >
                    댓글 {talk.comments || 0}개 모두 보기...
                  </button>
                ) : (
                  <div className="space-y-3 animate-in fade-in duration-200">
                    {/* 댓글 리스트 — 수정/삭제 기능 추가 */}
                    <div className="max-h-[150px] overflow-y-auto space-y-2.5 pr-1 scrollbar-hide py-1">
                      {talk.commentList?.map((c) => (
                        <div key={c.id} className="flex gap-2 items-start text-[11px] relative">
                          <span className="font-bold text-gray-900 flex-shrink-0">{c.user || "나"}</span>
                          {editingComment?.commentId === c.id ? (
                            <div className="flex-1 flex items-center gap-1">
                              <input
                                className="flex-1 border-b border-blue-400 outline-none bg-transparent text-[11px] text-gray-800"
                                value={editCommentValue}
                                onChange={(e) => setEditCommentValue(e.target.value)}
                                autoFocus
                                onKeyDown={(e) => {
                                  if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                                    onSaveTalkComment(talk.id, c.id);
                                  }
                                }}
                              />
                              <button
                                onClick={() => onSaveTalkComment(talk.id, c.id)}
                                className="text-[10px] text-blue-500 font-bold"
                              >저장</button>
                              <button
                                onClick={() => { setEditingComment(null); setEditCommentValue(""); }}
                                className="text-[10px] text-gray-400"
                              >취소</button>
                            </div>
                          ) : (
                            <span className="text-gray-800 font-medium leading-snug break-all flex-1">{c.content}</span>
                          )}
                          {/* 본인 댓글만 ··· 버튼 표시 */}
                          {editingComment?.commentId !== c.id && c.isOwner && (
                            <div className="relative flex-shrink-0">
                              <button
                                onClick={() => setCommentMenuId(commentMenuId === c.id ? null : c.id)}
                                className="p-0.5 text-gray-300 hover:text-gray-500"
                              >
                                <MoreHorizontal size={12} />
                              </button>
                              {commentMenuId === c.id && (
                                <>
                                  <div className="fixed inset-0 z-10" onClick={() => setCommentMenuId(null)} />
                                  <div className="absolute right-0 top-5 z-20 bg-white shadow-xl border border-gray-100 rounded-lg overflow-hidden min-w-[70px]">
                                    <button
                                      className="w-full px-3 py-2 text-[11px] text-left hover:bg-gray-50 flex items-center gap-2 text-gray-700"
                                      onClick={() => {
                                        setEditingComment({ postId: talk.id, commentId: c.id, content: c.content });
                                        setEditCommentValue(c.content);
                                        setCommentMenuId(null);
                                      }}
                                    >
                                      <Pencil size={10} /> 수정
                                    </button>
                                    <button
                                      className="w-full px-3 py-2 text-[11px] text-left hover:bg-red-50 text-red-500 flex items-center gap-2"
                                      onClick={() => onDeleteTalkComment(talk.id, c.id)}
                                    >
                                      <Trash2 size={10} /> 삭제
                                    </button>
                                  </div>
                                </>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>

                    <div className="h-[1px] w-full bg-black/[0.05] mt-3" />

                    <div className="space-y-1.5 pt-2">
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          value={commentValues[talk.id] || ""}
                          onChange={(e) =>
                            setCommentValues((prev) => ({ ...prev, [talk.id]: e.target.value }))
                          }
                          placeholder="댓글 달기..."
                          className="flex-1 bg-transparent border-none outline-none text-[14px] font-bold placeholder:text-gray-400/60 text-gray-800 p-0"
                          onKeyDown={(e) => {
                            if (e.key === "Enter" && !e.nativeEvent.isComposing && hasText) {
                              onTalkClick({ ...talk, currentInput: commentValues[talk.id] });
                              setCommentValues((prev) => ({ ...prev, [talk.id]: "" }));
                            }
                          }}
                        />
                        <button
                          disabled={!hasText}
                          onClick={() => {
                            onTalkClick({ ...talk, currentInput: commentValues[talk.id] });
                            setCommentValues((prev) => ({ ...prev, [talk.id]: "" }));
                          }}
                          className="text-[13px] font-extrabold px-1 transition-all duration-200"
                          style={{
                            color: "#e364b0",
                            opacity: hasText ? 1 : 0.3,
                            cursor: hasText ? "pointer" : "default",
                          }}
                        >
                          게시
                        </button>
                      </div>
                    </div>

                    <button
                      onClick={() => setActiveTalkId(null)}
                      className="text-[10px] text-gray-400 font-medium mt-1"
                    >
                      댓글 접기
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="text-center py-8 text-gray-400 text-[10px]">
        <p>최근 24시간 내의 게시글만 표시됩니다.</p>
      </div>
    </motion.div>
  );
}
function LiveTalkWriteModal({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (content: string, location: string) => void;
}) {
  const [content, setContent] = useState("");
  const [placeQuery, setPlaceQuery] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [showPlaceSearch, setShowPlaceSearch] = useState(false);
  const places = useAppStore((s) => s.places);

  const filteredPlaces = placeQuery.trim()
    ? places.filter((p) => p.title.includes(placeQuery) || p.address.includes(placeQuery))
    : places;

  const selectedPlace = places.find((p) => p.id === selectedPlaceId);

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        className="bg-white w-full max-w-sm rounded-[32px] p-8 relative shadow-2xl z-10"
      >
        <button onClick={onClose} className="absolute top-6 right-6 text-gray-300 hover:text-gray-500 transition-colors">
          <X size={24} />
        </button>

        <div className="text-center mb-8">
          <h3 className="text-xl font-bold text-gray-900">실시간 톡 남기기</h3>
          <p className="text-xs text-gray-400 mt-1.5">지금 우리 동네 산책 소식을 들려주세요!</p>
        </div>

        <div className="space-y-6">
          <div className="relative border-b border-black/[0.05] pb-4">
            <textarea
              autoFocus
              placeholder="어떤 이야기를 공유할까요?"
              className="w-full h-32 bg-transparent border-none outline-none text-[16px] font-bold text-gray-800 placeholder:text-gray-300 resize-none p-0"
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
          </div>

          <div className="relative">
            {selectedPlace ? (
              <div
                className="flex items-center justify-between py-2 cursor-pointer"
                onClick={() => { setSelectedPlaceId(null); setShowPlaceSearch(true); }}
              >
                <div className="flex items-center gap-2">
                  <MapPin size={16} className="text-[#e364b0]" />
                  <span className="text-sm text-[#e364b0] font-bold">{selectedPlace.title}</span>
                </div>
                <X size={14} className="text-gray-300" />
              </div>
            ) : (
              <div
                className="flex items-center gap-2 py-2 text-gray-300 cursor-pointer hover:text-gray-400 transition-all group"
                onClick={() => setShowPlaceSearch(!showPlaceSearch)}
              >
                <MapPin size={16} className="group-hover:text-[#e364b0]/50 transition-colors" />
                <span className="text-sm font-bold">장소 태그하기 (선택)</span>
              </div>
            )}

            <AnimatePresence>
              {showPlaceSearch && !selectedPlaceId && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="absolute z-50 w-full mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden"
                >
                  <div className="p-3 border-b border-gray-50 flex items-center gap-2 bg-gray-50">
                    <Search size={14} className="text-gray-400" />
                    <input
                      type="text"
                      placeholder="장소 검색..."
                      className="bg-transparent flex-1 text-sm outline-none"
                      value={placeQuery}
                      onChange={(e) => setPlaceQuery(e.target.value)}
                    />
                  </div>
                  <div className="max-h-[160px] overflow-y-auto">
                    {filteredPlaces.length > 0 ? (
                      filteredPlaces.map((place) => (
                        <button
                          key={place.id}
                          className="w-full text-left p-4 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0"
                          onClick={() => { setSelectedPlaceId(place.id); setShowPlaceSearch(false); setPlaceQuery(""); }}
                        >
                          <div className="text-sm font-bold text-gray-800">{place.title}</div>
                          <div className="text-[11px] text-gray-400">{place.address}</div>
                        </button>
                      ))
                    ) : (
                      <div className="p-4 text-center text-xs text-gray-400">결과 없음</div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            disabled={!content.trim()}
            onClick={() => {
              const finalLocation = selectedPlace ? selectedPlace.title : "내 주변";
              onSubmit(content.trim(), finalLocation);
              setContent("");
              setSelectedPlaceId(null);
              setPlaceQuery("");
              onClose();
            }}
            className="w-full py-4 rounded-2xl font-extrabold text-[16px] transition-all active:scale-95 disabled:opacity-30 shadow-lg shadow-[#e364b0]/20 text-white"
            style={{ backgroundColor: "#e364b0" }}
          >
            지금 등록하기
          </button>
        </div>
      </motion.div>
    </div>
  );
}