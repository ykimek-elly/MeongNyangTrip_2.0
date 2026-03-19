import api from './axios';
import type { ApiResponse, ReviewDto, ReviewRequest, PlaceReviewsResponse } from './types';

/**
 * 리뷰(Review) API 서비스.
 * 백엔드 ReviewController 엔드포인트와 매핑된다.
 */
export const reviewApi = {
  /** POST /api/v1/reviews/{placeId} → 리뷰 작성 */
  createReview: async (placeId: number, request: ReviewRequest): Promise<ReviewDto> => {
    const { data } = await api.post<ApiResponse<ReviewDto>>(`/reviews/${placeId}`, request);
    if (!data.data) throw new Error('리뷰 등록에 실패했습니다.');
    return data.data;
  },

  /** GET /api/v1/reviews/{placeId} → 장소 리뷰 목록 조회 */
  getReviewsByPlace: async (placeId: number): Promise<PlaceReviewsResponse> => {
    const { data } = await api.get<ApiResponse<PlaceReviewsResponse>>(`/reviews/${placeId}`);
    return data.data ?? { averageRating: 0, totalCount: 0, reviews: [] };
  },

  /** GET /api/v1/reviews/my → 내 리뷰 목록 조회 */
  getMyReviews: async (): Promise<ReviewDto[]> => {
    const { data } = await api.get<ApiResponse<ReviewDto[]>>('/reviews/my');
    return data.data ?? [];
  },

  /** DELETE /api/v1/reviews/{reviewId} → 리뷰 삭제 */
  deleteReview: async (reviewId: number): Promise<void> => {
    await api.delete(`/reviews/${reviewId}`);
  },
};
