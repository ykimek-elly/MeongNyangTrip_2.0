import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { useAppStore } from '../store/useAppStore';
import { Leaf } from 'lucide-react';

/**
 * Spring Security OAuth2.0 콜백 처리 페이지.
 * 백엔드가 인증 완료 후 아래 URL로 리다이렉트:
 *   {FRONTEND_URL}/oauth2/callback?token=JWT&userId=...&nickname=...&email=...&profileImage=...
 */
export function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const login = useAppStore(state => state.login);

  useEffect(() => {
    const token = searchParams.get('token');
    const nickname = searchParams.get('nickname');
    const email = searchParams.get('email');
    const userId = searchParams.get('userId');
    const profileImage = searchParams.get('profileImage');

    if (token) {
      localStorage.setItem('accessToken', token);
      login(nickname || '사용자', email || '', userId ? Number(userId) : undefined, profileImage || '');

      // 온보딩 미완료(신규 가입)이면 온보딩으로, 기존 유저면 홈으로
      const { hasCompletedOnboarding } = useAppStore.getState();
      navigate(hasCompletedOnboarding ? '/' : '/onboarding', { replace: true });
    } else {
      // 토큰 없음 = 인증 실패 → 로그인 페이지로
      navigate('/login', { replace: true });
    }
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="text-center">
        <Leaf size={40} className="text-primary mx-auto mb-4 animate-pulse" />
        <p className="text-gray-500 text-sm">소셜 로그인 처리 중...</p>
      </div>
    </div>
  );
}
