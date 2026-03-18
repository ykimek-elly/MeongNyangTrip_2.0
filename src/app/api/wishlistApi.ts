import api from './axios';
import type { ApiResponse, WishlistToggleResponse, WishlistItem } from './types';

/**
 * 찜하기(Wishlist) API 서비스.
 * 백엔드 WishlistController 엔드포인트와 매핑된다.
 */
export const wishlistApi = {
  /** POST /api/v1/wishlists/{placeId} → 찜하기 토글 */
  toggle: async (placeId: number): Promise<WishlistToggleResponse> => {
    const { data } = await api.post<ApiResponse<WishlistToggleResponse>>(`/wishlists/${placeId}`);
    if (!data.data) throw new Error('찜하기 처리에 실패했습니다.');
    return data.data;
  },

  /** GET /api/v1/wishlists/my → 내 찜 목록 조회 */
  getMyWishlists: async (): Promise<WishlistItem[]> => {
    const { data } = await api.get<ApiResponse<WishlistItem[]>>('/wishlists/my');
    return data.data ?? [];
  },

  /** GET /api/v1/wishlists/{placeId}/status → 찜 여부 확인 */
  isWishlisted: async (placeId: number): Promise<boolean> => {
    const { data } = await api.get<ApiResponse<boolean>>(`/wishlists/${placeId}/status`);
    return data.data ?? false;
  },
};
