import React, { useState, useRef } from "react";
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
// 더미 데이터 제거
import { useFeedStore, type FeedPost } from "../store/useFeedStore";
import { useAppStore } from "../store/useAppStore";
import { ShareSheet } from "../components/ShareSheet";

interface LoungeProps {
  onNavigate: (page: string, params?: any) => void;
}

// 실시간 산책 톡 목업 데이터
const WALK_TALKS = [
  {
    id: 1,
    user: "보리보리",
    content: "지금 올림픽공원 북2문 쪽 강아지 친구들 많아요! 놀러오세요~ 🐕",
    time: "10분 전",
    location: "올림픽공원",
    color: "bg-orange-50 border-orange-100",
    comments: [
      { user: "초코맘", Content: "와! 저도 지금 가고 있어요!" },
      { user: "망고주스", Content: "보리 너무 귀엽겠네요 ㅎㅎ" },
    ],
  },
  {
    id: 2,
    user: "망고주스",
    content: "한강 잠원지구 주차장 만차입니다 ㅠㅠ 참고하세요!",
    time: "25분 전",
    location: "잠원한강공원",
    color: "bg-red-50 border-red-100",
    comments: [{ user: "해피", Content: "만차ㅜㅜ 힘들겠어요!ㅜ" }],
  },
  {
    id: 3,
    user: "코코넨네",
    content: "비 그쳐서 산책 나왔는데 땅이 좀 질척거리네요. 발 닦일 준비 필수!",
    time: "40분 전",
    location: "여의도한강공원",
    color: "bg-blue-50 border-blue-100",
    comments: [],
  },
  {
    id: 4,
    user: "해피해피",
    content:
      "반려동물 놀이터 소형견 구역 지금 한가해요~ 소심한 친구들 오기 딱좋음",
    time: "1시간 전",
    location: "보라매공원",
    color: "bg-green-50 border-green-100",
    comments: [],
  },
];

// Sample images for demo post creation
// 데모 게시글 작성용 샘플 이미지
// 라운지.tsx 상단 SAMPLE_IMAGES 부분 수정
const SAMPLE_IMAGES = [
  "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&q=80",
  "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=800&q=80",
  "https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=800&q=80",
  "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=800&q=80",
];

