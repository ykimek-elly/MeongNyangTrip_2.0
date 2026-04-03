import api from './axios';
import type { ApiResponse } from './types';

/** AI 산책 가이드 요청 파라미터 (GET 쿼리로 사용) */
export interface WalkGuideRequest {
  petSize?: 'SMALL' | 'MEDIUM' | 'LARGE';
  activityLevel?: 'LOW' | 'NORMAL' | 'HIGH';
  lat?: number;
  lng?: number;
}

/** 추천 장소 정보 */
export interface RecommendedPlace {
  id: number;
  contentId: string;
  kakaoId: string;
  title: string;
  address: string;
  latitude: number;
  longitude: number;
  category: string;
  imageUrl: string;
  aiRating: number;
  reviewCount: number;
  blogCount: number;
  blogPositiveTags?: string | null;
  overview?: string | null;
  recommendationDescription?: string | null;
}

/** AI 산책 가이드 응답 (백엔드 실데이터 규격) */
export interface WalkGuideResponse {
  userId: number;
  petId: number;
  petName: string;
  weatherType: string;
  weatherWalkLevel: string;
  weatherSummary: string;
  place: RecommendedPlace;
  notificationSummary: string;
  recommendationDescription: string;
  fallbackUsed: boolean;
  cacheHit: boolean;
  error: boolean;
  errorCode: string | null;
}

export const walkGuideApi = {
  /** GET /api/v1/ai/walk-guide — 실시간 AI 맞춤 산책 가이드 조회 */
  generate: async (req: WalkGuideRequest): Promise<WalkGuideResponse> => {
    const { data } = await api.get<ApiResponse<WalkGuideResponse>>('/ai/walk-guide', {
      params: req
    });
    if (!data.data) throw new Error('산책 가이드 생성에 실패했습니다.');
    return data.data;
  },

  /** GET /api/v1/weather — 현재 날씨 조회 (기존 유지) */
  getWeather: async (lat: number, lng: number): Promise<{ temperature: number; weather: string; description: string }> => {
    const { data } = await api.get<ApiResponse<{ temperature: number; weather: string; description: string }>>('/weather', {
      params: { lat, lng }
    });
    return data.data ?? { temperature: 20, weather: 'sunny', description: '날씨 정보를 불러올 수 없습니다.' };
  },
};
