import api from './axios';
import type { AuthResponseDto } from './types';

/**
 * 인증(Auth) API 서비스.
 * 백엔드 AuthController 엔드포인트와 매핑된다.
 * NOTE: AuthController는 /api/auth/** 경로 사용 (v1 prefix 없음)
 */

const AUTH_BASE = '/auth';

export const authApi = {
  /** POST /api/auth/login → JWT 토큰 발급 */
  login: async (email: string, password: string): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/login`, { email, password });
    return data;
  },

  /** POST /api/auth/signup → 회원가입 + JWT 토큰 발급 */
  signup: async (email: string, password: string, nickname: string): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/signup`, { email, password, nickname });
    return data;
  },
};
