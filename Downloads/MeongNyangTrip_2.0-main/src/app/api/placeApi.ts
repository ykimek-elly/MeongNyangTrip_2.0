import api from './axios';
import type { ApiResponse, PlaceDto, PlaceRequest } from './types';

/**
 * 장소(Place) API 서비스.
 * 백엔드 PlaceController의 모든 엔드포인트와 매핑된다.
 */
export const placeApi = {
  /**
   * 장소 목록 조회
   * GET /api/v1/places
   */
  getPlaces: async (category?: string, keyword?: string): Promise<PlaceDto[]> => {
    const params: Record<string, string> = {};
    if (category) params.category = category;
    if (keyword) params.keyword = keyword;

    const { data } = await api.get<ApiResponse<PlaceDto[]>>('/places', { params });
    return data.data ?? [];
  },

  /**
   * 장소 상세 조회
   * GET /api/v1/places/{id}
   */
  getPlace: async (id: number): Promise<PlaceDto> => {
    const { data } = await api.get<ApiResponse<PlaceDto>>(`/places/${id}`);
    if (!data.data) throw new Error('장소를 찾을 수 없습니다.');
    return data.data;
  },

  /**
   * 장소 등록
   * POST /api/v1/places
   */
  createPlace: async (request: PlaceRequest): Promise<PlaceDto> => {
    const { data } = await api.post<ApiResponse<PlaceDto>>('/places', request);
    if (!data.data) throw new Error('장소 등록에 실패했습니다.');
    return data.data;
  },

  /**
   * 장소 수정
   * PUT /api/v1/places/{id}
   */
  updatePlace: async (id: number, request: PlaceRequest): Promise<PlaceDto> => {
    const { data } = await api.put<ApiResponse<PlaceDto>>(`/places/${id}`, request);
    if (!data.data) throw new Error('장소 수정에 실패했습니다.');
    return data.data;
  },

  /**
   * 장소 삭제
   * DELETE /api/v1/places/{id}
   */
  deletePlace: async (id: number): Promise<void> => {
    await api.delete(`/places/${id}`);
  },

  /**
   * (공공API 연동) 장소 목록 조회
   * GET /api/v1/public-places
   */
  getPublicPlaces: async (pageNo: number = 1, numOfRows: number = 20): Promise<PlaceDto[]> => {
    const { data } = await api.get<ApiResponse<PlaceDto[]>>(`/public-places`, {
      params: { pageNo, numOfRows }
    });
    return data.data ?? [];
  },
};
