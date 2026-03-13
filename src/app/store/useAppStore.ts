import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { PlaceDto } from '../api/types';
import { placeApi } from '../api/placeApi';

/**
 * PET 테이블 매핑 인터페이스 (2026-03-13 협의 확정)
 * 강아지/고양이 통합 → 단일 PET 테이블 / 다중 등록 가능
 *
 * DB 컬럼 매핑 (snake_case → camelCase)
 * pet_id              → id
 * pet_name            → name
 * pet_type            → type
 * pet_breed           → breed
 * pet_gender          → gender
 * pet_size            → size
 * pet_age             → age
 * pet_weight          → weight
 * pet_activity        → activity
 * personality         → personality
 * preferred_place     → preferredPlace
 * is_representative   → isRepresentative  (알림 수신 대표 동물)
 * reg_date            → (서버 자동 저장, 프론트 불필요)
 */
export interface PetInfo {
  id?: number;                          // pet_id — PUT/DELETE 요청 시 필수, 등록 전 undefined
  name: string;                         // pet_name, 1~20자
  type: '강아지' | '고양이';             // pet_type — 통합 테이블
  breed: string;                        // pet_breed, 1~50자
  gender: '남아' | '여아';              // pet_gender
  size: 'SMALL' | 'MEDIUM' | 'LARGE';  // pet_size enum
  age: number;                          // pet_age (년)
  weight?: number;                      // pet_weight (kg), 선택
  activity: 'LOW' | 'NORMAL' | 'HIGH'; // pet_activity enum
  personality?: string;                 // personality, 최대 100자, 선택
  preferredPlace?: string;              // preferred_place, 최대 50자, 선택
  isRepresentative?: boolean;           // is_representative — 알림 수신 대표 동물 (1마리만 true)
}

export interface SavedRoute {
  id: string;
  date: string;
  weather: string;
  temperature: number;
  bestTime: string;
  routes: Array<{ name: string; distance: string; type: string }>;
}

export interface UserLocation {
  lat: number | null;
  lng: number | null;
  address: string | null;
}

interface AppState {
  isLoggedIn: boolean;
  username: string;
  email: string;
  pets: PetInfo[];                  // 다중 등록 (2026-03-13 확정)
  hasCompletedOnboarding: boolean;
  wishlist: number[];
  savedRoutes: SavedRoute[];
  userLocation: UserLocation;

  login: (username: string, email?: string) => void;
  logout: () => void;
  updateProfile: (data: { username?: string; email?: string }) => void;
  completeOnboarding: () => void;
  // 반려동물 다중 관리
  addPet: (pet: PetInfo) => void;                         // 신규 등록 (첫 번째 등록 시 자동 대표 설정)
  updatePet: (tempId: number, partial: Partial<PetInfo>) => void; // 수정 (배열 내 index 기반, DB 연동 후 pet_id 사용)
  removePet: (tempId: number) => void;                    // 삭제
  setRepresentativePet: (tempId: number) => void;         // 대표 동물 변경
  // 대표 동물 getter (알림, AI 가이드 등에서 사용)
  getRepresentativePet: () => PetInfo | null;
  toggleWishlist: (id: number) => void;
  clearWishlist: () => void;
  addSavedRoute: (route: SavedRoute) => void;
  removeSavedRoute: (id: string) => void;
  setUserLocation: (location: UserLocation) => void;

  // 공공API 장소 목록 데이터
  places: PlaceDto[];
  isLoadingPlaces: boolean;
  fetchPlaces: () => Promise<void>;
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      isLoggedIn: false,
      username: '게스트',
      email: '',
      pets: [],
      hasCompletedOnboarding: false,
      wishlist: [],
      savedRoutes: [],
      userLocation: { lat: null, lng: null, address: null },
      places: [],
      isLoadingPlaces: false,

      // TODO: [DB 연동] POST /api/auth/login → Spring Security JWT 토큰 기반 인증으로 전환
      login: (username, email) => set({
        isLoggedIn: true,
        username,
        email: email || '',
      }),

      // TODO: [DB 연동] POST /api/auth/logout → JWT 토큰 블랙리스트(Redis) 처리 + 클라이언트 토큰 삭제
      logout: () => set({ isLoggedIn: false, username: '게스트', email: '', pets: [], hasCompletedOnboarding: false, wishlist: [] }),

