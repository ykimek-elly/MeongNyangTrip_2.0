import api from './axios';
import type { ApiResponse } from './types';

// ── 타입 정의 ─────────────────────────────────────

export interface CheckInRequest {
  placeName: string;
  latitude: number;
  longitude: number;
  photoUrl?: string;
}

export interface CheckInResponse {
  checkinId: number;
  placeName: string;
  latitude: number;
  longitude: number;
  photoUrl: string | null;
  badgeName: string | null;
  checkedInAt: string;
}

export interface BadgeDto {
  id: number;
  name: string;
  icon: string;
  description: string;
  unlocked: boolean;
}

export interface CheckInStatsResponse {
  totalVisits: number;
  thisMonthVisits: number;
  unlockedBadges: number;
  badges: BadgeDto[];
  recentHistory: CheckInResponse[];
}

// ── API 함수 ──────────────────────────────────────

export const checkInApi = {
  /**
   * 방문 인증 저장
   * POST /api/checkins
   */
  createCheckIn: async (request: CheckInRequest): Promise<CheckInResponse> => {
    const { data } = await api.post<ApiResponse<CheckInResponse>>('/checkins', request);
    if (!data.data) throw new Error('방문 인증에 실패했습니다.');
    return data.data;
  },

  /**
   * 내 방문 통계 + 기록 + 뱃지 조회
   * GET /api/checkins/my
   */
  getMyStats: async (): Promise<CheckInStatsResponse> => {
    const { data } = await api.get<ApiResponse<CheckInStatsResponse>>('/checkins/my');
    if (!data.data) throw new Error('데이터를 불러오지 못했습니다.');
    return data.data;
  },
};
