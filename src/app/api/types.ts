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
  createdAt: string;
  updatedAt: string;
}

/**
 * 인증 응답 타입.
 * 백엔드 AuthResponse와 1:1 매핑.
 */
export interface AuthResponseDto {
  token: string;
  userId: number;
  email: string;
  nickname: string;
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
