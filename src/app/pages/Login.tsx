import React, { useState, lazy, Suspense } from 'react';
import { Leaf, X, ArrowLeft } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';

// FooterModal을 lazy load — 초기 번들에서 제외 (클릭 시에만 로드)
const FooterModal = lazy(() => import('../components/FooterModal'));

interface LoginProps {
  onNavigate: (page: string) => void;
}

export function Login({ onNavigate }: LoginProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [activeModal, setActiveModal] = useState<'terms' | 'privacy' | 'support' | null>(null);
  const login = useAppStore(state => state.login);

  const handleSocialLogin = (provider: string) => {
    const apiHost = window.location.origin;
    window.location.href = `${apiHost}/oauth2/authorization/${provider}`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return setError('이메일과 비밀번호를 입력해주세요.');
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      localStorage.setItem('accessToken', res.token);
      localStorage.setItem('refreshToken', res.refreshToken);
      login(
        res.nickname, res.email, res.userId, res.profileImage,
        res.role === 'ADMIN', false,
        res.region, res.activityRadius,
        res.phoneNumber, res.notificationEnabled
      );
      onNavigate('home');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    // motion.div → 일반 div + CSS 애니메이션 (번들 크기 절감)
    <div className="min-h-screen bg-white flex flex-col px-6 animate-login-enter">
      <header className="pt-4 pb-2 -mx-0">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full transition-spring">
          <ArrowLeft size={24} />
        </button>
      </header>

      <div className="flex-1 flex flex-col justify-center">
        <div className="text-center mb-10 animate-fade-in-up">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-primary/10 rounded-full mb-6 transition-spring hover:scale-105">
            <Leaf size={40} className="text-primary" />
          </div>
          <h2 className="text-3xl font-bold text-gray-900 mb-2 leading-snug">멍냥트립</h2>
          <p className="text-gray-500 leading-snug">반려동물과 함께하는 자연 여행</p>
        </div>

        <form onSubmit={handleSubmit} className="w-full max-w-sm mx-auto space-y-3 animate-fade-in-up" style={{ animationDelay: '0.08s' }}>
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary focus:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] transition-spring"
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary focus:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] transition-spring mb-4"
          />
          {error && <p className="text-red-500 text-sm px-1">{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-md hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.98] transition-spring mt-4 disabled:opacity-60 disabled:hover:scale-100"
          >
            {isLoading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* 소셜 로그인 */}
        <div className="w-full max-w-sm mx-auto mt-6 space-y-2.5 animate-fade-in-up" style={{ animationDelay: '0.16s' }}>
          <div className="flex items-center gap-3 mb-1">
            <div className="flex-1 h-px bg-gray-100" />
            <span className="text-[11px] text-gray-400">또는 소셜 로그인</span>
            <div className="flex-1 h-px bg-gray-100" />
          </div>
          <button
            onClick={() => handleSocialLogin('google')}
            className="w-full flex items-center justify-center gap-3 py-3 border border-gray-200 rounded-2xl bg-white hover:bg-gray-50 hover:scale-[1.02] active:scale-[0.98] transition-spring"
          >
            <svg width="18" height="18" viewBox="0 0 48 48">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
            </svg>
            <span className="text-sm font-medium text-gray-700">Google로 계속하기</span>
          </button>
          <button
            onClick={() => handleSocialLogin('kakao')}
            className="w-full flex items-center justify-center gap-3 py-3 border border-yellow-300 rounded-2xl bg-[#FEE500] hover:bg-[#FDD800] hover:scale-[1.02] active:scale-[0.98] transition-spring"
          >
            <svg width="18" height="18" viewBox="0 0 48 48">
              <path fill="#3C1E1E" d="M24 4C12.95 4 4 11.16 4 20c0 5.6 3.6 10.5 9.09 13.37l-2.32 8.55c-.2.75.64 1.35 1.3.92L20.55 37c1.12.15 2.27.24 3.45.24 11.05 0 20-7.16 20-16S35.05 4 24 4z" />
            </svg>
            <span className="text-sm font-medium text-gray-900">카카오로 계속하기</span>
          </button>
        </div>

        <div className="mt-6 text-center flex justify-center text-sm text-gray-500 space-x-4">
          <span onClick={() => onNavigate('find-id')} className="cursor-pointer hover:text-gray-800 transition-spring">아이디 찾기</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => onNavigate('find-password')} className="cursor-pointer hover:text-gray-800 transition-spring">비밀번호 찾기</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => onNavigate('signup')} className="cursor-pointer hover:text-gray-800 transition-spring">회원가입</span>
        </div>

      </div>

      {/* Footer */}
      <footer className="w-full pb-8 pt-4 flex flex-col items-center justify-center text-[11px] text-gray-500">
        <div className="flex items-center gap-2 mb-2">
          <span onClick={() => onNavigate('team')} className="cursor-pointer font-semibold hover:text-gray-800 transition-spring">개발팀 소개</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('terms')} className="cursor-pointer hover:text-gray-800 transition-spring">이용약관</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('privacy')} className="cursor-pointer font-semibold text-gray-700 hover:text-gray-900 transition-spring">개인정보처리방침</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('support')} className="cursor-pointer hover:text-gray-800 transition-spring">고객센터</span>
        </div>
        <div className="text-gray-400 text-[12px] text-center">
          <p className="mt-0.5">Copyright © {new Date().getFullYear()} Team 멍냥트립. All rights reserved.</p>
        </div>
      </footer>

      {/* FooterModal — lazy load, 클릭 시에만 번들 로드 */}
      {activeModal && (
        <Suspense fallback={null}>
          <FooterModal type={activeModal} onClose={() => setActiveModal(null)} />
        </Suspense>
      )}
    </div>
  );
}