      // TODO: [DB 연동] PUT /api/users/profile → Spring Boot JPA users 테이블 UPDATE (PostgreSQL)
      updateProfile: (data) => set((state) => ({
        username: data.username ?? state.username,
        email: data.email ?? state.email,
      })),

      // TODO: [DB 연동] 온보딩 완료 플래그 서버 저장
      completeOnboarding: () => set({ hasCompletedOnboarding: true }),

      // TODO: [DB 연동] POST /api/pets → Spring Boot JPA pets 테이블 INSERT (PostgreSQL)
      // 첫 번째 등록 시 자동으로 대표 동물 설정
      addPet: (pet) => set((state) => {
        const isFirst = state.pets.length === 0;
        const setAsRep = isFirst || !!pet.isRepresentative;
        const updatedExisting = setAsRep
          ? state.pets.map(p => ({ ...p, isRepresentative: false }))
          : state.pets;
        return {
          pets: [...updatedExisting, { ...pet, isRepresentative: setAsRep }],
        };
      }),

      // TODO: [DB 연동] PUT /api/pets/{id} → Spring Boot JPA pets 테이블 UPDATE (PostgreSQL)
      updatePet: (tempId, partial) => set((state) => ({
        pets: state.pets.map((p, i) => i === tempId ? { ...p, ...partial } : p),
      })),

      // TODO: [DB 연동] DELETE /api/pets/{id} → Spring Boot JPA pets 테이블 DELETE (PostgreSQL)
      // 대표 동물 삭제 시 다음 동물을 자동으로 대표로 승격
      removePet: (tempId) => set((state) => {
        const removed = state.pets[tempId];
        const remaining = state.pets.filter((_, i) => i !== tempId);
        if (removed?.isRepresentative && remaining.length > 0) {
          remaining[0] = { ...remaining[0], isRepresentative: true };
        }
        return { pets: remaining };
      }),

      // TODO: [DB 연동] PATCH /api/pets/{id}/representative → is_representative 단독 업데이트
      setRepresentativePet: (tempId) => set((state) => ({
        pets: state.pets.map((p, i) => ({ ...p, isRepresentative: i === tempId })),
      })),

      // 대표 동물 getter — 알림/AI 가이드 등 단일 pet이 필요한 곳에서 사용
      getRepresentativePet: () => {
        const { pets } = get();
        return pets.find((p) => p.isRepresentative) ?? pets[0] ?? null;
      },

      // TODO: [DB 연동] POST|DELETE /api/wishlists/{placeId} → Spring Boot JPA wishlists 테이블 UPSERT/DELETE + 낙관적 업데이트
      toggleWishlist: (id) => set((state) => {
        const isWished = state.wishlist.includes(id);
        if (isWished) {
          return { wishlist: state.wishlist.filter(wId => wId !== id) };
        } else {
          return { wishlist: [...state.wishlist, id] };
        }
      }),

      // TODO: [DB 연동] DELETE /api/wishlists → Spring Boot JPA wishlists 전체 삭제 (PostgreSQL)
      clearWishlist: () => set({ wishlist: [] }),

      // TODO: [DB 연동] POST /api/saved-routes → Spring Boot JPA saved_routes 테이블 INSERT (PostgreSQL)
      addSavedRoute: (route) => set((state) => ({ savedRoutes: [route, ...state.savedRoutes] })),

      // TODO: [DB 연동] DELETE /api/saved-routes/{id} → Spring Boot JPA saved_routes 테이블 DELETE (PostgreSQL)
      removeSavedRoute: (id) => set((state) => ({ savedRoutes: state.savedRoutes.filter(r => r.id !== id) })),

      setUserLocation: (location) => set({ userLocation: location }),

      // 공공API 장소 목록 연동
      fetchPlaces: async () => {
        set({ isLoadingPlaces: true });
        try {
          const data = await placeApi.getPublicPlaces(1, 40);
          set({ places: data, isLoadingPlaces: false });
        } catch (error) {
          console.error("Failed to fetch places", error);
          set({ isLoadingPlaces: false });
        }
      },
    }),
    {
      // TODO: [DB 연동] persist 미들웨어 제거 → WebSocket(STOMP) 실시간 동기화로 대체
      name: 'meongnyang-storage',
      partialize: (state) => Object.fromEntries(
        Object.entries(state).filter(([key]) => !['places', 'isLoadingPlaces'].includes(key))
      ),
    }
  )
);
