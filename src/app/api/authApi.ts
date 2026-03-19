import api from './axios';
import type { AuthResponseDto } from './types';

/**
 * 인증(Auth) API 서비스.
 * 백엔드 AuthController 엔드포인트와 매핑된다.
 * NOTE: AuthController는 /api/auth/** 경로 사용 (v1 prefix 없음)
 */

const AUTH_BASE = '/auth';
const USER_BASE = '/users';

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

  /** PUT /api/v1/users/profile → 닉네임 수정 (JWT 인증 필요) */
  updateProfile: async (nickname: string): Promise<void> => {
    await api.put(`${USER_BASE}/profile`, { nickname });
  },

  /** PUT /api/v1/users/password → 비밀번호 변경 (JWT 인증 필요) */
  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.put(`${USER_BASE}/password`, { currentPassword, newPassword });
  },

  /** DELETE /api/v1/users/me → 회원 탈퇴 소프트 딜리트 (JWT 인증 필요) */
  deleteAccount: async (): Promise<void> => {
    await api.delete(`${USER_BASE}/me`);
  },
};
