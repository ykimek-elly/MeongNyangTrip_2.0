import api from './axios';
import type { ApiResponse, PetDto, PetRequest } from './types';

/**
 * 반려동물(Pet) API 서비스.
 * 백엔드 PetController의 모든 엔드포인트와 매핑된다.
 *
 * TODO: JWT 완성 후 X-User-Id 헤더 제거 → Authorization Bearer 토큰으로 대체
 */
export const petApi = {
  /** GET /api/v1/pets → 내 반려동물 목록 */
  getPets: async (userId: number): Promise<PetDto[]> => {
    const { data } = await api.get<ApiResponse<PetDto[]>>('/pets', {
      headers: { 'X-User-Id': userId },
    });
    return data.data ?? [];
  },

  /** POST /api/v1/pets → 반려동물 등록 */
  addPet: async (userId: number, request: PetRequest): Promise<PetDto> => {
    const { data } = await api.post<ApiResponse<PetDto>>('/pets', request, {
      headers: { 'X-User-Id': userId },
    });
    if (!data.data) throw new Error('반려동물 등록에 실패했습니다.');
    return data.data;
  },

  /** PUT /api/v1/pets/{id} → 반려동물 수정 */
  updatePet: async (petId: number, userId: number, request: PetRequest): Promise<PetDto> => {
    const { data } = await api.put<ApiResponse<PetDto>>(`/pets/${petId}`, request, {
      headers: { 'X-User-Id': userId },
    });
    if (!data.data) throw new Error('반려동물 수정에 실패했습니다.');
    return data.data;
  },

  /** DELETE /api/v1/pets/{id} → 반려동물 삭제 */
  deletePet: async (petId: number, userId: number): Promise<void> => {
    await api.delete(`/pets/${petId}`, {
      headers: { 'X-User-Id': userId },
    });
  },

  /** PATCH /api/v1/pets/{id}/representative → 대표 반려동물 설정 */
  setRepresentative: async (petId: number, userId: number): Promise<PetDto> => {
    const { data } = await api.patch<ApiResponse<PetDto>>(`/pets/${petId}/representative`, null, {
      headers: { 'X-User-Id': userId },
    });
    if (!data.data) throw new Error('대표 설정에 실패했습니다.');
    return data.data;
  },
};
