import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, User, Mail, Lock, Eye, EyeOff, Check, AlertCircle, Loader2 } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';

interface EditProfileProps {
  onNavigate: (page: string) => void;
}

export function EditProfile({ onNavigate }: EditProfileProps) {
  const { username, email, updateProfile, logout } = useAppStore();

  // 폼 상태
  const [newNickname, setNewNickname] = useState(username);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPw, setShowCurrentPw] = useState(false);
  const [showNewPw, setShowNewPw] = useState(false);
  const [showConfirmPw, setShowConfirmPw] = useState(false);

  // 저장 완료 토스트
  const [saved, setSaved] = useState(false);
  const [passwordSaved, setPasswordSaved] = useState(false);

  // 로딩 상태
  const [isProfileSaving, setIsProfileSaving] = useState(false);
  const [isPasswordChanging, setIsPasswordChanging] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // 에러 메시지
  const [profileError, setProfileError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  // 회원탈퇴 확인 모달
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // 유효성 검사
  const nicknameChanged = newNickname.trim() !== username;
  const profileChanged = nicknameChanged && newNickname.trim().length >= 2;

  const passwordValid = newPassword.length >= 6;
  const passwordMatch = newPassword === confirmPassword;
  const canChangePassword = currentPassword.length > 0 && passwordValid && passwordMatch;

  // 프로필 저장
  const handleSaveProfile = async () => {
    if (!profileChanged) return;
    setIsProfileSaving(true);
    setProfileError('');
    try {
      await authApi.updateProfile(newNickname.trim());
      updateProfile({ username: newNickname.trim() });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err: any) {
      setProfileError(err.response?.data?.message || err.message || '저장에 실패했습니다.');
    } finally {
      setIsProfileSaving(false);
    }
  };

  // 비밀번호 변경
  const handleChangePassword = async () => {
    if (!canChangePassword) return;
    setIsPasswordChanging(true);
    setPasswordError('');
    try {
      await authApi.changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordSaved(true);
      setTimeout(() => setPasswordSaved(false), 2000);
    } catch (err: any) {
      setPasswordError(err.response?.data?.message || err.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setIsPasswordChanging(false);
    }
  };

  // 회원 탈퇴
  const handleDeleteAccount = async () => {
    setIsDeleting(true);
    try {
      await authApi.deleteAccount();
      logout();
      onNavigate('home');
    } catch (err: any) {
      alert(err.response?.data?.message || '회원 탈퇴에 실패했습니다.');
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* 헤더 */}
      <header className="px-5 py-4 flex items-center bg-white sticky top-0 z-10 border-b border-gray-100">
        <button
          onClick={() => onNavigate('home')}
          className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full transition-spring"
        >
          <ArrowLeft size={24} />
        </button>
        <h2 className="flex-1 text-center font-bold text-gray-800 pr-8">회원정보 수정</h2>
      </header>

      <main className="flex-1 px-6 py-6 pb-24 space-y-6">
        {/* 프로필 정보 섹션 */}
        <motion.section
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100"
        >
          <h3 className="font-bold text-gray-800 mb-5 flex items-center gap-2">
            <User size={18} className="text-primary" />
            기본 정보
          </h3>

          <div className="space-y-4">
            {/* 닉네임 */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">닉네임</label>
              <div className="relative">
                <User size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  placeholder="닉네임을 입력해주세요"
                  maxLength={20}
                  className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-spring text-sm"
                />
              </div>
              {newNickname.trim().length > 0 && newNickname.trim().length < 2 && (
                <p className="text-xs text-destructive mt-1 ml-1 flex items-center gap-1">
                  <AlertCircle size={12} />
                  닉네임은 2자 이상이어야 합니다
                </p>
              )}
            </div>

            {/* 이메일 (읽기 전용) */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">이메일</label>
              <div className="relative">
                <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  value={email}
                  readOnly
                  className="w-full pl-11 pr-4 py-3.5 bg-gray-100 border border-gray-200 rounded-2xl outline-none text-sm text-gray-500 cursor-not-allowed"
                />
              </div>
              <p className="text-[10px] text-gray-400 mt-1 ml-1">이메일은 변경할 수 없습니다</p>
            </div>
          </div>

          {profileError && (
            <p className="text-xs text-destructive mt-3 flex items-center gap-1">
              <AlertCircle size={12} />
              {profileError}
            </p>
          )}

          {/* 저장 버튼 */}
          <button
            onClick={handleSaveProfile}
            disabled={!profileChanged || isProfileSaving}
            className={`w-full mt-5 py-3.5 rounded-2xl font-bold text-sm transition-spring active:scale-[0.98] ${
              profileChanged && !isProfileSaving
                ? 'bg-primary text-white shadow-md hover:bg-primary/90'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
          >
            {isProfileSaving ? (
              <span className="flex items-center justify-center gap-1.5">
                <Loader2 size={16} className="animate-spin" />
                저장 중...
              </span>
            ) : saved ? (
              <span className="flex items-center justify-center gap-1.5">
                <Check size={16} />
                저장되었습니다!
              </span>
            ) : (
              '변경사항 저장'
            )}
          </button>
        </motion.section>

        {/* 비밀번호 변경 섹션 */}
        <motion.section
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100"
        >
          <h3 className="font-bold text-gray-800 mb-5 flex items-center gap-2">
            <Lock size={18} className="text-primary" />
            비밀번호 변경
          </h3>

          <div className="space-y-4">
            {/* 현재 비밀번호 */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">현재 비밀번호</label>
              <div className="relative">
                <input
                  type={showCurrentPw ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="현재 비밀번호를 입력하세요"
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-spring text-sm pr-12"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPw(!showCurrentPw)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showCurrentPw ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* 새 비밀번호 */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">새 비밀번호</label>
              <div className="relative">
                <input
                  type={showNewPw ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="6자리 이상 입력해주세요"
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-spring text-sm pr-12"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPw(!showNewPw)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showNewPw ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {newPassword.length > 0 && !passwordValid && (
                <p className="text-xs text-destructive mt-1 ml-1 flex items-center gap-1">
                  <AlertCircle size={12} />
                  6자리 이상 입력해주세요
                </p>
              )}
            </div>

            {/* 비밀번호 확인 */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">새 비밀번호 확인</label>
              <div className="relative">
                <input
                  type={showConfirmPw ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="비밀번호를 다시 입력해주세요"
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-spring text-sm pr-12"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPw(!showConfirmPw)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showConfirmPw ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {confirmPassword.length > 0 && !passwordMatch && (
                <p className="text-xs text-destructive mt-1 ml-1 flex items-center gap-1">
                  <AlertCircle size={12} />
                  비밀번호가 일치하지 않습니다
                </p>
              )}
            </div>
          </div>

          {passwordError && (
            <p className="text-xs text-destructive mt-3 flex items-center gap-1">
              <AlertCircle size={12} />
              {passwordError}
            </p>
          )}

          {/* 비밀번호 변경 버튼 */}
          <button
            onClick={handleChangePassword}
            disabled={!canChangePassword || isPasswordChanging}
            className={`w-full mt-5 py-3.5 rounded-2xl font-bold text-sm transition-spring active:scale-[0.98] ${
              canChangePassword && !isPasswordChanging
                ? 'bg-gray-900 text-white shadow-md hover:bg-gray-800'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
          >
            {isPasswordChanging ? (
              <span className="flex items-center justify-center gap-1.5">
                <Loader2 size={16} className="animate-spin" />
                변경 중...
              </span>
            ) : passwordSaved ? (
              <span className="flex items-center justify-center gap-1.5">
                <Check size={16} />
                변경되었습니다!
              </span>
            ) : (
              '비밀번호 변경'
            )}
          </button>

          {/* 안내 문구 */}
          <p className="text-[10px] text-gray-400 mt-3 text-center">
            소셜 로그인 사용자는 비밀번호 변경이 불필요합니다
          </p>
        </motion.section>

        {/* 계정 삭제 (위험 영역) */}
        <motion.section
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100"
        >
          <h3 className="font-bold text-gray-800 mb-2 flex items-center gap-2">
            <AlertCircle size={18} className="text-destructive" />
            계정 삭제
          </h3>
          <p className="text-xs text-gray-500 mb-4">
            계정을 삭제하면 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.
          </p>
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="w-full py-3 rounded-2xl border border-red-200 text-destructive text-sm font-bold hover:bg-red-50 transition-spring active:scale-[0.98]"
          >
            회원 탈퇴
          </button>
        </motion.section>
      </main>

      {/* 회원탈퇴 확인 모달 */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center px-6">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-3xl p-6 w-full max-w-sm shadow-xl"
          >
            <h3 className="font-bold text-gray-800 text-lg mb-2">정말 탈퇴하시겠어요?</h3>
            <p className="text-sm text-gray-500 mb-6">
              계정과 모든 데이터가 삭제됩니다. 이 작업은 되돌릴 수 없습니다.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 py-3 rounded-2xl border border-gray-200 text-gray-700 font-bold text-sm hover:bg-gray-50 transition-spring"
              >
                취소
              </button>
              <button
                onClick={handleDeleteAccount}
                disabled={isDeleting}
                className="flex-1 py-3 rounded-2xl bg-red-500 text-white font-bold text-sm hover:bg-red-600 transition-spring disabled:opacity-60"
              >
                {isDeleting ? (
                  <span className="flex items-center justify-center gap-1.5">
                    <Loader2 size={14} className="animate-spin" />
                    처리 중...
                  </span>
                ) : (
                  '탈퇴하기'
                )}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
