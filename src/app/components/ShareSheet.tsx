import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Search, UserPlus, X, Check, Link2, BookmarkPlus,
  Send, Copy, CheckCircle2, CirclePlus
} from 'lucide-react';
import { useFriendStore, type Friend } from '../store/useFriendStore';

interface ShareSheetProps {
  isOpen: boolean;
  onClose: () => void;
  postId: number;
  postImage: string;
  postUser: string;
}

export function ShareSheet({ isOpen, onClose, postId, postImage, postUser }: ShareSheetProps) {
  const { friends, suggestedFriends, addFriend, sharePost, fetchFriends, fetchSuggestedFriends } = useFriendStore();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFriends, setSelectedFriends] = useState<string[]>([]);
  const [showAddFriend, setShowAddFriend] = useState(false);
  const [shareMessage, setShareMessage] = useState('');
  const [shared, setShared] = useState(false);
  const [linkCopied, setLinkCopied] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setSelectedFriends([]);
      setSearchTerm('');
      setShareMessage('');
      setShared(false);
      setShowAddFriend(false);
      setLinkCopied(false);
      fetchFriends();
      fetchSuggestedFriends();
    }
  }, [isOpen, fetchFriends, fetchSuggestedFriends]);

  const filteredFriends = friends.filter(
    (f) =>
      searchTerm === '' ||
      f.name.includes(searchTerm) ||
      f.petName.includes(searchTerm)
  );

  const filteredSuggested = suggestedFriends.filter(
    (f) =>
      searchTerm === '' ||
      f.name.includes(searchTerm) ||
      f.petName.includes(searchTerm)
  );

  const toggleFriend = (id: string) => {
    setSelectedFriends((prev) =>
      prev.includes(id) ? prev.filter((fId) => fId !== id) : [...prev, id]
    );
  };

  const handleShare = () => {
    if (selectedFriends.length === 0) return;
    sharePost(postId, selectedFriends, shareMessage || undefined);
    setShared(true);
    setTimeout(() => {
      onClose();
    }, 1200);
  };

  const handleCopyLink = () => {
    const url = `https://meongnyang.trip/post/${postId}`;
    try {
      const textArea = document.createElement('textarea');
      textArea.value = url;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    } catch {
      // fallback: do nothing
    }
    setLinkCopied(true);
    setTimeout(() => setLinkCopied(false), 2000);
  };

  const handleAddFriend = (friend: Friend) => {
    addFriend(friend);
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[1100]">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/50"
            onClick={onClose}
          />

          {/* Bottom Sheet */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="absolute bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px] bg-white rounded-t-[20px] max-h-[85vh] flex flex-col"
          >
            {/* Drag Handle */}
            <div className="flex justify-center pt-3 pb-1 shrink-0">
              <div className="w-9 h-[5px] rounded-full bg-gray-300" />
            </div>

            {/* Post Preview (small) */}
            {!shared && (
              <div className="flex items-center gap-3 px-4 pb-3 border-b border-gray-100">
                <div className="w-10 h-10 rounded-lg overflow-hidden bg-gray-100 shrink-0">
                  <img src={postImage} alt="" className="w-full h-full object-cover" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-gray-500 truncate">
                    <span className="font-bold text-gray-700">{postUser}</span>님의 게시물 공유
                  </p>
                </div>
              </div>
            )}

            {/* Success State */}
            {shared ? (
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center justify-center py-16 gap-3"
              >
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', delay: 0.1 }}
                >
                  <CheckCircle2 size={56} className="text-primary" />
                </motion.div>
                <p className="text-base font-bold text-gray-800">공유 완료!</p>
                <p className="text-sm text-gray-500">
                  {selectedFriends.length}명의 친구에게 전송했어요
                </p>
              </motion.div>
            ) : (
              <>
                {/* Search + Add Friend */}
                <div className="px-4 py-3 shrink-0">
                  <div className="flex items-center gap-2">
                    <div className="flex-1 flex items-center gap-2 bg-gray-100 rounded-xl px-3 py-2.5">
                      <Search size={16} className="text-gray-400 shrink-0" />
                      <input
                        ref={searchRef}
                        type="text"
                        placeholder="검색"
                        className="flex-1 text-sm bg-transparent outline-none placeholder:text-gray-400"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                      />
                      {searchTerm && (
                        <button onClick={() => setSearchTerm('')}>
                          <X size={14} className="text-gray-400" />
                        </button>
                      )}
                    </div>
                    <button
                      onClick={() => setShowAddFriend(!showAddFriend)}
                      className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 transition-spring ${
                        showAddFriend
                          ? 'bg-primary text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      <UserPlus size={18} />
                    </button>
                  </div>
                </div>

                {/* Content Area (scrollable) */}
                <div className="flex-1 overflow-y-auto min-h-0">
                  <AnimatePresence mode="wait">
                    {showAddFriend ? (
                      <AddFriendPanel
                        key="add"
                        suggested={filteredSuggested}
                        onAdd={handleAddFriend}
                      />
                    ) : (
                      <FriendGrid
                        key="grid"
                        friends={filteredFriends}
                        selected={selectedFriends}
                        onToggle={toggleFriend}
                        searchTerm={searchTerm}
                      />
                    )}
                  </AnimatePresence>
                </div>

                {/* Bottom Actions */}
                <div className="border-t border-gray-100 shrink-0">
                  {/* Message input when friends selected */}
                  <AnimatePresence>
                    {selectedFriends.length > 0 && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="overflow-hidden"
                      >
                        <div className="px-4 pt-3 flex items-center gap-2">
                          <input
                            type="text"
                            placeholder="메시지 작성..."
                            className="flex-1 text-sm bg-gray-100 rounded-full px-4 py-2.5 outline-none placeholder:text-gray-400"
                            value={shareMessage}
                            onChange={(e) => setShareMessage(e.target.value)}
                          />
                          <button
                            onClick={handleShare}
                            className="bg-primary text-white text-sm font-bold px-5 py-2.5 rounded-full hover:bg-primary/90 active:scale-[0.97] transition-spring shrink-0"
                          >
                            보내기
                          </button>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  {/* Quick share options row */}
                  <div className="flex items-center justify-center gap-1 px-2 py-3 overflow-x-auto">
                    <QuickShareButton
                      icon={linkCopied ? <Check size={22} className="text-primary" /> : <Link2 size={22} />}
                      label={linkCopied ? '복사됨!' : '링크 복사'}
                      onClick={handleCopyLink}
                      active={linkCopied}
                    />
                    <QuickShareButton
                      icon={<Copy size={22} />}
                      label="메시지"
                      onClick={() => alert('메시지 앱으로 공유합니다. (데모)')}
                    />
                  </div>
                </div>
              </>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

function QuickShareButton({
  icon,
  label,
  onClick,
  active,
}: {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
  active?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-1.5 min-w-[72px] py-1.5 active:scale-[0.97] transition-transform"
    >
      <div
        className={`w-12 h-12 rounded-full flex items-center justify-center border-2 transition-spring ${
          active
            ? 'border-primary bg-primary/5 text-primary'
            : 'border-gray-200 bg-white text-gray-700'
        }`}
      >
        {icon}
      </div>
      <span className="text-[10px] text-gray-600 font-medium leading-tight text-center whitespace-nowrap">{label}</span>
    </button>
  );
}

function FriendGrid({
  friends,
  selected,
  onToggle,
  searchTerm,
}: {
  friends: Friend[];
  selected: string[];
  onToggle: (id: string) => void;
  searchTerm: string;
}) {
  if (friends.length === 0) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="flex flex-col items-center justify-center py-12 text-gray-400"
      >
        <Search size={28} className="mb-2 opacity-40" />
        <p className="text-sm">
          {searchTerm ? `"${searchTerm}" 검색 결과 없음` : '친구를 추가해보세요!'}
        </p>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="grid grid-cols-4 gap-y-4 gap-x-2 px-4 py-3"
    >
      {friends.map((friend) => {
        const isSelected = selected.includes(friend.id);
        return (
          <button
            key={friend.id}
            onClick={() => onToggle(friend.id)}
            className="flex flex-col items-center gap-1.5 active:scale-[0.97] transition-transform"
          >
            <div className="relative">
              <div
                className={`w-[60px] h-[60px] rounded-full overflow-hidden border-2 transition-spring ${
                  isSelected ? 'border-primary' : 'border-transparent'
                }`}
              >
                <img
                  src={friend.profileImg}
                  alt={friend.name}
                  className="w-full h-full object-cover"
                />
              </div>

              {/* Selection indicator */}
              {isSelected && (
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="absolute -bottom-0.5 -right-0.5 w-5 h-5 bg-primary rounded-full flex items-center justify-center shadow-sm"
                >
                  <Check size={12} className="text-white" strokeWidth={3} />
                </motion.div>
              )}

              {/* Verified badge */}
              {friend.isVerified && !isSelected && (
                <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center border-2 border-white">
                  <Check size={8} className="text-white" strokeWidth={3} />
                </div>
              )}
            </div>

            <span className="text-[11px] text-gray-800 font-medium leading-tight text-center truncate w-full px-1">
              {friend.name}
            </span>
          </button>
        );
      })}
    </motion.div>
  );
}

function AddFriendPanel({
  suggested,
  onAdd,
}: {
  suggested: Friend[];
  onAdd: (friend: Friend) => void;
}) {
  const [addedIds, setAddedIds] = useState<string[]>([]);

  const handleAdd = (friend: Friend) => {
    onAdd(friend);
    setAddedIds((prev) => [...prev, friend.id]);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="px-4 py-3"
    >
      <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wide mb-3">
        추천 친구
      </h4>

      {suggested.length === 0 && addedIds.length === 0 ? (
        <div className="text-center py-8 text-gray-400">
          <UserPlus size={28} className="mx-auto mb-2 opacity-40" />
          <p className="text-sm">추천 친구가 없습니다</p>
        </div>
      ) : (
        <div className="space-y-1">
          {suggested.map((friend) => {
            const justAdded = addedIds.includes(friend.id);
            return (
              <motion.div
                key={friend.id}
                layout
                className="flex items-center gap-3 p-2 rounded-xl hover:bg-gray-50 transition-spring"
              >
                <div className="w-11 h-11 rounded-full overflow-hidden bg-gray-100 shrink-0 relative">
                  <img
                    src={friend.profileImg}
                    alt={friend.name}
                    className="w-full h-full object-cover"
                  />
                  {friend.isVerified && (
                    <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center border-2 border-white">
                      <Check size={8} className="text-white" strokeWidth={3} />
                    </div>
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <p className="text-sm font-bold text-gray-800 truncate">{friend.name}</p>
                  <p className="text-[11px] text-gray-500 truncate">
                    {friend.petType === '강아지' ? '🐶' : '🐱'} {friend.petName}
                  </p>
                </div>

                {justAdded ? (
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    className="flex items-center gap-1 text-primary text-xs font-bold"
                  >
                    <CheckCircle2 size={16} /> 추가됨
                  </motion.div>
                ) : (
                  <button
                    onClick={() => handleAdd(friend)}
                    className="bg-primary text-white text-xs font-bold px-3 py-1.5 rounded-lg hover:bg-primary/90 active:scale-[0.97] transition-spring flex items-center gap-1"
                  >
                    <CirclePlus size={14} /> 친구 추가
                  </button>
                )}
              </motion.div>
            );
          })}

          {/* Already added confirmation */}
          {addedIds.length > 0 && suggested.length === 0 && (
            <div className="text-center py-6 text-gray-400">
              <CheckCircle2 size={28} className="mx-auto mb-2 text-primary opacity-50" />
              <p className="text-sm">모든 추천 친구를 추가했어요!</p>
            </div>
          )}
        </div>
      )}
    </motion.div>
  );
}