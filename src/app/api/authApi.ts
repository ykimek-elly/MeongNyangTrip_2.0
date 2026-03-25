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
  signup: async (email: string, password: string, nickname: string, phone: string): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/signup`, { email, password, nickname, phoneNumber: phone });
    return data;
  },

  /** POST /api/auth/sms/send → 휴대폰 인증번호 발송 */
  sendSmsCode: async (phone: string): Promise<void> => {
    await api.post(`${AUTH_BASE}/sms/send`, { phone });
  },

  /** POST /api/auth/sms/verify → 인증번호 확인 */
  verifySmsCode: async (phone: string, code: string): Promise<boolean> => {
    const { data } = await api.post<{ status: number; data: { verified: boolean } }>(
      `${AUTH_BASE}/sms/verify`, { phone, code }
    );
    return data.data.verified;
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

  /** PATCH /api/v1/users/phone → 소셜 로그인 후 휴대폰 번호 저장 (JWT 인증 필요) */
  savePhone: async (phone: string): Promise<void> => {
    await api.patch(`${USER_BASE}/phone`, { phone });
  },

  /** POST /api/auth/find-id → 이름+휴대폰으로 이메일(아이디) 조회 */
  findId: async (name: string, phone: string): Promise<string> => {
    const { data } = await api.post<{ status: number; data: { email: string } }>(
      `${AUTH_BASE}/find-id`,
      { name, phone }
    );
    return data.data.email;
  },

  /** POST /api/auth/reset-password → 이메일+휴대폰으로 임시 비밀번호 발송 */
  resetPassword: async (email: string, phone: string): Promise<void> => {
    await api.post(`${AUTH_BASE}/reset-password`, { email, phone });
  },
};
