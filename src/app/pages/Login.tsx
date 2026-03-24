import React, { useState } from 'react';
import { Leaf, X, ArrowLeft } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';

import { motion } from 'motion/react';

type ModalType = 'terms' | 'privacy' | 'support' | null;

function FooterModal({ type, onClose }: { type: ModalType; onClose: () => void }) {
  if (!type) return null;

  const content: Record<NonNullable<ModalType>, { title: string; body: React.ReactNode }> = {
    terms: {
      title: '이용약관',
      body: (
        <div className="space-y-4 text-sm text-gray-600 leading-relaxed">
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제1조 (목적)</h3>
            <p>본 약관은 멍냥트립(이하 "서비스")이 제공하는 반려동물 동반 여행 정보 서비스의 이용 조건 및 절차에 관한 사항을 규정합니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제2조 (회원가입)</h3>
            <p>서비스 이용을 위해 회원가입이 필요하며, 만 14세 이상만 가입 가능합니다. 소셜 로그인(Google, Kakao)을 통한 간편 가입을 지원합니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제3조 (서비스 이용)</h3>
            <p>회원은 서비스를 통해 반려동물 동반 가능 장소 검색, 리뷰 작성, 여행 코스 저장 등의 기능을 이용할 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제4조 (금지 행위)</h3>
            <p>타인의 정보 도용, 허위 정보 게재, 서비스 운영 방해 등의 행위는 금지되며 위반 시 이용이 제한될 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제5조 (책임 제한)</h3>
            <p>서비스는 장소 정보의 정확성을 위해 노력하나, 실제 운영 상황과 다를 수 있습니다. 방문 전 해당 업체에 직접 확인을 권장합니다.</p>
          </section>
          <p className="text-xs text-gray-400 mt-4">시행일: 2025년 1월 1일</p>
        </div>
      ),
    },
    privacy: {
      title: '개인정보처리방침',
      body: (
        <div className="space-y-4 text-sm text-gray-600 leading-relaxed">
          <section>
            <h3 className="font-bold text-gray-800 mb-1">수집하는 개인정보</h3>
            <p>이메일 주소, 닉네임, 프로필 사진(소셜 로그인 시), 반려동물 정보(선택)</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">수집 목적</h3>
            <p>회원 식별 및 서비스 제공, 반려동물 맞춤 장소 추천, AI 산책 가이드 개인화</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">보유 및 이용 기간</h3>
            <p>회원 탈퇴 시까지 보유하며, 탈퇴 후 30일 이내 파기합니다. 단, 관계 법령에 따라 일부 정보는 일정 기간 보존될 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제3자 제공</h3>
            <p>이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다. 단, 법령에 의한 경우는 예외입니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">이용자의 권리</h3>
            <p>이용자는 언제든지 개인정보 열람, 수정, 삭제를 요청할 수 있으며 고객센터를 통해 처리됩니다.</p>
          </section>
          <p className="text-xs text-gray-400 mt-4">시행일: 2025년 1월 1일</p>
        </div>
      ),
    },
    support: {
      title: '고객센터',
      body: (
        <div className="space-y-5 text-sm text-gray-600">
          <div className="bg-primary/5 rounded-2xl p-4 space-y-2">
            <p className="font-bold text-gray-800">이메일 문의</p>
            <p className="text-primary font-medium">support@meongnyangtrip.com</p>
            <p className="text-xs text-gray-400">평일 09:00 ~ 18:00 (주말·공휴일 제외)</p>
          </div>
          <div className="space-y-3">
            <p className="font-bold text-gray-800">자주 묻는 질문</p>
            <div className="space-y-2">
              {[
                { q: '반려동물 정보는 어떻게 수정하나요?', a: '마이페이지 > 반려동물 관리에서 수정할 수 있습니다.' },
                { q: '소셜 로그인 계정을 변경하고 싶어요.', a: '현재 계정 탈퇴 후 새 계정으로 재가입이 필요합니다.' },
                { q: '장소 정보가 잘못되었어요.', a: '장소 상세 페이지에서 오류 신고 버튼을 이용해주세요.' },
              ].map(({ q, a }) => (
                <div key={q} className="bg-gray-50 rounded-xl p-3">
                  <p className="font-medium text-gray-700 mb-1">Q. {q}</p>
                  <p className="text-gray-500 text-xs">A. {a}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      ),
    },
  };

  const { title, body } = content[type];

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center" onClick={onClose}>
      <div className="absolute inset-0 bg-black/40" />
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className="relative w-full max-w-[600px] bg-white rounded-t-3xl max-h-[75vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-base font-bold text-gray-900">{title}</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-5">{body}</div>
      </motion.div>
    </div>
  );
}

interface LoginProps {
  onNavigate: (page: string) => void;
}

export function Login({ onNavigate }: LoginProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const login = useAppStore(state => state.login);

  const handleSocialLogin = (provider: string) => {
    const apiHost = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace('/api/v1', '');
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
      login(res.nickname, res.email, res.userId, res.profileImage, res.role === 'ADMIN');
      onNavigate('home');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="min-h-screen bg-white flex flex-col px-6"
    >
      <header className="pt-4 pb-2 -mx-0">
        <button onClick={() => onNavigate('home')} className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={24} />
        </button>
      </header>

      <div className="flex-1 flex flex-col justify-center">
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-primary/10 rounded-full mb-6">
            <Leaf size={40} className="text-primary" />
          </div>
          <h2 className="text-3xl font-bold text-gray-900 mb-2">멍냥트립</h2>
          <p className="text-gray-500">반려동물과 함께하는 자연 여행</p>
        </div>

        <form onSubmit={handleSubmit} className="w-full max-w-sm mx-auto space-y-3">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"  
            className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary transition-colors"
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password" 
            className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary transition-colors mb-4"
          />
          {error && <p className="text-red-500 text-sm px-1">{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-md hover:bg-primary/90 active:scale-[0.98] transition-all mt-4 disabled:opacity-60"
          >
            {isLoading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* 소셜 로그인 */}
        <div className="w-full max-w-sm mx-auto mt-6 space-y-2.5">
          <div className="flex items-center gap-3 mb-1">
            <div className="flex-1 h-px bg-gray-100" />
            <span className="text-[11px] text-gray-400">또는 소셜 로그인</span>
            <div className="flex-1 h-px bg-gray-100" />
          </div>
          <button
            onClick={() => handleSocialLogin('google')}
            className="w-full flex items-center justify-center gap-3 py-3 border border-gray-200 rounded-2xl bg-white hover:bg-gray-50 active:scale-[0.98] transition-all"
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
            className="w-full flex items-center justify-center gap-3 py-3 border border-yellow-300 rounded-2xl bg-[#FEE500] hover:bg-[#FDD800] active:scale-[0.98] transition-all"
          >
            <svg width="18" height="18" viewBox="0 0 48 48">
              <path fill="#3C1E1E" d="M24 4C12.95 4 4 11.16 4 20c0 5.6 3.6 10.5 9.09 13.37l-2.32 8.55c-.2.75.64 1.35 1.3.92L20.55 37c1.12.15 2.27.24 3.45.24 11.05 0 20-7.16 20-16S35.05 4 24 4z" />
            </svg>
            <span className="text-sm font-medium text-gray-900">카카오로 계속하기</span>
          </button>
        </div>

        <div className="mt-6 text-center flex justify-center text-sm text-gray-500 space-x-4">
          <span onClick={() => onNavigate('find-id')} className="cursor-pointer hover:text-gray-800">아이디 찾기</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => onNavigate('find-password')} className="cursor-pointer hover:text-gray-800">비밀번호 찾기</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => onNavigate('signup')} className="cursor-pointer hover:text-gray-800">회원가입</span>
        </div>

      </div>{/* flex-1 end */}

      {/* Footer */}
      <footer className="w-full pb-8 pt-4 flex flex-col items-center justify-center text-[11px] text-gray-500">
        <div className="flex items-center gap-2 mb-2">
          <span onClick={() => onNavigate('team')} className="cursor-pointer font-semibold hover:text-gray-800 transition-colors">개발팀 소개</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('terms')} className="cursor-pointer hover:text-gray-800 transition-colors">이용약관</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('privacy')} className="cursor-pointer font-semibold text-gray-700 hover:text-gray-900 transition-colors">개인정보처리방침</span>
          <span className="text-gray-300">|</span>
          <span onClick={() => setActiveModal('support')} className="cursor-pointer hover:text-gray-800 transition-colors">고객센터</span>
        </div>
        <div className="text-gray-400 text-[12px] text-center">
          <p className="mt-0.5">Copyright © {new Date().getFullYear()} Team 멍냥트립. All rights reserved.</p>
        </div>
      </footer>

      <FooterModal type={activeModal} onClose={() => setActiveModal(null)} />
    </motion.div>
  );
}
