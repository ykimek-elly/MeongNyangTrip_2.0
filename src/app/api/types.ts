/**
 * API 공통 응답 타입.
 * 백엔드 ApiResponse<T> record와 1:1 매핑.
 * @see docs/specs/core-setup.md 1장
 */
export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T | null;
}

/**
 * 장소(Place) 응답 타입.
 * 백엔드 PlaceResponseDto와 1:1 매핑.
 */
export interface PlaceDto {
  id: number;
  title: string;
  description: string | null;
  address: string;
  latitude: number;
  longitude: number;
  category: string;
  rating: number;
  reviewCount: number;
  imageUrl: string | null;
  phone: string | null;
  tags: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * 장소 등록/수정 요청 타입.
 * 백엔드 PlaceRequestDto와 1:1 매핑.
 */
export interface PlaceRequest {
  title: string;
  description?: string;
  address: string;
  latitude: number;
  longitude: number;
  category: string;
  imageUrl?: string;
  phone?: string;
  tags?: string;
}
