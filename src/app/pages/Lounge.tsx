import React, { useState } from "react";
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
  Users,
  Pencil,
  Trash2,
  Search,
} from "lucide-react";
// 더미 데이터 제거
import {
  useFeedStore,
  type FeedPost,
} from "../store/useFeedStore";
import { useAppStore } from "../store/useAppStore";
import { ShareSheet } from "../components/ShareSheet";

interface LoungeProps {
  onNavigate: (page: string, params?: any) => void;
}

// Mock Data for Walk Talk
// 실시간 산책 톡 목업 데이터
const WALK_TALKS = [
  {
    id: 1,
    user: "보리보리",
    content:
      "지금 올림픽공원 북2문 쪽 강아지 친구들 많아요! 놀러오세요~ 🐕",
    time: "10분 전",
    location: "올림픽공원",
    color: "bg-orange-50 border-orange-100",
  },
  {
    id: 2,
    user: "망고주스",
    content: "한강 잠원지구 주차장 만차입니다 ㅠㅠ 참고하세요!",
    time: "25분 전",
    location: "잠원한강공원",
    color: "bg-red-50 border-red-100",
  },
  {
    id: 3,
    user: "코코넨네",
    content:
      "비 그쳐서 산책 나왔는데 땅이 좀 질척거리네요. 발 닦일 준비 필수!",
    time: "40분 전",
    location: "여의��공원",
    color: "bg-blue-50 border-blue-100",
  },
  {
    id: 4,
    user: "해피해피",
    content:
      "반려동물 놀이터 소형견 구역 지금 한가해요~ 소심한 친구들 오기 딱좋음",
    time: "1시간 전",
    location: "보라매공원",
    color: "bg-green-50 border-green-100",
  },
];

// Sample images for demo post creation
// 데모 게시글 작성용 샘플 이미지
const SAMPLE_IMAGES = [
  "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&q=80",
  "https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&q=80",
  "https://images.unsplash.com/photo-1560807707-8cc77767d783?w=800&q=80",
  "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&q=80",
];

