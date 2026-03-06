import React, { useState } from 'react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { ArrowLeft, Mail, Eye, EyeOff, Leaf } from 'lucide-react';

interface SignupProps {
  onNavigate: (page: string) => void;
}

export function Signup({ onNavigate }: SignupProps) {
  const login = useAppStore(state => state.login);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);

  const isValid = email.trim() && password.length >= 6 && nickname.trim() && agreeTerms;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    // TODO: [DB 연동] supabase.auth.signUp → 이메일 인증 후 계정 생성
    login(nickname, email);
    // 가입 후 온보딩으로 이동
    onNavigate('onboarding');
  };

  const handleSocialLogin = (provider: string) => {
    // TODO: [DB 연동] supabase.auth.signInWithOAuth → 소셜 로그인
    login(provider === 'google' ? '구글유저' : '카카오유저', `${provider}@example.com`);
    onNavigate('onboarding');
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* 헤더 */}
      <header className="px-5 py-4 flex items-center bg-white sticky top-0 z-10">
        <button onClick={() => onNavigate('login')} className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1" />
      </header>

      <main className="flex-1 px-6 py-4 pb-24">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-8"
        >
          {/* 타이틀 */}
          <div>
            <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center mb-5">
              <Leaf size={28} className="text-primary" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">간편 가입하기</h2>
            <p className="text-sm text-gray-500 leading-relaxed">
              30초면 완료! 가입 후 반려동물 등록까지<br />한 번에 진행할 수 있어요.
            </p>
          </div>

          {/* 소셜 로그인 */}
          <div className="space-y-3">
            <button
              onClick={() => handleSocialLogin('google')}
              className="w-full flex items-center justify-center gap-3 py-3.5 border border-gray-200 rounded-2xl bg-white hover:bg-gray-50 active:scale-[0.98] transition-all"
            >
              <svg width="20" height="20" viewBox="0 0 48 48">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
              </svg>
              <span className="font-bold text-sm text-gray-700">Google로 계속하기</span>
            </button>

            <button
              onClick={() => handleSocialLogin('kakao')}
              className="w-full flex items-center justify-center gap-3 py-3.5 border border-yellow-300 rounded-2xl bg-[#FEE500] hover:bg-[#FDD800] active:scale-[0.98] transition-all"
            >
              <svg width="20" height="20" viewBox="0 0 48 48">
                <path fill="#3C1E1E" d="M24 4C12.95 4 4 11.16 4 20c0 5.6 3.6 10.5 9.09 13.37l-2.32 8.55c-.2.75.64 1.35 1.3.92L20.55 37c1.12.15 2.27.24 3.45.24 11.05 0 20-7.16 20-16S35.05 4 24 4z"/>
              </svg>
              <span className="font-bold text-sm text-gray-900">카카오로 계속하기</span>
            </button>
          </div>

          {/* 구분선 */}
          <div className="flex items-center gap-4">
            <div className="flex-1 h-px bg-gray-200" />
            <span className="text-xs text-gray-400">또는 이메일로 가입</span>
            <div className="flex-1 h-px bg-gray-200" />
          </div>

          {/* 이메일 가입 폼 */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">이메일</label>
              <div className="relative">
                <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="example@email.com"
                  className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="6자리 이상 입력해주세요"
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm pr-12"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {password.length > 0 && password.length < 6 && (
                <p className="text-xs text-destructive mt-1 ml-1">6자리 이상 입력해주세요</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1.5">닉네임</label>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="활동할 닉네임을 입력해주세요"
                className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                maxLength={20}
              />
            </div>

            {/* 약관 동의 */}
            <label className="flex items-start gap-3 cursor-pointer mt-2 p-3 bg-gray-50 rounded-2xl">
              <input
                type="checkbox"
                checked={agreeTerms}
                onChange={(e) => setAgreeTerms(e.target.checked)}
                className="w-5 h-5 mt-0.5 rounded border-gray-300 text-primary accent-[var(--primary)] shrink-0"
              />
              <span className="text-xs text-gray-600 leading-relaxed">
                <span className="font-bold text-gray-800">이용약관</span> 및 <span className="font-bold text-gray-800">개인정보처리방침</span>에 동의합니다
              </span>
            </label>

            {/* 가입 버튼 */}
            <button
              type="submit"
              disabled={!isValid}
              className={`w-full py-4 rounded-2xl font-bold shadow-lg transition-all active:scale-[0.98] mt-2 ${
                isValid
                  ? 'bg-primary text-white hover:bg-primary/90'
                  : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
              }`}
            >
              가입하기
            </button>
          </form>

          {/* 로그인 링크 */}
          <div className="text-center text-sm text-gray-400">
            이미 계정이 있으신가요?{' '}
            <button onClick={() => onNavigate('login')} className="font-bold text-primary hover:underline">
              로그인
            </button>
          </div>
        </motion.div>
      </main>
    </div>
  );
}