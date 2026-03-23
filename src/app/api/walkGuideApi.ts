import api from './axios';
import type { ApiResponse } from './types';

/** AI 산책 가이드 요청 파라미터 */
export interface WalkGuideRequest {
  petSize: 'SMALL' | 'MEDIUM' | 'LARGE';
  activityLevel: 'LOW' | 'NORMAL' | 'HIGH';
  lat?: number;
  lng?: number;
}

/** 추천 경로 단건 */
export interface WalkRouteDto {
  name: string;
  distance: string;
  type: string;
  difficulty?: string;
}

/** AI 산책 가이드 응답 */
export interface WalkGuideResponse {
  summary: string;
  temperature: number;
  weather: 'sunny' | 'cloudy' | 'rainy' | 'snowy';
  bestTime: string;
  duration: string;
  routes: WalkRouteDto[];
  tips: string[];
  emergency?: { name: string; distance: string; phone: string }[];
}

/**
 * AI 산책 가이드 API 서비스.
 * 백엔드 팀원C의 WalkGuideController 엔드포인트와 매핑된다.
 *
 * BE 엔드포인트:
 *   POST /api/v1/ai/walk-guide
 *   Body: { petSize, activityLevel, lat?, lng? }
 *   Response: ApiResponse<WalkGuideResponse>
 *
 * 날씨 API 연동 (팀원C):
 *   GET /api/v1/weather?lat={lat}&lng={lng}
 *   Response: ApiResponse<{ temperature, weather, description }>
 */
export const walkGuideApi = {
  /** POST /api/v1/ai/walk-guide — Gemini 기반 맞춤 산책 가이드 생성 */
  generate: async (req: WalkGuideRequest): Promise<WalkGuideResponse> => {
    const { data } = await api.post<ApiResponse<WalkGuideResponse>>('/ai/walk-guide', req);
    if (!data.data) throw new Error('산책 가이드 생성에 실패했습니다.');
    return data.data;
  },

  /** GET /api/v1/weather — 현재 날씨 조회 */
  getWeather: async (lat: number, lng: number): Promise<{ temperature: number; weather: string; description: string }> => {
    const { data } = await api.get<ApiResponse<{ temperature: number; weather: string; description: string }>>('/weather', {
      params: { lat, lng }
    });
    return data.data ?? { temperature: 20, weather: 'sunny', description: '날씨 정보를 불러올 수 없습니다.' };
  },
};