export function Lounge({ onNavigate }: LoungeProps) {
  const [activeTab, setActiveTab] = useState<"feed" | "talk">(
    "feed",
  );
  const [isWriteModalOpen, setIsWriteModalOpen] =
    useState(false);
  const { posts, addPost } = useFeedStore();
  const places = useAppStore((s) => s.places);

  const handleCreatePost = (
    content: string,
    imgIndex: number,
    placeId?: number,
  ) => {
    addPost({
      user: "나",
      userImg:
        "https://images.unsplash.com/photo-1535930749574-1399327ce78f?w=100&q=80",
      img: SAMPLE_IMAGES[imgIndex % SAMPLE_IMAGES.length],
      content,
      time: "방금 전",
      placeId,
    });
    setIsWriteModalOpen(false);
  };

  return (
    <div className="bg-white -mt-2.5">
      {/* 탭 전환 */}
      <div className="sticky top-0 z-40 bg-white border-b border-gray-100 flex">
        <button
          onClick={() => setActiveTab("feed")}
          className={`flex-1 py-3 text-sm font-bold transition-colors relative ${
            activeTab === "feed"
              ? "text-gray-900"
              : "text-gray-400"
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
          className={`flex-1 py-3 text-sm font-bold transition-colors relative ${
            activeTab === "talk"
              ? "text-gray-900"
              : "text-gray-400"
          }`}
        >
          <MessageCircle
            className="inline-block mr-1"
            size={16}
          />{" "}
          실시간 톡
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
              posts={posts.filter((p) => !p.isHidden)}
              onNavigate={onNavigate}
            />
          ) : (
            <WalkTalkView key="talk" talks={WALK_TALKS} />
          )}
        </AnimatePresence>
      </div>

      {/* 플로팅 버튼 그룹 — 컨테이너 우측 외부 고정 (AI 챗봇 버튼 위) */}
      <div className="fixed bottom-[162px] inset-x-0 flex justify-center pointer-events-none z-50">
        <div className="relative w-full max-w-[600px]">
          <div className="absolute bottom-0 left-full flex flex-col gap-2.5 pl-3">
            <button
              onClick={() => setIsWriteModalOpen(true)}
              className="w-14 h-14 bg-primary text-white rounded-full shadow-[0_4px_20px_rgba(227,99,148,0.4)] flex items-center justify-center hover:scale-105 active:scale-95 transition-transform pointer-events-auto"
              title="글쓰기"
            >
              <Camera size={22} />
            </button>
          </div>
        </div>
      </div>

      {/* 글쓰기 모달 */}
      <AnimatePresence>
        {isWriteModalOpen && (
          <WriteModal
            onClose={() => setIsWriteModalOpen(false)}
            onSubmit={handleCreatePost}
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
  onSubmit: (
    content: string,
    imgIndex: number,
    placeId?: number,
  ) => void;
}) {
  const [content, setContent] = useState("");
  const [selectedImg, setSelectedImg] = useState(0);
  const [showPlaceSearch, setShowPlaceSearch] = useState(false);
  const [placeQuery, setPlaceQuery] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState<
    number | undefined
  >(undefined);
  const places = useAppStore((s) => s.places);

  const filteredPlaces = placeQuery.trim()
    ? places.filter(
        (p) =>
          p.title.includes(placeQuery) ||
          p.address.includes(placeQuery),
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
          {/* 이미지 선택 */}
          <div className="grid grid-cols-4 gap-2">
            {SAMPLE_IMAGES.map((img, i) => (
              <div
                key={i}
                className={`aspect-square rounded-xl overflow-hidden cursor-pointer border-2 transition-all ${
                  selectedImg === i
                    ? "border-primary scale-95"
                    : "border-transparent"
                }`}
                onClick={() => setSelectedImg(i)}
              >
                <img
                  src={img}
                  alt={`Sample ${i + 1}`}
                  className="w-full h-full object-cover"
                />
              </div>
            ))}
          </div>

          <textarea
            placeholder="오늘의 산책 이야기를 들려주세요!"
            className="w-full h-24 bg-gray-50 rounded-xl p-3 text-sm outline-none resize-none placeholder:text-gray-400"
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />

          {/* 장소 태그 선택 */}
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
                  <span className="text-sm text-primary font-bold">
                    {selectedPlace.title}
                  </span>
                  <span className="text-[11px] text-gray-400">
                    {selectedPlace.address}
                  </span>
                </div>
                <X size={14} className="text-gray-400" />
              </div>
            ) : (
              <div
                className="flex items-center gap-2 p-3 bg-gray-50 rounded-xl text-gray-500 cursor-pointer hover:bg-gray-100"
                onClick={() =>
                  setShowPlaceSearch(!showPlaceSearch)
                }
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
                    <Search
                      size={14}
                      className="text-gray-400"
                    />
                    <input
                      type="text"
                      placeholder="장소명 또는 지역으로 검색"
                      className="flex-1 text-sm bg-transparent outline-none placeholder:text-gray-400"
                      value={placeQuery}
                      onChange={(e) =>
                        setPlaceQuery(e.target.value)
                      }
                      autoFocus
                    />
                    {placeQuery && (
                      <button onClick={() => setPlaceQuery("")}>
                        <X
                          size={14}
                          className="text-gray-400"
                        />
                      </button>
                    )}
                  </div>
                  <div className="max-h-[140px] overflow-y-auto space-y-1">
                    {filteredPlaces.length > 0 ? (
                      filteredPlaces.map((place) => (
                        <button
                          key={place.id}
                          className="flex items-center gap-2 w-full p-2 rounded-lg hover:bg-gray-50 transition-colors text-left"
                          onClick={() => {
                            setSelectedPlaceId(place.id);
                            setShowPlaceSearch(false);
                            setPlaceQuery("");
                          }}
                        >
                          <div className="w-8 h-8 rounded-lg overflow-hidden bg-gray-200 flex-shrink-0">
                            <img
                              src={place.imageUrl || ""}
                              alt={place.title}
                              className="w-full h-full object-cover"
                            />
                          </div>
                          <div className="min-w-0">
                            <div className="text-sm font-bold text-gray-800 truncate">
                              {place.title}
                            </div>
                            <div className="text-[11px] text-gray-400">
                              {place.address}
                            </div>
                          </div>
                        </button>
                      ))
                    ) : (
                      <div className="text-center py-3 text-xs text-gray-400">
                        검색 결과가 없습니다.
                      </div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            className="w-full bg-primary text-white font-bold py-3 rounded-xl hover:bg-primary/90 active:scale-95 transition-all disabled:opacity-50"
            disabled={!content.trim()}
            onClick={() =>
              onSubmit(content, selectedImg, selectedPlaceId)
            }
          >
            등록하기
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
  const { toggleLike, addComment, deletePost, editPost } =
    useFeedStore();
  const places = useAppStore((s) => s.places);
  const [commentingPostId, setCommentingPostId] = useState<
    number | null
  >(null);
  const [commentText, setCommentText] = useState("");
  const [sharePostData, setSharePostData] = useState<{
    id: number;
    img: string;
    user: string;
  } | null>(null);
  const [menuOpenPostId, setMenuOpenPostId] = useState<
    number | null
  >(null);
  const [editingPost, setEditingPost] = useState<{
    id: number;
    content: string;
  } | null>(null);

  const handleAddComment = (postId: number) => {
    if (!commentText.trim()) return;
    addComment(postId, "나", commentText.trim());
    setCommentText("");
    setCommentingPostId(null);
  };

  const feedContent = (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="pb-4"
    >
      {posts.map((post) => (
        <article
          key={post.id}
          className="bg-white mb-4 border-b border-gray-100 last:border-0"
        >
          {/* 게시글 헤더 */}
          <div className="flex items-center justify-between p-3 relative">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-200">
                <img
                  src={post.userImg}
                  alt={post.user}
                  className="w-full h-full object-cover"
                />
              </div>
              <span className="text-sm font-bold text-gray-800">
                {post.user}
              </span>
            </div>
            <button
              className="text-gray-400 hover:text-gray-600 relative"
              onClick={() => {
                if (post.user === "나") {
                  setMenuOpenPostId(
                    menuOpenPostId === post.id ? null : post.id,
                  );
                }
              }}
            >
              <MoreHorizontal size={20} />
            </button>
            {/* 게시글 메뉴 드롭다운 */}
            <AnimatePresence>
              {menuOpenPostId === post.id &&
                post.user === "나" && (
                  <>
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => setMenuOpenPostId(null)}
                    />
                    <motion.div
                      initial={{
                        opacity: 0,
                        scale: 0.9,
                        y: -4,
                      }}
                      animate={{ opacity: 1, scale: 1, y: 0 }}
                      exit={{ opacity: 0, scale: 0.9, y: -4 }}
                      transition={{ duration: 0.15 }}
                      className="absolute right-3 top-12 z-20 bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden min-w-[120px]"
                    >
                      <button
                        className="flex items-center gap-2 w-full px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                        onClick={() => {
                          setEditingPost({
                            id: post.id,
                            content: post.content,
                          });
                          setMenuOpenPostId(null);
                        }}
                      >
                        <Pencil
                          size={15}
                          className="text-gray-500"
                        />
                        수정
                      </button>
                      <div className="h-px bg-gray-100" />
                      <button
                        className="flex items-center gap-2 w-full px-4 py-3 text-sm text-red-500 hover:bg-red-50 transition-colors"
                        onClick={() => {
                          if (
                            confirm(
                              "정말 이 게시물을 삭제할까요?",
                            )
                          ) {
                            deletePost(post.id);
                          }
                          setMenuOpenPostId(null);
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
            <img
              src={post.img}
              alt="Feed"
              className="w-full h-full object-cover"
            />
            {/* 더블탭 좋아요 애니메이션 영역 */}
          </div>

          {/* 액션 버튼 */}
          <div className="p-3">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-4">
                <button
                  className="hover:scale-110 active:scale-95 transition-transform"
                  onClick={() => toggleLike(post.id)}
                >
                  <Heart
                    size={24}
                    className={
                      post.isLiked
                        ? "fill-primary text-primary transition-colors"
                        : "text-gray-800 hover:text-primary transition-colors"
                    }
                  />
                </button>
                <button
                  className="text-gray-800 hover:text-gray-600"
                  onClick={() =>
                    setCommentingPostId(
                      commentingPostId === post.id
                        ? null
                        : post.id,
                    )
                  }
                >
                  <MessageCircle size={24} />
                </button>
                <button
                  className="text-gray-800 hover:text-gray-600"
                  onClick={() =>
                    setSharePostData({
                      id: post.id,
                      img: post.img,
                      user: post.user,
                    })
                  }
                >
                  <Send size={24} />
                </button>
              </div>
            </div>

            <div className="font-bold text-sm mb-1">
              좋아요 {post.likes}개
            </div>

            <div className="text-sm text-gray-800 mb-2">
              <span className="font-bold mr-2">
                {post.user}
              </span>
              {post.content}
            </div>

            {/* 댓글 미리보기 */}
            {post.comments > 0 && (
              <button
                className="text-xs text-gray-400 mb-1"
                onClick={() =>
                  setCommentingPostId(
                    commentingPostId === post.id
                      ? null
                      : post.id,
                  )
                }
              >
                댓글 {post.comments}개 모두 보기
              </button>
            )}

            {/* 댓글 입력 */}
            <AnimatePresence>
              {commentingPostId === post.id && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden"
                >
                  {/* 최근 댓글 표시 */}
                  {post.commentList.slice(-3).map((c) => (
                    <div key={c.id} className="text-sm mb-1">
                      <span className="font-bold mr-1">
                        {c.user}
                      </span>
                      <span className="text-gray-700">
                        {c.content}
                      </span>
                    </div>
                  ))}
                  <div className="flex items-center gap-2 mt-2 border-t border-gray-100 pt-2">
                    <input
                      type="text"
                      placeholder="댓글 달기..."
                      className="flex-1 text-sm outline-none bg-transparent placeholder:text-gray-400"
                      value={commentText}
                      onChange={(e) =>
                        setCommentText(e.target.value)
                      }
                      onKeyDown={(e) =>
                        e.key === "Enter" &&
                        handleAddComment(post.id)
                      }
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

            {/* 장소 태그 */}
            {post.placeId && (
              <div
                className="inline-flex items-center gap-1 bg-primary/5 text-primary px-2 py-1 rounded text-xs font-bold cursor-pointer hover:bg-primary/10 transition-colors mb-2 mt-1"
                onClick={() => {
                  const place = places.find(
                    (p) => p.id === post.placeId,
                  );
                  if (place)
                    onNavigate("detail", { id: place.id });
                }}
              >
                <MapPin size={12} />
                {places.find((p) => p.id === post.placeId)
                  ?.title || "알 수 없는 장소"}
              </div>
            )}

            <div className="text-[10px] text-gray-400 uppercase tracking-wide">
              {post.time}
            </div>
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
      {/* 게시글 수정 모달 */}
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
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
        >
          <X size={24} />
        </button>

        <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
          <Pencil className="text-primary" size={20} /> 게시글
          수정
        </h3>

        <textarea
          placeholder="내용을 입력하세요"
          className="w-full h-32 bg-gray-50 rounded-xl p-3 text-sm outline-none resize-none placeholder:text-gray-400"
          value={text}
          onChange={(e) => setText(e.target.value)}
          autoFocus
        />

        <button
          className="w-full mt-4 bg-primary text-white font-bold py-3 rounded-xl hover:bg-primary/90 active:scale-95 transition-all disabled:opacity-50"
          disabled={!text.trim()}
          onClick={() => onSave(text.trim())}
        >
          수정 완료
        </button>
      </motion.div>
    </div>
  );
}

function WalkTalkView({ talks }: { talks: typeof WALK_TALKS }) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="space-y-3 px-[16px] pt-[78px] pb-[16px]"
    >
      <div className="bg-primary/10 p-4 rounded-2xl flex items-center justify-between mb-4">
        <div>
          <h4 className="font-bold text-primary text-sm">
            실시간 산책 톡 💬
          </h4>
          <p className="text-xs text-gray-500">
            지금 우리 동네 산책 상황을 공유해요!
            <br />
            작성된 글은 24시간 뒤 사라집니다.
          </p>
        </div>
        <Clock size={32} className="text-primary/50" />
      </div>

      {talks.map((talk) => (
        <div
          key={talk.id}
          className={`p-4 rounded-2xl border ${talk.color} relative`}
        >
          <div className="flex justify-between items-start mb-2">
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-gray-800">
                {talk.user}
              </span>
              <span className="text-[10px] text-gray-500 bg-white/50 px-1.5 py-0.5 rounded">
                {talk.time}
              </span>
            </div>
            <div className="flex items-center gap-1 text-[10px] text-gray-500 font-medium">
              <MapPin size={10} /> {talk.location}
            </div>
          </div>
          <p className="text-sm text-gray-700 leading-relaxed">
            {talk.content}
          </p>
        </div>
      ))}

      <div className="text-center py-8 text-gray-400 text-xs">
        <p>최근 24시간 내의 게시글만 표시됩니다.</p>
      </div>
    </motion.div>
  );
}