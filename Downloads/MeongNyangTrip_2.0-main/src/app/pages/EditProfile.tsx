import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, User, Mail, Lock, Eye, EyeOff, Check, AlertCircle } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';

interface EditProfileProps {
  onNavigate: (page: string) => void;
}

export function EditProfile({ onNavigate }: EditProfileProps) {
  const { username, email, updateProfile } = useAppStore();

  // 폼 상태
  const [newNickname, setNewNickname] = useState(username);
  const [newEmail, setNewEmail] = useState(email);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPw, setShowCurrentPw] = useState(false);
  const [showNewPw, setShowNewPw] = useState(false);
  const [showConfirmPw, setShowConfirmPw] = useState(false);

  // 저장 완료 토스트
  const [saved, setSaved] = useState(false);
  const [passwordSaved, setPasswordSaved] = useState(false);

  // 유효성 검사
  const nicknameChanged = newNickname.trim() !== username;
  const emailChanged = newEmail.trim() !== email;
  const profileChanged = (nicknameChanged || emailChanged) && newNickname.trim().length > 0;

  const passwordValid = newPassword.length >= 6;
  const passwordMatch = newPassword === confirmPassword;
  const canChangePassword = currentPassword.length > 0 && passwordValid && passwordMatch;

  // 프로필 저장
  const handleSaveProfile = () => {
    if (!profileChanged) return;
    // TODO: [DB 연동] PUT /api/users/profile → Spring Boot JPA users 테이블 UPDATE 닉네임/이메일 (PostgreSQL)
    updateProfile({
      username: newNickname.trim(),
      email: newEmail.trim(),
    });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  // 비밀번호 변경
  const handleChangePassword = () => {
    if (!canChangePassword) return;
    // TODO: [DB 연동] PUT /api/auth/password → Spring Security 비밀번호 변경 (BCrypt 암호화)
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setPasswordSaved(true);
    setTimeout(() => setPasswordSaved(false), 2000);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* 헤더 */}
      <header className="px-5 py-4 flex items-center bg-white sticky top-0 z-10 border-b border-gray-100">
        <button
          onClick={() => onNavigate('home')}
          className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
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
                  className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                />
              </div>
              {newNickname.trim().length === 0 && (
                <p className="text-xs text-destructive mt-1 ml-1 flex items-center gap-1">
                  <AlertCircle size={12} />
                  닉네임은 필수입니다
                </p>
              )}
            </div>

            {/* 이메일 */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">이메일</label>
              <div className="relative">
                <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  placeholder="example@email.com"
                  className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                />
              </div>
            </div>
          </div>

          {/* 저장 버튼 */}
          <button
            onClick={handleSaveProfile}
            disabled={!profileChanged}
            className={`w-full mt-5 py-3.5 rounded-2xl font-bold text-sm transition-all active:scale-[0.98] ${
              profileChanged
                ? 'bg-primary text-white shadow-md hover:bg-primary/90'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
          >
            {saved ? (
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
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm pr-12"
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
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm pr-12"
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
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm pr-12"
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

          {/* 비밀번호 변경 버튼 */}
          <button
            onClick={handleChangePassword}
            disabled={!canChangePassword}
            className={`w-full mt-5 py-3.5 rounded-2xl font-bold text-sm transition-all active:scale-[0.98] ${
              canChangePassword
                ? 'bg-gray-900 text-white shadow-md hover:bg-gray-800'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
          >
            {passwordSaved ? (
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
          <button className="w-full py-3 rounded-2xl border border-red-200 text-destructive text-sm font-bold hover:bg-red-50 transition-colors active:scale-[0.98]">
            {/* TODO: [DB 연동] 계정 삭제 확인 모달 + DELETE /api/auth/account → Spring Security 회원 탈퇴 (JWT 무효화 + 데이터 삭제) */}
            회원 탈퇴
          </button>
        </motion.section>
      </main>
    </div>
  );
}
