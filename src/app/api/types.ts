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
  addr2: string | null;
  latitude: number;
  longitude: number;
  category: string;
  rating: number;
  reviewCount: number;
  imageUrl: string | null;
  phone: string | null;
  tags: string | null;
  overview: string | null;
  chkPetInside: string | null;
  accomCountPet: string | null;
  petTurnAdroose: string | null;
  homepage: string | null;
  aiRating: number | null;
  blogCount: number | null;
  blogPositiveTags: string | null;
  blogNegativeTags: string | null;
  petFacility: string | null;
  petPolicy: string | null;
  operatingHours: string | null;
  operationPolicy: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * 인증 응답 타입.
 * 백엔드 AuthResponse와 1:1 매핑.
 */
export interface AuthResponseDto {
  token: string;
  refreshToken: string;  // ← 이 줄 추가
  userId: number;
  email: string;
  nickname: string;
  profileImage?: string;
  role?: 'USER' | 'ADMIN';
}

/**
 * 반려동물 응답 타입.
 * 백엔드 PetResponseDto와 1:1 매핑.
 */
export interface PetDto {
  petId: number;
  petName: string;
  petType: '강아지' | '고양이';
  petBreed: string;
  petGender: '남아' | '여아';
  petSize: 'SMALL' | 'MEDIUM' | 'LARGE';
  petAge: number;
  petWeight?: number | null;
  petActivity: 'LOW' | 'NORMAL' | 'HIGH';
  personality?: string | null;
  preferredPlace?: string | null;
  isRepresentative: boolean;
}

/**
 * 반려동물 등록/수정 요청 타입.
 * 백엔드 PetRequestDto와 1:1 매핑.
 */
export interface PetRequest {
  petName: string;
  petType: '강아지' | '고양이';
  petBreed: string;
  petGender: '남아' | '여아';
  petSize: 'SMALL' | 'MEDIUM' | 'LARGE';
  petAge: number;
  petWeight?: number;
  petActivity: 'LOW' | 'NORMAL' | 'HIGH';
  personality?: string;
  preferredPlace?: string;
}

/**
 * 찜하기 토글 응답 타입.
 */
export interface WishlistToggleResponse {
  wishlisted: boolean;
  placeId: number;
}

/**
 * 찜 목록 단건 타입.
 * 백엔드 WishlistDto.Response와 1:1 매핑.
 */
export interface WishlistItem {
  wishlistId: number;
  placeId: number;
  title: string;
  category: string;
  imageUrl: string | null;
  address: string;
  rating: number;
  reviewCount: number;
}

/**
 * 리뷰 단건 타입.
 * 백엔드 ReviewDto.Response와 1:1 매핑.
 */
export interface ReviewDto {
  reviewId: number;
  userId: number;
  nickname: string;
  profileImage: string | null;
  content: string;
  rating: number;
  imageUrl: string | null;
  createdAt: string;
}

/**
 * 리뷰 작성 요청 타입.
 */
export interface ReviewRequest {
  content: string;
  rating: number;
  imageUrl?: string;
}

/**
 * 장소 리뷰 목록 응답 (평균 별점 포함).
 * 백엔드 ReviewDto.PlaceReviewsResponse와 1:1 매핑.
 */
export interface PlaceReviewsResponse {
  averageRating: number;
  totalCount: number;
  reviews: ReviewDto[];
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
