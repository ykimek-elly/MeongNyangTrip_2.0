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
    const params = Object.fromEntries(searchParams.entries());
    const { 
      token, 
      refreshToken, 
      nickname, 
      email, 
      userId, 
      profileImage, 
      region, 
      activityRadius, 
      isNewUser: isNewUserStr 
    } = params;

    if (!token) {
      console.error('OAuth 인증 실패: 토큰이 없습니다.');
      navigate('/login?error=auth_failed', { replace: true });
      return;
    }

    try {
      localStorage.setItem('accessToken', token);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }

      const isNewUser = isNewUserStr === 'true';
      const payload = JSON.parse(atob(token.split('.')[1]));
      const isAdmin = payload.role === 'ADMIN' || payload.role === 'ROLE_ADMIN';
      login(
        nickname || '사용자', 
        email || '', 
        userId ? Number(userId) : undefined, 
        profileImage || '', 
        isAdmin, 
        true, 
        region || undefined, 
        activityRadius ? Number(activityRadius) : undefined
      );

      if (isNewUser) {
        navigate('/onboarding', { replace: true });
      } else {
        // 기존 사용자는 온보딩 단계를 스킵함
        useAppStore.getState().completeOnboarding();
        navigate('/', { replace: true });
      }
    } catch (err) {
      console.error('OAuth 인증 처리 중 오류 발생:', err);
      navigate('/login?error=process_failed', { replace: true });
    }
  }, [searchParams, navigate, login]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="text-center">
        <Leaf size={40} className="text-primary mx-auto mb-4 animate-pulse" />
        <p className="text-gray-500 text-sm">소셜 로그인 처리 중...</p>
      </div>
    </div>
  );
}
