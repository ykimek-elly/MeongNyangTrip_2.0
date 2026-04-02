import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { useAppStore } from '../store/useAppStore';
import { Leaf } from 'lucide-react';

/**
 * Spring Security OAuth2.0 콜백 처리 페이지.
 * 백엔드가 인증 완료 후 아래 URL로 리다이렉트:
 *   {FRONTEND_URL}/oauth2/callback?token=JWT&refreshToken=...&userId=...&nickname=...&email=...&profileImage=...
 */
export function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const login = useAppStore(state => state.login);

  useEffect(() => {
    const token        = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const nickname     = searchParams.get('nickname');
    const email        = searchParams.get('email');
    const userId       = searchParams.get('userId');
    const profileImage = searchParams.get('profileImage');

    if (token) {
      localStorage.setItem('accessToken', token);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }

      const isNewUser = searchParams.get('isNewUser') === 'true';
      login(nickname || '사용자', email || '', userId ? Number(userId) : undefined, profileImage || '');

      if (isNewUser) {
        navigate('/onboarding', { replace: true });
      } else {
        useAppStore.getState().completeOnboarding();
        navigate('/', { replace: true });
      }
    } else {
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