export function Lounge({ onNavigate }: LoungeProps) {
  const [activeTab, setActiveTab] = useState<"feed" | "talk">("feed");
  const [isWriteModalOpen, setIsWriteModalOpen] = useState(false);

  const [isMenuOpen, setIsMenuOpen] = useState(false); // 에러 해결 위한 상태값 추가

  // 실시간 톡 선언
  const [talks, setTalks] = useState<any[]>(WALK_TALKS);
  const [isLiveTalkModalOpen, setIsLiveTalkModalOpen] = useState(false);

  // 실시간 톡 댓글 관리
  const [talkComments, setTalkComments] = useState<Record<number, any[]>>({});
  const [selectedTalk, setSelectedTalk] = useState<any | null>(null);
  const [isCommentSheetOpen, setIsCommentSheetOpen] = useState(false);

  const { posts, addPost } = useFeedStore();
  const places = useAppStore((s) => s.places);

  const handleCreatePost = (
    content: string,
    imgSource: any, // imgIndex 대신 imgSource로 받기
    placeId?: number,
  ) => {
    // 만약 넘겨받은 값이 숫자(인덱스)면 더미를 쓰고,
    // 문자열(Base64)이면 내가 올린 사진을 그대로 씁니다.
    const finalImg =
      typeof imgSource === "number"
        ? SAMPLE_IMAGES[imgSource % SAMPLE_IMAGES.length]
        : imgSource; // 0빼고 전체 사용

    addPost({
      user: "나",
      userImg:
        "https://images.unsplash.com/photo-1535930749574-1399327ce78f?w=100&q=80",
      img: finalImg, // 👈 여기서 진짜 이미지가 결정됩니다!
      content,
      time: "방금 전",
      placeId,
    });
    setIsWriteModalOpen(false);
  };

  // 2. 실시간 톡 등록 함수
  const handleCreateTalk = (content: string, location: string) => {
    // 기존 데이터들처럼 랜덤한 색상 테마 부여 (선택 사항)
    const colors = [
      "bg-orange-50 border-orange-100",
      "bg-red-50 border-red-100",
      "bg-blue-50 border-blue-100",
      "bg-green-50 border-green-100",
      "bg-purple-50 border-purple-100",
    ];
    const randomColor = colors[Math.floor(Math.random() * colors.length)];

    const newTalk = {
      id: Date.now(), // 고유 ID
      user: "나", // 작성자
      content: content,
      time: "방금 전",
      location: location || "내 주변",
      color: randomColor, // 기존 WALK_TALKS 스타일 유지
      comments: [],
    };

    // 🔴 중요: 대문자 WALK_TALKS는 수정이 안 되므로
    // 상단에 [talks, setTalks] = useState(WALK_TALKS)가 선언되어 있어야 합니다.
    setTalks([newTalk, ...talks]);
    setIsLiveTalkModalOpen(false);
  };

  // talkComments 상태를 업데이트하는 함수 추가
  const handleCreateTalkComment = (talkId: number, content: string) => {
    if (!content.trim()) return;

    setTalkComments((prev) => ({
      ...prev,
      [talkId]: [
        ...(prev[talkId] || []),
        { content: content, user: "나", id: Date.now() }, // 새 댓글 객체 추가
      ],
    }));
  };

  return (
    <div className="px-[0px] pt-[0px] pb-[96px]">
      {/* 탭 전환 */}
      <div className="sticky top-[0px] z-40 bg-white/95 backdrop-blur-sm border-b border-gray-100 flex">
        <button
          onClick={() => setActiveTab("feed")}
          className={`flex-1 py-3 text-sm font-bold transition-colors relative ${
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
          className={`flex-1 py-3 text-sm font-bold transition-colors relative ${
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
              posts={posts.filter((p) => !p.isHidden)}
              onNavigate={onNavigate}
            />
          ) : (
            <WalkTalkView
              key="talk"
              // 1. [수정] 기존 더미(t.comments)와 실시간 상태(talkComments[t.id])를 합쳐서 전달
              talks={talks.map((t) => ({
                ...t,
                comments: [
                  ...(t.comments || []), // 기존 더미 데이터 유지
                  ...(talkComments[t.id] || []), // 새로 작성한 댓글 추가
                ],
              }))}
              onTalkClick={(talkData) => {
                // 2. 게시 버튼을 눌러서 'currentInput'이 넘어온 경우에만 저장 실행
                if (talkData.currentInput) {
                  setTalkComments((prev) => ({
                    ...prev,
                    [talkData.id]: [
                      ...(prev[talkData.id] || []),
                      {
                        id: Date.now(),
                        user: "나",
                        content: talkData.currentInput,
                      },
                    ],
                  }));
                } else {
                  // 입력값 없이 클릭한 경우는 일반적인 상세 보기(바텀시트) 열기
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
              onClick={() => setIsMenuOpen(true)} // 여기서 팝업 메뉴를 띄웁니다
              className="w-14 h-14 bg-primary text-white rounded-full shadow-[0_4px_20px_rgba(227,99,148,0.4)] flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
            >
              <Camera size={22} />
            </button>
          </div>
        </div>
      </div>

      {/* 1. 메뉴 선택 팝업 (분홍색 메인 버튼 누르면 뜸) */}
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
                {/* ✅ [게시물 작성] 버튼 클릭 시 동작 */}
                <button
                  onClick={() => {
                    setActiveTab("feed"); // 피드 탭으로 변경
                    setIsWriteModalOpen(true); // ⭐ 아까 그 게시물 작성 모달 열기!
                    setIsMenuOpen(false); // 현재 팝업 메뉴는 닫기
                  }}
                  className="flex items-center gap-4 p-4 bg-pink-50 rounded-2xl border border-pink-100 hover:bg-pink-100 transition-colors"
                >
                  <div className="w-12 h-12 bg-primary rounded-full flex items-center justify-center text-white shadow-lg">
                    <Camera size={24} />
                  </div>
                  <div className="text-left">
                    <p className="font-bold text-gray-900">게시물 작성</p>
                    <p className="text-xs text-pink-400">
                      오늘의 산책을 공유해요
                    </p>
                  </div>
                </button>

                {/* ✅ [실시간 톡] 버튼 클릭 시 동작 */}
                <button
                  onClick={() => {
                    setActiveTab("talk"); // 실시간 톡 탭으로 변경
                    setIsMenuOpen(false); // 현재 팝업 메뉴는 닫기
                    setIsLiveTalkModalOpen(true); // 새로 만든 실시간 톡 전용 모달 열기
                  }}
                  className="flex items-center gap-4 p-4 bg-blue-50 rounded-2xl border border-blue-100 hover:bg-blue-100 transition-colors"
                >
                  <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center text-white shadow-lg">
                    <MessageCircle size={24} />
                  </div>
                  <div className="text-left">
                    <p className="font-bold text-gray-900">실시간 톡</p>
                    <p className="text-xs text-blue-400">
                      이웃과 바로 대화해요
                    </p>
                  </div>
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 2. 진짜 게시물 작성 페이지 (WriteModal) */}
      <AnimatePresence>
        {isWriteModalOpen && (
          <WriteModal
            onClose={() => setIsWriteModalOpen(false)}
            onSubmit={handleCreatePost}
          />
        )}
      </AnimatePresence>

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
  onSubmit: (content: string, imgIndex: number, placeId?: number) => void;
}) {
  const [content, setContent] = useState("");

  const [myImages, setMyImages] = useState<string[]>([]); // 업로드된 이미지 URL 배열
  const [selectedImg, setSelectedImg] = useState<number | null>(null); // 선택된 인덱스 (초기값 null)
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 1. 이벤트 타입 구체화
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 파일 타입 체크 (이미지만 허용)
    if (!file.type.startsWith("image/")) {
      alert("이미지 파일만 업로드 가능합니다.");
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      const result = reader.result as string;

      // 사진을 교체하는 방식일 때
      setMyImages([result]);
      setSelectedImg(0);

      // 사진을 '추가'하는 방식이라면:
      // setMyImages(prev => [...prev, result]);
    };
    reader.readAsDataURL(file);
  };

  const [showPlaceSearch, setShowPlaceSearch] = useState(false);
  const [placeQuery, setPlaceQuery] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | undefined>(
    undefined,
  );
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
          {/* 이미지 선택 */}
          <div className="grid grid-cols-4 gap-2">
            {/* 1. 파일 업로드 버튼 */}
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

            {/* 2. 업로드된 내 이미지들 보여주기 */}
            {myImages.map((img, i) => (
              <div
                key={i}
                onClick={() => setSelectedImg(i)} // 인덱스로 선택 (0, 1, 2...)
                className={`relative aspect-square rounded-xl overflow-hidden cursor-pointer border-2 transition-all ${
                  selectedImg === i
                    ? "border-primary scale-95"
                    : "border-transparent"
                }`}
              >
                <img
                  src={img}
                  alt="upload"
                  className="w-full h-full object-cover"
                />
                {selectedImg === i && (
                  <div className="absolute inset-0 bg-primary/10 flex items-center justify-center"></div>
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
                          className="flex items-center gap-2 w-full p-2 rounded-lg hover:bg-gray-50 transition-colors text-left"
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
                                // 만약 URL은 있는데 서버에서 이미지를 못 찾을 경우(404)를 대비
                                onError={(e) => {
                                  (e.target as HTMLImageElement).style.display =
                                    "none";
                                }}
                              />
                            ) : (
                              // 이미지가 없을 때 보여줄 기본 아이콘 (lucide-react의 MapPin 등)
                              <MapPin size={14} className="text-gray-400" />
                            )}
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
            // 필수 조건: 내용이 있고 + 이미지가 선택되었을 때만 활성화
            disabled={!content.trim() || selectedImg === null}
            onClick={() => {
              // 1. 선택된 이미지가 있는지 최종 확인
              if (selectedImg !== null && myImages[selectedImg]) {
                // 2. 부모에게 받은 onSubmit 실행
                // content: 글 내용
                // myImages[selectedImg]: 선택된 이미지의 base64 데이터
                // selectedPlaceId: 선택된 장소의 ID (있을 경우만)
                onSubmit(
                  content,
                  myImages[selectedImg] as any,
                  selectedPlaceId,
                );

                console.log("게시글 전송 데이터:", {
                  내용: content,
                  이미지: "base64 데이터 생략",
                  장소ID: selectedPlaceId,
                });
              }
            }}
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
  // 이미 선언되어 있는 스토어 도구들을 그대로 사용
  const { toggleLike, addComment, deletePost, editPost } = useFeedStore();

  // 선언 유지
  const places = useAppStore((s) => s.places);
  const [commentingPostId, setCommentingPostId] = useState<number | null>(null);
  const [commentText, setCommentText] = useState("");
  const [editValue, setEditValue] = useState("");

  const [sharePostData, setSharePostData] = useState<{
    id: number;
    img: string;
    user: string;
  } | null>(null);
  const [menuOpenPostId, setMenuOpenPostId] = useState<number | null>(null);
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

  // 1. 댓글 삭제 로직 (기존 menuOpenPostId 사용)
  const onDeleteComment = (postId: number, commentId: number) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      const targetPost = posts.find((p) => p.id === postId);
      if (targetPost) {
        const newCommentList = targetPost.commentList.filter(
          (c) => c.id !== commentId,
        );

        // 인자 3개 전달 (ID, 데이터, 빈 객체)
        editPost(postId, {
          commentList: newCommentList,
          comments: Math.max(0, targetPost.comments - 1),
        } as any);
      }
    }
    setMenuOpenPostId(null); // 기존 선언된 이름으로 변경
  };

  // 댓글 수정
  const onUpdateComment = (postId: number, commentId: number) => {
  const targetPost = posts.find((p) => p.id === postId);
  if (targetPost) {
    // 1. 기존 댓글 리스트에서 해당 ID만 수정된 내용으로 교체
    const newCommentList = targetPost.commentList.map((c) =>
      c.id === commentId ? { ...c, content: editValue } : c
    );

    // 2. 스토어의 editPost를 호출하여 상태 반영
    editPost(postId, {
      commentList: newCommentList,
    } as any);
  }
  // 3. 수정 모드 종료 및 입력값 초기화
  setEditingPost(null);
  setEditValue("");
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
              {menuOpenPostId === post.id && post.user === "나" && (
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
                      <Pencil size={15} className="text-gray-500" />
                      수정
                    </button>
                    <div className="h-px bg-gray-100" />
                    <button
                      className="flex items-center gap-2 w-full px-4 py-3 text-sm text-red-500 hover:bg-red-50 transition-colors"
                      onClick={() => {
                        if (confirm("정말 이 게시물을 삭제할까요?")) {
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
                      commentingPostId === post.id ? null : post.id,
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

            <div className="font-bold text-sm mb-1">좋아요 {post.likes}개</div>

            <div className="text-sm text-gray-800 mb-2">
              <span className="font-bold mr-2">{post.user}</span>
              {post.content}
            </div>

            {/* 댓글 미리보기 */}
            {post.comments > 0 && (
              <button
                className="text-xs text-gray-400 mb-1"
                onClick={() =>
                  setCommentingPostId(
                    commentingPostId === post.id ? null : post.id,
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
                    <div
                      key={c.id}
                      className="flex items-start justify-between mb-1 relative text-sm min-h-[24px]"
                    >
                      {/* [왼쪽] 이름 + 내용 */}
                      <div className="flex-1 pr-2">
                        <span className="font-bold mr-1">{c.user}</span>

                        {editingPost?.id === c.id ? (
                          <div className="inline-flex items-center gap-2">
                            <input
                              className="border-b border-primary outline-none bg-transparent py-0 h-auto text-sm"
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              autoFocus
                              onKeyDown={(e) =>
                                e.key === "Enter" &&
                                onUpdateComment(post.id, c.id)
                              }
                            />
                            <button
                              onClick={() => onUpdateComment(post.id, c.id)}
                              className="text-[10px] text-primary font-bold"
                            >
                              저장
                            </button>
                            <button
                              onClick={() => {
                                setEditingPost(null);
                                setEditValue("");
                              }}
                              className="text-[10px] text-gray-400"
                            >
                              취소
                            </button>
                          </div>
                        ) : (
                          <span className="text-gray-700">{c.content}</span>
                        )}
                      </div>

                      {/* [오른쪽] 수정/삭제 메뉴 버튼 (항상 노출) */}
                      {c.user === "나" && editingPost?.id !== c.id && (
                        <div className="relative flex-shrink-0">
                          <button
                            onClick={() =>
                              setMenuOpenPostId(
                                menuOpenPostId === c.id ? null : c.id,
                              )
                            }
                            className="p-1 text-gray-300 hover:text-gray-600 transition-colors"
                          >
                            <MoreHorizontal size={14} />
                          </button>

                          {menuOpenPostId === c.id && (
                            <>
                              <div
                                className="fixed inset-0 z-10"
                                onClick={() => setMenuOpenPostId(null)}
                              />
                              <div className="absolute right-0 top-6 z-20 bg-white shadow-xl border border-gray-100 rounded-lg overflow-hidden min-w-[70px]">
                                <button
                                  className="w-full px-3 py-2 text-[11px] text-left hover:bg-gray-50 flex items-center gap-2 text-gray-700"
                                  onClick={() => {
                                    setEditingPost({
                                      id: c.id,
                                      content: c.content,
                                    });
                                    setEditValue(c.content);
                                    setMenuOpenPostId(null);
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

                  {/* 댓글 입력창 */}
                  <div className="flex items-center gap-2 mt-2 border-t border-gray-100 pt-2">
                    <input
                      type="text"
                      placeholder="댓글 달기..."
                      className="flex-1 text-sm outline-none bg-transparent placeholder:text-gray-400"
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      onKeyDown={(e) =>
                        e.key === "Enter" && handleAddComment(post.id)
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
                  const place = places.find((p) => p.id === post.placeId);
                  if (place) onNavigate("detail", { id: place.id });
                }}
              >
                <MapPin size={12} />
                {places.find((p) => p.id === post.placeId)?.title ||
                  "알 수 없는 장소"}
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

// props 타입을 정의할 때 기존 WALK_TALKS 구조에 comments와 onTalkClick을 추가합니다.
function WalkTalkView({
  talks,
  onTalkClick,
}: {
  talks: any[]; // 댓글 데이터가 합쳐진 배열을 받으므로 any[]로 유연하게 처리합니다.
  onTalkClick: (talk: any) => void;
}) {
  const [activeTalkId, setActiveTalkId] = React.useState<number | null>(null);
  const [commentValues, setCommentValues] = useState<{ [key: number]: string }>(
    {},
  );

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
        {talks.map((talk) => {
          const isExpanded = activeTalkId === talk.id;
          // 글자가 있는지 확인 (부모의 상태값과 연결 확인 필요)
          const hasText = commentValues[talk.id]?.length > 0;

          return (
            <div
              key={talk.id}
              // 카드 전체 배경 랜덤 색상 (talk.color)
              className={`p-4 rounded-[28px] border border-black/[0.03] ${talk.color} relative transition-all shadow-sm`}
            >
              {/* 상단 정보 (기존 유지) */}
              <div className="flex justify-between items-start mb-2">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-sm text-gray-800">
                    {talk.user}
                  </span>
                  <span className="text-[10px] text-gray-400">{talk.time}</span>
                </div>
                <div className="flex items-center gap-1 text-[10px] text-gray-500 font-medium bg-white/30 px-2 py-0.5 rounded-full">
                  <MapPin size={10} className="text-gray-400" /> {talk.location}
                </div>
              </div>

              {/* 본문 내용 */}
              <p className="text-sm text-gray-700 leading-relaxed mb-3 px-1">
                {talk.content}
              </p>

              {/* 댓글 영역 */}
              <div className="mt-2 px-1">
                {!isExpanded ? (
                  <button
                    onClick={() => setActiveTalkId(talk.id)}
                    className="text-[11px] text-gray-500/70 font-medium"
                  >
                    댓글 {talk.comments?.length || 0}개 모두 보기...
                  </button>
                ) : (
                  <div className="space-y-3 animate-in fade-in duration-200">
                    {/* 댓글 리스트 (블랙 톤) */}
                    <div className="max-h-[150px] overflow-y-auto space-y-2.5 pr-1 scrollbar-hide py-1">
                      {talk.comments?.map((c: any, i: number) => (
                        <div
                          key={i}
                          className="flex gap-2 items-start text-[11px]"
                        >
                          <span className="font-bold text-gray-900 flex-shrink-0">
                            {c.user || "나"}
                          </span>
                          <span className="text-gray-800 font-medium leading-snug break-all">
                            {c.content || c.Content}
                          </span>
                        </div>
                      ))}
                    </div>

                    {/* 구분선 */}
                    <div className="h-[1px] w-full bg-black/[0.05] mt-3" />

                    {/* 입력 영역 */}
                    <div className="space-y-1.5 pt-2">
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          // ✅ 1. 현재 입력된 값 가져오기
                          value={commentValues[talk.id] || ""}
                          // ✅ 2. 에러 났던 handleCommentChange 대신 직접 상태 업데이트
                          onChange={(e) => {
                            setCommentValues((prev) => ({
                              ...prev,
                              [talk.id]: e.target.value,
                            }));
                          }}
                          placeholder="댓글 달기..."
                          // 디자인: 크기 키우고 두껍게
                          className="flex-1 bg-transparent border-none outline-none text-[14px] font-bold placeholder:text-gray-400/60 text-gray-800 p-0"
                          onKeyDown={(e) => {
                            // e.nativeEvent.isComposing은 한글 입력 시 중복 등록되는 현상을 방지합니다.
                            if (
                              e.key === "Enter" &&
                              !e.nativeEvent.isComposing
                            ) {
                              if (hasText) {
                                // 기존 게시 버튼 클릭 시와 동일한 로직 실행
                                onTalkClick({
                                  ...talk,
                                  currentInput: commentValues[talk.id],
                                });

                                // 입력창 비우기
                                setCommentValues((prev) => ({
                                  ...prev,
                                  [talk.id]: "",
                                }));
                              }
                            }
                          }}
                        />

                        {/* ✅ 게시 버튼: 커지는 효과 없이 색상 투명도만 조절 */}
                        <button
                          disabled={!hasText}
                          onClick={() => {
                            // 🔴 수정: 입력한 텍스트를 content라는 이름으로 함께 전달
                            onTalkClick({
                              ...talk,
                              currentInput: commentValues[talk.id],
                            });

                            // 게시 후 입력창 비우기
                            setCommentValues((prev) => ({
                              ...prev,
                              [talk.id]: "",
                            }));
                          }}
                          className={`text-[13px] font-extrabold px-1 transition-all duration-200`}
                          style={{
                            color: "#e364b0",
                            opacity: hasText ? 1 : 0.3,
                            cursor: hasText ? "pointer" : "default",
                          }}
                        >
                          게시
                        </button>
                      </div>
                      <div className="text-[10px] text-gray-400/80">
                        방금 전
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
    ? places.filter(
        (p) => p.title.includes(placeQuery) || p.address.includes(placeQuery),
      )
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
        {/* 닫기 버튼 */}
        <button
          onClick={onClose}
          className="absolute top-6 right-6 text-gray-300 hover:text-gray-500 transition-colors"
        >
          <X size={24} />
        </button>

        <div className="text-center mb-8">
          <h3 className="text-xl font-bold text-gray-900">실시간 톡 남기기</h3>
          <p className="text-xs text-gray-400 mt-1.5">
            지금 우리 동네 산책 소식을 들려주세요!
          </p>
        </div>

        <div className="space-y-6">
          {/* ✅ 입력창: 배경 빼고 글씨 굵게 (댓글창 스타일과 통일) */}
          <div className="relative border-b border-black/[0.05] pb-4">
            <textarea
              autoFocus
              placeholder="어떤 이야기를 공유할까요?"
              // text-[16px] font-bold 적용
              className="w-full h-32 bg-transparent border-none outline-none text-[16px] font-bold text-gray-800 placeholder:text-gray-300 resize-none p-0"
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
          </div>

          {/* 📍 장소 선택 (디자인 간소화 및 핑크 포인트) */}
          <div className="relative">
            {selectedPlace ? (
              <div
                className="flex items-center justify-between py-2 cursor-pointer"
                onClick={() => {
                  setSelectedPlaceId(null);
                  setShowPlaceSearch(true);
                }}
              >
                <div className="flex items-center gap-2">
                  <MapPin size={16} className="text-[#e364b0]" />
                  <span className="text-sm text-[#e364b0] font-bold">
                    {selectedPlace.title}
                  </span>
                </div>
                <X size={14} className="text-gray-300" />
              </div>
            ) : (
              <div
                className="flex items-center gap-2 py-2 text-gray-300 cursor-pointer hover:text-gray-400 transition-all group"
                onClick={() => setShowPlaceSearch(!showPlaceSearch)}
              >
                <MapPin
                  size={16}
                  className="group-hover:text-[#e364b0]/50 transition-colors"
                />
                <span className="text-sm font-bold">장소 태그하기 (선택)</span>
              </div>
            )}

            {/* 장소 검색창 디자인 유지 */}
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
                          onClick={() => {
                            setSelectedPlaceId(place.id);
                            setShowPlaceSearch(false);
                            setPlaceQuery("");
                          }}
                        >
                          <div className="text-sm font-bold text-gray-800">
                            {place.title}
                          </div>
                          <div className="text-[11px] text-gray-400">
                            {place.address}
                          </div>
                        </button>
                      ))
                    ) : (
                      <div className="p-4 text-center text-xs text-gray-400">
                        결과 없음
                      </div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* ✅ 등록 버튼: #e364b0 배경 적용 */}
          <button
            disabled={!content.trim()}
            onClick={() => {
              const finalLocation = selectedPlace
                ? selectedPlace.title
                : "내 주변";
              onSubmit(content.trim(), finalLocation);
              setContent("");
              setSelectedPlaceId(null);
              setPlaceQuery("");
              onClose();
            }}
            className="w-full py-4 rounded-2xl font-extrabold text-[16px] transition-all active:scale-95 disabled:opacity-30 shadow-lg shadow-[#e364b0]/20 text-white"
            style={{
              backgroundColor: "#e364b0",
            }}
          >
            지금 등록하기
          </button>
        </div>
      </motion.div>
    </div>
  );
}
