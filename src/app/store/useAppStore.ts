import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { PlaceDto, PetRequest } from '../api/types';
import { placeApi } from '../api/placeApi';
import { petApi } from '../api/petApi';
import { wishlistApi } from '../api/wishlistApi';

/** PetInfo(FE) → PetRequest(BE) 변환 헬퍼 */
const toPetRequest = (pet: PetInfo): PetRequest => ({
  petName: pet.name,
  petType: pet.type,
  petBreed: pet.breed,
  petGender: pet.gender,
  petSize: pet.size,
  petAge: pet.age,
  petWeight: pet.weight,
  petActivity: pet.activity,
  personality: pet.personality,
  preferredPlace: pet.preferredPlace,
  region: pet.region,
  activityRadius: pet.activityRadius,
  notifyEnabled: pet.notifyEnabled,
});

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
  region?: string;                      // 지역 선택 (시도 + 시군구, 알림 서비스용)
  activityRadius?: 5 | 15 | 30;        // 활동 반경 (km)
  preferredCategories?: ('PLACE' | 'STAY' | 'DINING')[];  // 선호 카테고리
  notifyEnabled?: boolean;              // 맞춤 알림 수신 동의
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
  userId: number | null;   // DB user_id — JWT 완성 후 로그인 시 설정됨
  username: string;
  email: string;
  profileImage: string;
  isAdmin: boolean;        // 관리자 여부 (JWT role 기반, 로그인 시 세팅)
  isSocial: boolean;       // 소셜 로그인 여부 (온보딩 분기용)
  pets: PetInfo[];                  // 다중 등록 (2026-03-13 확정)
  hasCompletedOnboarding: boolean;
  wishlist: number[];
  savedRoutes: SavedRoute[];
  userLocation: UserLocation;
  userRegionSido: string;
  userRegionDistrict: string;
  userActivityRadius: 5 | 15 | 30;

  login: (username: string, email?: string, userId?: number, profileImage?: string, isAdmin?: boolean, isSocial?: boolean, region?: string, activityRadius?: number) => void;
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
  setUserRegion: (sido: string, district: string, radius: 5 | 15 | 30) => void;

  // 공공API 장소 목록 데이터
  places: PlaceDto[];
  isLoadingPlaces: boolean;
  fetchPlaces: () => Promise<void>;
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      isLoggedIn: false,
      isAdmin: false,
      isSocial: false,
      userId: null,
      username: '게스트',
      email: '',
      profileImage: '',
      pets: [],
      hasCompletedOnboarding: false,
      wishlist: [],
      savedRoutes: [],
      userLocation: { lat: null, lng: null, address: null },
      userRegionSido: '',
      userRegionDistrict: '',
      userActivityRadius: 15,
      places: [],
      isLoadingPlaces: false,

      // TODO: [DB 연동] POST /api/auth/login → Step 4에서 JWT 토큰 기반으로 전환 (userId 자동 세팅)
      login: (username, email, userId, profileImage, isAdmin, isSocial, region, activityRadius) => {
        const _sido = region ? region.split(' ')[0] : undefined;
        const _district = region ? region.split(' ').slice(1).join(' ') : undefined;
        set((state) => ({
          isLoggedIn: true,
          isAdmin: isAdmin ?? false,
          isSocial: isSocial ?? false,
          username,
          email: email || '',
          userId: userId ?? null,
          profileImage: profileImage || '',
          userRegionSido: _sido !== undefined ? _sido : state.userRegionSido,
          userRegionDistrict: _district !== undefined ? _district : state.userRegionDistrict,
          userActivityRadius: activityRadius !== undefined ? (activityRadius as 5|15|30) : state.userActivityRadius,
        }));
        // 로그인 후 서버 찜 목록 동기화
        wishlistApi.getMyWishlists()
          .then((items) => set({ wishlist: items.map(i => i.placeId) }))
          .catch(() => {});
        // 로그인 후 서버 펫 목록 동기화
        if (userId) {
          petApi.getPets(userId)
            .then((pets) => set({
              pets: pets.map(p => ({
                id: p.petId,
                name: p.petName,
                type: p.petType,
                breed: p.petBreed,
                gender: p.petGender,
                size: p.petSize,
                age: p.petAge,
                weight: p.petWeight ?? undefined,
                activity: p.petActivity,
                personality: p.personality ?? undefined,
                preferredPlace: p.preferredPlace ?? undefined,
                region: p.region ?? undefined,
                activityRadius: (p.activityRadius ?? undefined) as 5 | 15 | 30 | undefined,
                isRepresentative: p.isRepresentative,
                notifyEnabled: p.notifyEnabled ?? true,
              }))
            }))
            .catch(() => {});
        }
      },

      // TODO: [DB 연동] POST /api/auth/logout → JWT 토큰 블랙리스트(Redis) 처리 + 클라이언트 토큰 삭제
      logout: () => set({ isLoggedIn: false, isAdmin: false, userId: null, username: '게스트', email: '', profileImage: '', pets: [], wishlist: [] }),

      // TODO: [DB 연동] PUT /api/users/profile → Spring Boot JPA users 테이블 UPDATE (PostgreSQL)
      updateProfile: (data) => set((state) => ({
        username: data.username ?? state.username,
        email: data.email ?? state.email,
      })),

      // TODO: [DB 연동] 온보딩 완료 플래그 서버 저장
      completeOnboarding: () => set({ hasCompletedOnboarding: true }),

      // POST /api/v1/pets — DB 저장 후 로컬 상태 동기화
      addPet: (pet) => {
        const { userId } = get();
        // 로컬 상태 즉시 반영 (낙관적 업데이트)
        set((state) => {
          const isFirst = state.pets.length === 0;
          const setAsRep = isFirst || !!pet.isRepresentative;
          const updatedExisting = setAsRep
            ? state.pets.map(p => ({ ...p, isRepresentative: false }))
            : state.pets;
          return { pets: [...updatedExisting, { ...pet, isRepresentative: setAsRep }] };
        });
        // DB 저장 (userId 있을 때만)
        if (userId) {
          petApi.addPet(userId, toPetRequest(pet))
            .then((saved) => set((state) => ({
              pets: state.pets.map(p =>
                p.name === saved.petName && p.id == null
                  ? { ...p, id: saved.petId }
                  : p
              ),
            })))
            .catch((err) => console.error('[Pet] addPet API 실패:', err));
        }
      },

      // PUT /api/v1/pets/{id} — DB 업데이트 후 로컬 상태 동기화
      updatePet: (tempId, partial) => {
        const { userId, pets } = get();
        set((state) => ({
          pets: state.pets.map((p, i) => i === tempId ? { ...p, ...partial } : p),
        }));
        const pet = pets[tempId];
        if (userId && pet?.id) {
          const updated = { ...pet, ...partial };
          petApi.updatePet(pet.id, userId, toPetRequest(updated))
            .catch((err) => console.error('[Pet] updatePet API 실패:', err));
        }
      },

      // DELETE /api/v1/pets/{id} — DB 삭제 후 로컬 상태 동기화
      removePet: (tempId) => {
        const { userId, pets } = get();
        const removed = pets[tempId];
        set((state) => {
          const remaining = state.pets.filter((_, i) => i !== tempId);
          if (removed?.isRepresentative && remaining.length > 0) {
            remaining[0] = { ...remaining[0], isRepresentative: true };
          }
          return { pets: remaining };
        });
        if (userId && removed?.id) {
          petApi.deletePet(removed.id, userId)
            .catch((err) => console.error('[Pet] deletePet API 실패:', err));
        }
      },

      // PATCH /api/v1/pets/{id}/representative — 대표 동물 변경
      setRepresentativePet: (tempId) => {
        const { userId, pets } = get();
        set((state) => ({
          pets: state.pets.map((p, i) => ({ ...p, isRepresentative: i === tempId })),
        }));
        const pet = pets[tempId];
        if (userId && pet?.id) {
          petApi.setRepresentative(pet.id, userId)
            .catch((err) => console.error('[Pet] setRepresentative API 실패:', err));
        }
      },

      // 대표 동물 getter — 알림/AI 가이드 등 단일 pet이 필요한 곳에서 사용
      getRepresentativePet: () => {
        const { pets } = get();
        return pets.find((p) => p.isRepresentative) ?? pets[0] ?? null;
      },

      // POST /api/v1/wishlists/{placeId} — 낙관적 업데이트 + API 동기화
      toggleWishlist: (id) => {
        const { isLoggedIn, wishlist } = get();
        const wasWished = wishlist.includes(id);
        // 낙관적 업데이트
        set({ wishlist: wasWished ? wishlist.filter(wId => wId !== id) : [...wishlist, id] });
        // 로그인 상태일 때만 API 호출
        if (isLoggedIn) {
          wishlistApi.toggle(id).catch((err) => {
            console.error('[Wishlist] toggle 실패, 롤백:', err);
            // 실패 시 원래 상태로 롤백
            set((state) => ({
              wishlist: wasWished
                ? [...state.wishlist, id]
                : state.wishlist.filter(wId => wId !== id),
            }));
          });
        }
      },

      clearWishlist: () => set({ wishlist: [] }),

      // TODO: [DB 연동] POST /api/saved-routes → Spring Boot JPA saved_routes 테이블 INSERT (PostgreSQL)
      addSavedRoute: (route) => set((state) => ({ savedRoutes: [route, ...state.savedRoutes] })),

      // TODO: [DB 연동] DELETE /api/saved-routes/{id} → Spring Boot JPA saved_routes 테이블 DELETE (PostgreSQL)
      removeSavedRoute: (id) => set((state) => ({ savedRoutes: state.savedRoutes.filter(r => r.id !== id) })),

      setUserLocation: (location) => set({ userLocation: location }),
      setUserRegion: (sido, district, radius) => set({ userRegionSido: sido, userRegionDistrict: district, userActivityRadius: radius }),

      // 공공API 장소 목록 연동
      fetchPlaces: async () => {
        set({ isLoadingPlaces: true });
        try {
          const data = await placeApi.getPlaces();
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
