import api from './axios';
import type { AuthResponseDto } from './types';

const AUTH_BASE = '/auth';
const USER_BASE = '/users';

export const authApi = {
  /** POST /api/v1/auth/login → Access Token + Refresh Token 발급 */
  login: async (email: string, password: string): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/login`, { email, password });
    return data;
  },

  /** POST /api/v1/auth/signup → 회원가입 + Access Token + Refresh Token 발급 */
  signup: async (email: string, password: string, nickname: string, phoneNumber?: string, notificationEnabled?: boolean): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/signup`, { email, password, nickname, phoneNumber, notificationEnabled });
    return data;
  },

  /** GET /api/v1/auth/check-phone → 휴대폰 번호 중복 확인 */
  checkPhone: async (phone: string): Promise<boolean> => {
    const { data } = await api.get<{ status: number; data: { available: boolean } }>(`${AUTH_BASE}/check-phone`, { params: { phone } });
    return data.data.available;
  },

  /**
   * POST /api/v1/auth/refresh → Access Token 재발급.
   * axios 인터셉터가 자동 호출하므로 직접 호출할 일은 드물다.
   */
  refresh: async (refreshToken: string): Promise<AuthResponseDto> => {
    const { data } = await api.post<AuthResponseDto>(`${AUTH_BASE}/refresh`, { refreshToken });
    return data;
  },

  /** POST /api/v1/auth/sms/send */
  sendSmsCode: async (phone: string): Promise<void> => {
    await api.post(`${AUTH_BASE}/sms/send`, { phone });
  },

  /** POST /api/v1/auth/sms/verify */
  verifySmsCode: async (phone: string, code: string): Promise<boolean> => {
    const { data } = await api.post<{ status: number; data: { verified: boolean } }>(
      `${AUTH_BASE}/sms/verify`, { phone, code }
    );
    return data.data.verified;
  },

  /** PUT /api/v1/users/profile */
  updateProfile: async (nickname: string): Promise<void> => {
    await api.put(`${USER_BASE}/profile`, { nickname });
  },

  /** PUT /api/v1/users/password */
  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.put(`${USER_BASE}/password`, { currentPassword, newPassword });
  },

  /** DELETE /api/v1/users/me */
  deleteAccount: async (): Promise<void> => {
    await api.delete(`${USER_BASE}/me`);
  },

  /** PATCH /api/v1/users/phone */
  savePhone: async (phone: string): Promise<void> => {
    await api.patch(`${USER_BASE}/phone`, { phone });
  },

  /** PATCH /api/v1/users/location — 활동 지역 좌표 + 반경 + 지역명 저장 */
  saveLocation: async (latitude?: number, longitude?: number, activityRadius?: number, region?: string): Promise<void> => {
    await api.patch(`${USER_BASE}/location`, { latitude, longitude, activityRadius, region });
  },

  /** POST /api/v1/auth/find-id */
  findId: async (name: string, phone: string): Promise<string> => {
    const { data } = await api.post<{ status: number; data: { email: string } }>(
      `${AUTH_BASE}/find-id`,
      { name, phone }
    );
    return data.data.email;
  },

  /** POST /api/v1/auth/reset-password */
  resetPassword: async (email: string, phone: string): Promise<void> => {
    await api.post(`${AUTH_BASE}/reset-password`, { email, phone });
  },
};
