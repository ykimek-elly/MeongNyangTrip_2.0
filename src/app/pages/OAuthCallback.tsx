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

      // 백엔드가 신규 유저 여부를 전달 — 기존 유저면 무조건 홈으로
      const isNewUser = searchParams.get('isNewUser') === 'true';

      login(nickname || '사용자', email || '', userId ? Number(userId) : undefined, profileImage || '');

      if (isNewUser) {
        useAppStore.getState().completeOnboarding(); // 온보딩 완료 후 다시 안 뜨도록
        navigate('/onboarding', { replace: true });
      } else {
        useAppStore.getState().completeOnboarding(); // 기존 유저 — 온보딩 스킵 처리
        navigate('/', { replace: true });
      }
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
